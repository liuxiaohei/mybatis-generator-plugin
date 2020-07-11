package com.luca.mybatis.generator.plugins;
import com.luca.mybatis.generator.plugins.el.Bool;
import com.luca.mybatis.generator.plugins.el.Pair;
import com.luca.mybatis.generator.plugins.el.Str;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.config.TableConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides the ability to remap awkward SQL Type to more convenient one (e.g. NUMBER(18,0) - from DECIMAL and java.math.BigDecimal to BIGINT and Long; TIMESTAMP (3) WITH LOCAL TIME ZONE from OTHER and java.lang.Object to TIMESTAMP and java.util.Date).
 */
public class JDBCTypesPlugin extends IntrospectorPlugin {
    private boolean verbose;
    private boolean introspect;
    private final List<Pair<Type, Type>> convert = new ArrayList<Pair<Type, Type>>();
    private final Map<String, Map<String, ColumnInfo>> columnInfo = new HashMap<String, Map<String, ColumnInfo>>();

    private Pattern include;
    private Pattern exclude;

    private static class ColumnInfo {
        String nativeTypeName;
    }

    private static class IntegerInterval {
        int min;
        int max;
        public boolean contains(int value) {
            return value >= min && value <= max;
        }

        private static Pattern INTERVAL = Pattern.compile("^(\\d++)?\\s*(\\.\\.)?\\s*(\\d+)?$");

        public static IntegerInterval parse(String s) {
            if (Str.trim(s) == null)
                return null;
            Matcher matcher = INTERVAL.matcher(s);
            if (!matcher.matches())
                throw new IllegalArgumentException("Invalid interval definition: '"+s+"'");
            IntegerInterval i = new IntegerInterval();
            if (matcher.group(2) != null) {
                i.min = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : Integer.MIN_VALUE;
                i.max = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : Integer.MAX_VALUE;
            } else {
                i.min = i.max = Integer.parseInt(matcher.group(1));
            }
            return i;
        }

        @Override
        public String toString() {
            return min != max ? (min == Integer.MIN_VALUE ? "" : String.valueOf(min)) + ".." + (max == Integer.MAX_VALUE ? "" : String.valueOf(max)) : String.valueOf(min);
        }
    }

    private static class Type {
        String name;
        String regexp;
        IntegerInterval length;
        IntegerInterval scale;
        String javaType;
        String typeHandler;

        private static final Pattern SIMPLE = Pattern.compile("^[^\\(]+$");
        private static final Pattern WITH_LEN = Pattern.compile("^([^(]+?)\\s*\\(([^,]+)(?:,([^,)]+))?\\)$");
        public static Type parse(String s) {
            s = Str.trim(s);
            if (s == null) return null;
            Type type = new Type();
            if (s.charAt(0) == '/') {
                type.regexp = s.substring(1, s.length() - 1).trim();
                return type;
            }
            List<String> options = new ArrayList<String>();
            while (true) {
                int comma = s.lastIndexOf(',');
                if (comma > 0) {
                    if (s.indexOf(')', comma) < 0) {
                        options.add(Str.trim(s.substring(comma + 1)));
                        s = s.substring(0, comma).trim();
                    } else break;
                } else break;
            }

            for (String option : options) {
                if (option == null) continue;
                int eq = option.indexOf('=');
                if (eq < 0) {
                    if (options.size() == 1)
                        type.javaType = option;
                } else {
                    String optionName = Str.trim(option.substring(0, eq));
                    String optionValue = Str.trim(option.substring(eq + 1));
                    if ("javaType".equals(optionName))
                        type.javaType = optionValue;
                    else if ("typeHandler".equals(optionName))
                        type.typeHandler = optionValue;
                }
            }

            if (SIMPLE.matcher(s).matches()) {
                type.name = s;
            } else {
                Matcher matcher = WITH_LEN.matcher(s);
                if (!matcher.matches())
                    throw new IllegalArgumentException("Invalid type definition: '"+s+"'");
                type.name = matcher.group(1);
                type.length = IntegerInterval.parse(matcher.group(2));
                if (matcher.groupCount() > 2)
                    type.scale = IntegerInterval.parse(matcher.group(3));
            }
            return type;
        }

        public boolean equals(Object o) {
            if (o instanceof IntrospectedColumn) {
                IntrospectedColumn c = (IntrospectedColumn)o;
                if (regexp != null) {
                    String nativeTypeName = c.getProperties().getProperty(NATIVE_TYPE);
                    if (nativeTypeName != null && nativeTypeName.matches(regexp))
                        return true;
                }
                if (!java.util.Objects.equals(name, c.getJdbcTypeName()))
                    return false;
                if (length != null && !length.contains(c.getLength()))
                    return false;
                if (scale != null && !scale.contains(c.getScale()))
                    return false;
                return true;
            }
            else return super.equals(o);
        }

        @Override
        public String toString() {
            return regexp == null ?  name + (length != null ? "("+length + (scale != null ? ", "+scale : "") + ")" : "") : ("/"+regexp+"/");
        }
    }


    @Override
    public boolean validate(List<String> warnings) {
        verbose = Bool.bool(Str.trim(properties.getProperty("verbose")), false);
        include = Pattern.compile(com.luca.mybatis.generator.plugins.el.Objects.nvl(Str.trim(properties.getProperty("tables")), ".+"));
        exclude = Pattern.compile(com.luca.mybatis.generator.plugins.el.Objects.nvl(Str.trim(properties.getProperty("excludeTables")), "^-$"));

        introspectNativeSQLTypes();
        for (Enumeration<?> names = properties.propertyNames(); names.hasMoreElements(); ) {
            String name = (String)names.nextElement();
            String value = properties.getProperty(name);
            name = Str.trim(name);
            if (name == null || !name.startsWith("from:")) continue;
            String from = name.substring("from:".length()).trim();
            try {
                convert.add(Pair.of(Type.parse(from), Type.parse(value)));
            } catch (IllegalArgumentException e) {
                warnings.add(e.getMessage());
                return false;
            }
        }
        introspect = Bool.bool(Str.trim(properties.getProperty("introspectColumns")), convert.isEmpty());
        return true;
    }

    private void introspectNativeSQLTypes() {
        Connection conn = null;
        try {
            conn = getConnection();
            DatabaseMetaData dbmd = conn.getMetaData();
            for (TableConfiguration tc : context.getTableConfigurations()) {
                String tableName = tc.getTableName();
                Map<String, ColumnInfo> map = new HashMap<String, ColumnInfo>();
                columnInfo.put(tableName, map);

                TableName tn = new TableName(tc, dbmd);

                ResultSet rset = dbmd.getColumns(tn.catalog, tn.schema, tn.table, null);

                while (rset.next()) {
                    ColumnInfo info = new ColumnInfo();
                    map.put(rset.getString("COLUMN_NAME"), info);
                    info.nativeTypeName = rset.getString("TYPE_NAME");
                }

                rset.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    private final static String NATIVE_TYPE = "nativeSQLTypeName";
    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getTableConfiguration().getTableName();
        if (!include.matcher(tableName).matches() || exclude.matcher(tableName).matches())
            return;

        Map<String, ColumnInfo> map = columnInfo.get(tableName);
        for (IntrospectedColumn c : introspectedTable.getAllColumns()) {
            ColumnInfo info = map.get(c.getActualColumnName());
            if (info != null)
                c.getProperties().setProperty(NATIVE_TYPE, info.nativeTypeName);

            if (introspect)
                System.out.println(String.format("Table %s, column %s - %s {%s} (%d, %d)",
                        introspectedTable.getFullyQualifiedTableNameAtRuntime(), c.getActualColumnName(),
                        c.getJdbcTypeName(), c.getProperties().getProperty(NATIVE_TYPE), c.getLength(), c.getScale()
                ));

            int patternNumber = 0;
            for (Pair<Type, Type> p : convert) {
                patternNumber++;
                if (p.a().equals(c)) {
                    String targetTypeName = p.b().name;
                    Integer targetType = typeToId.get(targetTypeName);
                    if (targetType == null) {
                        System.err.println("Unknown JDBC Type Name: '"+targetTypeName+"'");
                    } else {
                        boolean wasBLOB = c.isBLOBColumn();

                        int length = p.b().length != null ? p.b().length.max : c.getLength();
                        int scale = p.b().scale != null ? p.b().scale.max : c.getScale();
                        String targetJavaType = p.b().javaType != null ? p.b().javaType : c.getFullyQualifiedJavaType().getFullyQualifiedName();
                        if (verbose)
                            System.out.println(
                                String.format("Table %s, column %s - transform from %s (%d, %d) - %s to %s (%d, %d) - %s. Pattern %s",
                                    introspectedTable.getFullyQualifiedTableNameAtRuntime(), c.getActualColumnName(),
                                    c.getJdbcTypeName(), c.getLength(), c.getScale(), c.getFullyQualifiedJavaType().getFullyQualifiedName(),
                                    targetTypeName, length, scale, targetJavaType,
                                    p.a()
                            ));
                        c.setJdbcType(targetType);
                        c.setJdbcTypeName(targetTypeName);
                        c.setLength(length);
                        c.setScale(scale);
                        c.setFullyQualifiedJavaType(new FullyQualifiedJavaType(targetJavaType));
                        if (p.b().typeHandler != null)
                            c.setTypeHandler(p.b().typeHandler);

                        if (wasBLOB && !c.isBLOBColumn()) {
                            for (Iterator<IntrospectedColumn> it = introspectedTable.getBLOBColumns().iterator(); it.hasNext(); )
                                if (it.next() == c) {
                                    it.remove();
                                    introspectedTable.getBaseColumns().add(c);
                                    break;
                                }
                        }
                        else if (!wasBLOB && c.isBLOBColumn()) {
                            for (Iterator<IntrospectedColumn> it = introspectedTable.getBaseColumns().iterator(); it.hasNext(); )
                                if (it.next() == c) {
                                    it.remove();
                                    introspectedTable.getBLOBColumns().add(c);
                                    break;
                                }
                        }

                    }
                    break;
                }
            }
        }
    }

    private static final Map<String, Integer> typeToId = new HashMap<>();
    private static final Map<Integer, String> idToType  = new HashMap<>();

    static {
        Field[] constants = java.sql.Types.class.getFields();
        for (Field c : constants) {
            if (!Modifier.isStatic(c.getModifiers())) continue;
            if (c.getType() != int.class && c.getType() != Integer.class) continue;
            try {
                Integer value = (Integer)c.get(null);
                typeToId.put(c.getName(), value);
                idToType.put(value, c.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
