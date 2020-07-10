package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.utils.InnerInterface;
import com.luca.mybatis.generator.plugins.utils.InnerInterfaceWrapperToInnerClass;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.internal.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Example 增强插件
 */
public class ExampleEnhancedPlugin extends PluginAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ExampleEnhancedPlugin.class);

    @Override
    public boolean validate(List<String> warnings) {
        // 插件使用前提是targetRuntime为MyBatis3
        if (StringUtility.stringHasValue(getContext().getTargetRuntime()) && !"MyBatis3".equalsIgnoreCase(getContext().getTargetRuntime())) {
            logger.warn("chrm:插件" + "ExampleEnhancedPlugin" + "要求运行targetRuntime必须为MyBatis3！");
            return false;
        }
        return true;
    }

    /**
     * ModelExample Methods 生成
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     */
    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        List<InnerClass> innerClasses = topLevelClass.getInnerClasses();
        for (InnerClass innerClass : innerClasses) {
            if ("Criteria".equals(innerClass.getType().getShortName())) {
                addFactoryMethodToCriteria(topLevelClass, innerClass);
                addAndIfMethodToCriteria(topLevelClass, innerClass);
            }
        }

        List<Method> methods = topLevelClass.getMethods();
        for (Method method : methods) {
            if (!"createCriteriaInternal".equals(method.getName()))
                continue;
            method.getBodyLines().set(0, "Criteria criteria = new Criteria(this);");
            logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "修改createCriteriaInternal方法，修改构造Criteria时传入Example对象");
        }

        // orderBy方法
        addOrderByMethodToExample(topLevelClass);

        return true;
    }

    /**
     * 添加工厂方法
     */
    private void addFactoryMethodToCriteria(TopLevelClass topLevelClass, InnerClass innerClass) {
        Field f = new Field("example", topLevelClass.getType());
        f.setVisibility(JavaVisibility.PRIVATE);
        innerClass.addField(f);

        // overwrite constructor
        List<Method> methods = innerClass.getMethods();
        for (Method method : methods) {
            if (method.isConstructor()) {
                method.addParameter(new Parameter(topLevelClass.getType(), "example"));
                method.addBodyLine("this.example = example;");
                logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "修改构造方法，增加example参数");
            }
        }

        // add factory method "example"
        Method method = new Method("example");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(topLevelClass.getType());
        method.addBodyLine("return this.example;");
        innerClass.addMethod(method);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "." + innerClass.getType().getShortName() + "增加工厂方法example");
    }


    /**
     * 增强Criteria的链式调用，添加andIf(boolean addIf, CriteriaAdd add)方法，实现链式调用中按条件增加查询语句
     */
    private void addAndIfMethodToCriteria(TopLevelClass topLevelClass, InnerClass innerClass) {
        // 添加接口CriteriaAdd
        InnerInterface criteriaAddInterface = new InnerInterface("ICriteriaAdd");
        criteriaAddInterface.setVisibility(JavaVisibility.PUBLIC);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "." + innerClass.getType().getShortName() + "增加接口ICriteriaAdd");

        // ICriteriaAdd增加接口add
        Method addMethod = new Method("add");
        addMethod.setReturnType(innerClass.getType());
        addMethod.addParameter(new Parameter(innerClass.getType(), "add"));
        criteriaAddInterface.addMethod(addMethod);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "." + innerClass.getType().getShortName() + "." + criteriaAddInterface.getType().getShortName() + "增加方法add");

        InnerClass innerClassWrapper = new InnerInterfaceWrapperToInnerClass(criteriaAddInterface);
        innerClass.addInnerClass(innerClassWrapper);

        // 添加andIf方法
        Method method = new Method("andIf");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(innerClass.getType());
        method.addParameter(new Parameter(FullyQualifiedJavaType.getBooleanPrimitiveInstance(), "ifAdd"));
        method.addParameter(new Parameter(criteriaAddInterface.getType(), "add"));

        method.addBodyLine("if (ifAdd) {");
        method.addBodyLine("add.add(this);");
        method.addBodyLine("}");
        method.addBodyLine("return this;");
        innerClass.addMethod(method);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "." + innerClass.getType().getShortName() + "增加方法andIf");
    }

    /**
     * Example增强了setOrderByClause方法，新增orderBy(String orderByClause)方法直接返回example，增强链式调用，可以一路.下去了。
     */
    private void addOrderByMethodToExample(TopLevelClass topLevelClass) {
        // 添加orderBy
        Method method = new Method("orderBy");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(topLevelClass.getType());
        method.addParameter(new Parameter(FullyQualifiedJavaType.getStringInstance(), "orderByClause"));

        method.addBodyLine("this.setOrderByClause(orderByClause);");
        method.addBodyLine("return this;");

        topLevelClass.addMethod(method);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法orderBy");

        // 添加orderBy
        Method mOrderByMore = new Method("orderBy");
        mOrderByMore.setVisibility(JavaVisibility.PUBLIC);
        mOrderByMore.setReturnType(topLevelClass.getType());
        mOrderByMore.addParameter(new Parameter(FullyQualifiedJavaType.getStringInstance(), "orderByClauses", true));

        mOrderByMore.addBodyLine("StringBuffer sb = new StringBuffer();");
        mOrderByMore.addBodyLine("for (int i = 0; i < orderByClauses.length; i++) {");
        mOrderByMore.addBodyLine("sb.append(orderByClauses[i]);");
        mOrderByMore.addBodyLine("if (i < orderByClauses.length - 1) {");
        mOrderByMore.addBodyLine("sb.append(\" , \");");
        mOrderByMore.addBodyLine("}");
        mOrderByMore.addBodyLine("}");
        mOrderByMore.addBodyLine("this.setOrderByClause(sb.toString());");
        mOrderByMore.addBodyLine("return this;");

        topLevelClass.addMethod(mOrderByMore);
        logger.debug("chrm(Example增强插件):" + topLevelClass.getType().getShortName() + "增加方法orderBy(String ... orderByClauses)");
    }
}
