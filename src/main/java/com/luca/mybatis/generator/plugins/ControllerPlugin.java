package com.luca.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.DefaultJavaFormatter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.StringUtility;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 参照Servce 也自动生成一个Controller
 */
public class ControllerPlugin extends PluginAdapter {

    private String targetProject;
    private String targetPackage;

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        String targetProject = this.properties.getProperty("targetProject");
        if (StringUtility.stringHasValue(targetProject)) {
            this.targetProject = targetProject;
        } else {
            throw new RuntimeException("targetProject 属性不能为空！");
        }
        String targetPackage = this.properties.getProperty("targetPackage");
        if (StringUtility.stringHasValue(targetPackage)) {
            this.targetPackage = targetPackage;
        } else {
            throw new RuntimeException("targetPackage 属性不能为空！");
        }
    }

    /**
     */
    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
        String domainObjectName = introspectedTable.getFullyQualifiedTable().getDomainObjectName();
        String controller = targetPackage + "." + domainObjectName + "Controller";
        TopLevelClass topLevelClass = new TopLevelClass(new FullyQualifiedJavaType(controller));
//        topLevelClass.addImportedType(entityType);
        topLevelClass.addImportedType(new FullyQualifiedJavaType(controller));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("org.springframework.web.bind.annotation.RestController"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));
        topLevelClass.addAnnotation("@RestController(\"" + firstLetterLowerCase(domainObjectName + "Controller") + "\")");
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        setMapperField(introspectedTable, topLevelClass);
        return Collections.singletonList(new GeneratedJavaFile(topLevelClass, targetProject, new DefaultJavaFormatter()));
    }

    private void setMapperField(IntrospectedTable introspectedTable, TopLevelClass clazz) {
        // 实体类的类名
        String domainObjectName = introspectedTable.getFullyQualifiedTable().getDomainObjectName();
        // Mapper类所在包的包名
        String servicePackage = introspectedTable.getContext().getJavaClientGeneratorConfiguration().getTargetPackage().replace("mapper","service");
        Field serviceField = new Field();
        // 设置Field的注解
        serviceField.addAnnotation("@Autowired");
        serviceField.setVisibility(JavaVisibility.PRIVATE);
        // 设置Field的类型
        serviceField.setType(new FullyQualifiedJavaType(domainObjectName + "Service"));
        // 设置Field的名称
        serviceField.setName(firstLetterLowerCase(domainObjectName) + "Service");
        // 将Field添加到对应的类中
        clazz.addField(serviceField);
        // 对应的类需要import Mapper类(使用全限定类名)
        clazz.addImportedType(new FullyQualifiedJavaType(servicePackage + "." + domainObjectName + "Service"));
    }

    private String firstLetterLowerCase(String name) {
        char c = name.charAt(0);
        if (c >= 'A' && c <= 'Z') {
            String temp = String.valueOf(c);
            return name.replaceFirst(temp, temp.toLowerCase());
        }
        return name;
    }
}

