package com.luca.mybatis.generator.plugins;

import java.util.List;
import java.util.regex.Pattern;

import com.luca.mybatis.generator.plugins.el.MBGenerator;
import com.luca.mybatis.generator.plugins.el.Messages;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * Adds custom criterion to specified *Example-class.
 */
public class AddComplexCriteriaPlugin extends PluginAdapter {
    private Pattern target;
    private String methodName;
    private String methodBody;

    @Override
    public boolean validate(List<String> warnings) {
        String target = properties.getProperty("targetTable");
        if (target == null) {
            warnings.add(Messages.load(this).get("requiredProperty", "targetTable"));
            return false;
        }
        this.target = Pattern.compile(target);

        methodName = properties.getProperty("methodName");
        if (methodName == null) {
            warnings.add(Messages.load(this).get("requiredProperty", "methodName"));
            return false;
        }
        methodBody = properties.getProperty("methodBody");
        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String table = MBGenerator.tableName(introspectedTable);
        if (!target.matcher(table).matches())
            return true;

        InnerClass generatedCriteria = null;

        for (InnerClass innerClass : topLevelClass.getInnerClasses()) {
            if ("GeneratedCriteria".equals(innerClass.getType().getShortName())) {
                generatedCriteria = innerClass;
                break;
            }
        }
        if (generatedCriteria == null) return false;

        Method m = new Method();
        m.setName(methodName);
        m.setVisibility(JavaVisibility.PUBLIC);
        m.setReturnType(FullyQualifiedJavaType.getCriteriaInstance());
        if (methodBody != null)
            m.addBodyLine(methodBody);
        m.addBodyLine("return (Criteria)this;");
        generatedCriteria.addMethod(m);

        return true;
    }
}
