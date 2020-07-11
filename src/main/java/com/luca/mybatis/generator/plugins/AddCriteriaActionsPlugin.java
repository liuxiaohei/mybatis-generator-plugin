package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.el.Bool;
import com.luca.mybatis.generator.plugins.el.MyBatisClasses;
import com.luca.mybatis.generator.plugins.el.Objects;
import com.luca.mybatis.generator.plugins.el.Str;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.internal.rules.Rules;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import static org.mybatis.generator.api.dom.java.JavaVisibility.PUBLIC;
import static com.luca.mybatis.generator.plugins.el.MBGenerator.*;
import static com.luca.mybatis.generator.plugins.el.MBGenerator.FQJT.*;

/**
 * List<Foo> list = new FooExample().createCriteria().and.....list(sql);
 */
public class AddCriteriaActionsPlugin extends PluginAdapter {
    private static final String SKIP = "-";
    private static final FullyQualifiedJavaType sqlSession = new FullyQualifiedJavaType("org.apache.ibatis.session.SqlSession");

    private String listMethod;
    private String firstMethod;
    private String singleMethod;
    private String listWithBLOBsMethod;
    private String firstWithBLOBsMethod;
    private String singleWithBLOBsMethod;
    private String optionalMethod;
    private String optionalWithBLOBsMethod;
    private String deleteMethod;
    private String countMethod;
    private String updateMethod;
    private String updateWithBLOBsMethod;
    private String updateSelectiveMethod;
    private String updateSelectiveWithBLOBsMethod;

    private boolean generateCriteriaMethods;

    private Pattern userDefinedMethods;

    private TopLevelClass exampleClass;
    private Interface mapperClass;

    @Override
    public boolean validate(List<String> strings) {
        listMethod = Objects.nvl(Str.trim(properties.getProperty("listMethodName")), "list");
        listWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("listWithBLOBsMethodName")), listMethod+"WithBLOBs");
        firstMethod = Objects.nvl(Str.trim(properties.getProperty("firstMethodName")), "first");
        firstWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("firstWithBLOBsMethodName")), firstMethod+"WithBLOBs");
        singleMethod = Objects.nvl(Str.trim(properties.getProperty("singleMethodName")), "single");
        singleWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("singleWithBLOBsMethodName")), singleMethod+"WithBLOBs");
        optionalMethod = Objects.nvl(Str.trim(properties.getProperty("optionalMethodName")), "optional");
        optionalWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("optionalWithBLOBsMethodName")), optionalMethod+"WithBLOBs");
        countMethod = Objects.nvl(Str.trim(properties.getProperty("countMethodName")), "count");
        deleteMethod = Objects.nvl(Str.trim(properties.getProperty("deleteMethodName")), "delete");
        updateMethod = Objects.nvl(Str.trim(properties.getProperty("updateMethodName")), "update");
        updateWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("updateWithBLOBsMethodName")), updateMethod+"WithBLOBs");
        updateSelectiveMethod = Objects.nvl(Str.trim(properties.getProperty("updateSelectiveMethodName")), updateMethod+"Selective");
        updateSelectiveWithBLOBsMethod = Objects.nvl(Str.trim(properties.getProperty("updateSelectiveWithBLOBsMethodName")), updateSelectiveMethod+"WithBLOBs");

        generateCriteriaMethods = Bool.bool(Str.trim(properties.getProperty("generateCriteriaMethods")), true);

        userDefinedMethods = Pattern.compile(Objects.nvl(Str.trim(properties.getProperty("userDefinedMethods")), "^(.+WithNull)$"));

        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        MyBatisClasses cls = MyBatisClasses.calculate(topLevelClass, introspectedTable);
        topLevelClass.addImportedType(sqlSession);
        if (cls.imports.mapper != null)
            topLevelClass.addImportedType(cls.imports.mapper);

        int newMethodsStart = topLevelClass.getMethods().size();

        addListMethods(topLevelClass, introspectedTable, cls);
        addGetRecordMethods(topLevelClass, introspectedTable, cls, firstMethod, firstWithBLOBsMethod, "list == null || list.isEmpty() ? null : list.get(0)");
        addGetRecordMethods(topLevelClass, introspectedTable, cls, singleMethod, singleWithBLOBsMethod, "list == null || list.size() != 1 ? null : list.get(0)");
        addGetRecordMethods(topLevelClass, introspectedTable, cls, optionalMethod, optionalWithBLOBsMethod, "list == null || list.isEmpty() ? new @result() : list.get(0)");
        addUpdateMethods(topLevelClass, introspectedTable, cls, updateMethod, updateWithBLOBsMethod, false);
        addUpdateMethods(topLevelClass, introspectedTable, cls, updateSelectiveMethod, updateSelectiveWithBLOBsMethod, true);
        addDeleteMethods(topLevelClass, introspectedTable, cls);
        addCountMethods(topLevelClass, introspectedTable, cls);
        if (mapperClass != null) {
            addUserDefinedMethods(topLevelClass, mapperClass, introspectedTable, cls);
            mapperClass = null;
            exampleClass = null;
        } else {
            exampleClass = topLevelClass;
        }

        addCriteriaMethods(topLevelClass, newMethodsStart);

        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (topLevelClass != null)
            return false;
        if (exampleClass != null) {
            MyBatisClasses cls = MyBatisClasses.calculate(exampleClass, introspectedTable);
            int newMethodsStart = exampleClass.getMethods().size();
            addUserDefinedMethods(exampleClass, interfaze, introspectedTable, cls);
            addCriteriaMethods(exampleClass, newMethodsStart);
            exampleClass = null;
            mapperClass = null;
        } else {
            mapperClass = interfaze;
        }
        return true;
    }

    private void addCriteriaMethods(TopLevelClass topLevelClass, int newMethodsStart) {
        if (!generateCriteriaMethods) return;
        InnerClass criteria = null;
        for (InnerClass c : topLevelClass.getInnerClasses()) {
            if (c.getType().getShortName().equals("Criteria")) criteria = c;
        }
        if (criteria == null) return;
        boolean owner = false;
        for (Field f : criteria.getFields()) if (ExampleMethodsChainPlugin.OWNER.equals(f.getName())) owner = true;
        if (!owner) return;

        for (ListIterator<Method> methods = topLevelClass.getMethods().listIterator(newMethodsStart); methods.hasNext(); ) {
            Method base = methods.next();
            if (base.getVisibility() != PUBLIC || base.isStatic() || base.isConstructor()) continue;
            Method m = method(PUBLIC, base.getReturnType(), base.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("return ").append(ExampleMethodsChainPlugin.OWNER).append(".").append(base.getName()).append("(");
            for (ListIterator<Parameter> params = base.getParameters().listIterator(); params.hasNext(); ) {
                if (params.hasPrevious()) sb.append(", ");
                Parameter p = params.next();
                m.addParameter(new Parameter(p.getType(), p.getName()));
                sb.append(p.getName());
            }
            sb.append(");");
            m.addBodyLine(sb.toString());
            criteria.addMethod(m);
        }
    }

    private void addListMethods(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, MyBatisClasses cls) {
        String returnType = "java.util.List<"+cls.names.base+">";
        topLevelClass.addImportedType(cls.imports.base);
        if (!listMethod.startsWith(SKIP) && introspectedTable.getRules().generateSelectByExampleWithoutBLOBs()) {
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), listMethod, param(sqlSession, "sql"), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class).selectByExample(this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), listMethod, param(cls.types.mapper, "mapper"), body(
                    "return mapper.selectByExample(this);"
            )));
        }
        if (introspectedTable.hasBLOBColumns() && !listWithBLOBsMethod.startsWith(SKIP) && introspectedTable.getRules().generateSelectByExampleWithBLOBs()) {
            if (cls.exists.blob) {
                returnType = "java.util.List<" + cls.names.blob + ">";
                topLevelClass.addImportedType(cls.imports.blob);
            }
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), listWithBLOBsMethod, param(sqlSession, "sql"), body(
                    "return sql.getMapper("+cls.names.mapper+".class).selectByExampleWithBLOBs(this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), listWithBLOBsMethod, param(cls.types.mapper, "mapper"), body(
                    "return mapper.selectByExampleWithBLOBs(this);"
            )));
        }
    }

    private void addGetRecordMethods(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, MyBatisClasses cls, String base, String withBLOBs, String expression) {
        String returnType = cls.names.base;
        topLevelClass.addImportedType(cls.imports.base);
        String listType = "java.util.List<"+returnType+">";
        if (!base.startsWith(SKIP) && introspectedTable.getRules().generateSelectByExampleWithoutBLOBs()) {
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), base, param(sqlSession, "sql"), body(
                    listType + " list = sql.getMapper(" + cls.names.mapper + ".class).selectByExample(this);",
                    "return "+expression.replace("@result", returnType)+";"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), base, param(cls.types.mapper, "mapper"), body(
                    listType + " list = mapper.selectByExample(this);",
                    "return "+expression.replace("@result", returnType)+";"
            )));
        }
        if (introspectedTable.hasBLOBColumns() && !withBLOBs.startsWith(SKIP) && introspectedTable.getRules().generateSelectByExampleWithBLOBs()) {
            if (cls.exists.blob) {
                returnType = cls.names.blob;
                topLevelClass.addImportedType(cls.imports.blob);
            }
            listType = "java.util.List<"+returnType+">";
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), withBLOBs, param(sqlSession, "sql"), body(
                    listType + " list = sql.getMapper(" + cls.names.mapper + ".class).selectByExampleWithBLOBs(this);",
                    "return "+expression.replace("@result", returnType)+";"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, new FullyQualifiedJavaType(returnType), withBLOBs, param(cls.types.mapper, "mapper"), body(
                    listType + " list = mapper.selectByExampleWithBLOBs(this);",
                    "return "+expression.replace("@result", returnType)+";"
            )));
        }
    }

    private void addUpdateMethods(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, MyBatisClasses cls, String base, String withBLOBs, boolean selective) {
        String record = cls.names.base;
        topLevelClass.addImportedType(cls.imports.base);
        String mapperMethod = selective ? "updateByExampleSelective" : "updateByExample";
        Rules r = introspectedTable.getRules();
        if (!base.startsWith(SKIP) && ( selective ? r.generateUpdateByExampleSelective() : r.generateUpdateByExampleWithoutBLOBs() )) {
            if (selective && r.generateRecordWithBLOBsClass()) {
                record = cls.names.blob;
                topLevelClass.addImportedType(cls.imports.blob);
            }
            topLevelClass.addMethod(method(
                PUBLIC, INT, base, param(sqlSession, "sql"), param(new FullyQualifiedJavaType(record), "record"), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class)."+mapperMethod+"(record, this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, INT, base, param(cls.types.mapper, "mapper"), param(new FullyQualifiedJavaType(record), "record"), body(
                    "return mapper."+mapperMethod+"(record, this);"
            )));
        }
        if (introspectedTable.hasBLOBColumns() && !withBLOBs.startsWith(SKIP) && !selective && r.generateUpdateByExampleWithBLOBs()) {
            if (r.generateRecordWithBLOBsClass()) {
                record = cls.names.blob;
                topLevelClass.addImportedType(cls.imports.blob);
            }
            mapperMethod = selective ? "updateByExampleSelectiveWithBLOBs" /* not supported */ : "updateByExampleWithBLOBs";
            topLevelClass.addMethod(method(
                PUBLIC, INT, withBLOBs, param(sqlSession, "sql"), param(new FullyQualifiedJavaType(record), "record"), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class)."+mapperMethod+"(record, this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, INT, withBLOBs, param(cls.types.mapper, "mapper"), param(new FullyQualifiedJavaType(record), "record"), body(
                    "return mapper."+mapperMethod+"(record, this);"
            )));
        }
    }

    private void addDeleteMethods(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, MyBatisClasses cls) {
        if (!deleteMethod.startsWith(SKIP) && introspectedTable.getRules().generateDeleteByExample()) {
            topLevelClass.addMethod(method(
                PUBLIC, INT, deleteMethod, param(sqlSession, "sql"), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class).deleteByExample(this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, INT, deleteMethod, param(cls.types.mapper, "mapper"), body(
                    "return mapper.deleteByExample(this);"
            )));
        }
    }

    private void addCountMethods(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, MyBatisClasses cls) {
        if (!countMethod.startsWith(SKIP) && introspectedTable.getRules().generateCountByExample()) {
            topLevelClass.addMethod(method(
                PUBLIC, INT, countMethod, param(sqlSession, "sql"), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class).countByExample(this);"
            )));
            topLevelClass.addMethod(method(
                PUBLIC, INT, countMethod, param(cls.types.mapper, "mapper"), body(
                    "return mapper.countByExample(this);"
            )));
        }
    }

    private void addUserDefinedMethods(TopLevelClass exampleClass, Interface mapperClass, IntrospectedTable introspectedTable, MyBatisClasses cls) {
        for (Method action : mapperClass.getMethods()) {
            if (!userDefinedMethods.matcher(action.getName()).matches()) continue;
            StringBuilder args = new StringBuilder();
            List<Parameter> params = new ArrayList<Parameter>();
            boolean example = false;
            if (action.getParameters() != null)
                for (Parameter param : action.getParameters()) {
                    String name;
                    if (Objects.equals(param.getType(), exampleClass.getType())) {
                        example = true;
                        name = "this";
                    } else {
                        name = param.getName();
                        params.add(new Parameter(param.getType(), name));
                    }
                    if (args.length() > 0)
                        args.append(", ");
                    args.append(name);
                }
            if (!example) {
                //System.err.println("Invalid user-defined mapper method: "+action.getName());
                continue;
            }

            exampleClass.addMethod(method(
                PUBLIC, INT, action.getName(), param(sqlSession, "sql"), params.toArray(new Parameter[params.size()]), body(
                    "return sql.getMapper(" + cls.names.mapper + ".class)."+action.getName()+"("+args+");"
            )));
            exampleClass.addMethod(method(
                PUBLIC, INT, action.getName(), param(cls.types.mapper, "mapper"), params.toArray(new Parameter[params.size()]), body(
                    "return mapper."+action.getName()+"("+args+");"
            )));
        }
    }
}
