package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.el.Bool;
import com.luca.mybatis.generator.plugins.el.Str;
import org.mybatis.generator.api.*;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.TableConfiguration;
import org.mybatis.generator.internal.PluginAggregator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Provides global configuration parameters (alias, ignoreQualifiersAtRuntime, modelOnly, runtimeCatalog, runtimeSchema) for <table>
 */
public class CommonTablePropertiesPlugin extends PluginAdapter {

    private final Map<String, Set<String>> modified = new HashMap<>();

    @Override
    public boolean validate(List<String> strings) {
        String TABLES = "tables";
        String EXCLUDE_TABLES = "excludeTables";
        Pattern include = Pattern.compile(com.luca.mybatis.generator.plugins.el.Objects.nvl(Str.trim(properties.getProperty(TABLES)), ".+"));
        Pattern exclude = Pattern.compile(com.luca.mybatis.generator.plugins.el.Objects.nvl(Str.trim(properties.getProperty(EXCLUDE_TABLES)), "^-$"));
        for (TableConfiguration tc : context.getTableConfigurations()) {
            String tableName = tc.getTableName();
            if (!include.matcher(tableName).matches() || exclude.matcher(tableName).matches())
                continue;
            Set<String> set = modified.computeIfAbsent(tableName, k -> new HashSet<>());
            Properties p = tc.getProperties();
            for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
                String name = (String)e.nextElement();
                if (TABLES.equals(name) || EXCLUDE_TABLES.equals(name)) continue;

                String value = properties.getProperty(name);
                if ("alias".equals(name)) {
                    if (Str.trim(tc.getAlias()) == null) {
                        tc.setAlias(value);
                        set.add(name);
                    }
                }
                else if (!p.containsKey(name)) {
                    p.setProperty(name, value);
                    set.add(name);
                }
            }
        }
        return true;
    }

    private IntrospectedTable helloKitty;
    private static boolean pluginDisabled;

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        if (pluginDisabled) return;
        if (introspectedTable == helloKitty) {
            pluginDisabled = true;
            return;
        }
        Set<String> set = this.modified.get(introspectedTable.getTableConfiguration().getTableName());
        if (set == null || set.isEmpty())
            return;
        boolean modified;
        modified = processIgnoreQualifiersAtRuntime(introspectedTable);
        modified = processRuntimeCatalog(introspectedTable) || modified;
        modified = processRuntimeSchema(introspectedTable) || modified;
        modified = processAlias(introspectedTable) || modified;
        if (set.contains("modelOnly"))
            modified = true;

        if (modified)
            reinitialize(introspectedTable);

    }

    private void reinitialize(IntrospectedTable introspectedTable) {
        try {
            helloKitty = introspectedTable;
            Plugin plugins = context.getPlugins();
            pluginAggregator.set(context, new PluginAggregator());
            introspectedTable.initialize();
            pluginAggregator.set(context, plugins);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean processIgnoreQualifiersAtRuntime(IntrospectedTable introspectedTable) {
        FullyQualifiedTable t = introspectedTable.getFullyQualifiedTable();

        try {
            Boolean b = (Boolean)ignoreQualifiersAtRuntime.get(introspectedTable.getFullyQualifiedTable());
            boolean property = Bool.bool(introspectedTable.getTableConfigurationProperty("ignoreQualifiersAtRuntime"), false);
            if (b == property)
                return false;
            ignoreQualifiersAtRuntime.set(t, property);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean processRuntimeSchema(IntrospectedTable introspectedTable) {
        FullyQualifiedTable t = introspectedTable.getFullyQualifiedTable();

        try {
            String value = (String)runtimeSchema.get(introspectedTable.getFullyQualifiedTable());
            String property = introspectedTable.getTableConfigurationProperty("runtimeSchema");
            if (value != null && com.luca.mybatis.generator.plugins.el.Objects.equals(Str.trim(value), Str.trim(property)) || value == null && property == null)
                return false;
            runtimeSchema.set(t, property);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean processRuntimeCatalog(IntrospectedTable introspectedTable) {
        FullyQualifiedTable t = introspectedTable.getFullyQualifiedTable();

        try {
            String value = (String)runtimeCatalog.get(introspectedTable.getFullyQualifiedTable());
            String property = introspectedTable.getTableConfigurationProperty("runtimeCatalog");
            if (value != null && com.luca.mybatis.generator.plugins.el.Objects.equals(Str.trim(value), Str.trim(property)) || value == null && property == null)
                return false;
            runtimeCatalog.set(t, property);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean processAlias(IntrospectedTable introspectedTable) {
        FullyQualifiedTable t = introspectedTable.getFullyQualifiedTable();

        try {
            String value = (String)alias.get(introspectedTable.getFullyQualifiedTable());
            String property = introspectedTable.getTableConfiguration().getAlias();
            if (value != null && com.luca.mybatis.generator.plugins.el.Objects.equals(Str.trim(value), Str.trim(property)) || value == null && property == null)
                return false;
            alias.set(t, property);
            for (IntrospectedColumn c : introspectedTable.getAllColumns())
                c.setTableAlias(property);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Field ignoreQualifiersAtRuntime;
    private static Field pluginAggregator;
    private static Field runtimeSchema;
    private static Field runtimeCatalog;
    private static Field alias;

    static {
        try {
            ignoreQualifiersAtRuntime = FullyQualifiedTable.class.getDeclaredField("ignoreQualifiersAtRuntime");
            ignoreQualifiersAtRuntime.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            runtimeSchema = FullyQualifiedTable.class.getDeclaredField("runtimeSchema");
            runtimeSchema.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            runtimeCatalog = FullyQualifiedTable.class.getDeclaredField("runtimeCatalog");
            runtimeCatalog.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            alias = FullyQualifiedTable.class.getDeclaredField("alias");
            alias.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            pluginAggregator = Context.class.getDeclaredField("pluginAggregator");
            pluginAggregator.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
