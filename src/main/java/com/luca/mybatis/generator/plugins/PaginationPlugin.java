package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.utils.PluginUtils;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.List;

/**
 ##PaginationPlugin
 ###Java sample
 AccountMapper mapper = ...
 AccountExample example = new AccountExample();
 example.page(0, 2).createCriteria().andAgeEqualTo(33);
 List<Account> as = mapper.selectByExample(example);
 */
public class PaginationPlugin extends PluginAdapter {
    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        PluginUtils.addProperty(OFFSET, INTEGER_TYPE, topLevelClass, this.getContext(), tableName);
        PluginUtils.addProperty(LIMIT, INTEGER_TYPE, topLevelClass, this.getContext(), tableName);

        Method method = new Method();
        method.setName("page");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(topLevelClass.getType());
        method.addBodyLine("this.offset = offset;");
        method.addBodyLine("this.limit = limit;");
        method.addBodyLine("return this;");
        method.addParameter(new Parameter(FullyQualifiedJavaType.getIntInstance(), "offset"));
        method.addParameter(new Parameter(FullyQualifiedJavaType.getIntInstance(), "limit"));
        PluginUtils.addDoc(this.getContext(), method, tableName);
        topLevelClass.getMethods().add(method);

        System.out.println("-----------------" + topLevelClass.getType().getShortName() + " add method=page for pagination.");
        return true;
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<Element> els = element.getElements();
        els.add(new TextElement(template));
        return true;
    }

    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<Element> els = element.getElements();
        els.add(new TextElement(template));
        return true;
    }

    private final static FullyQualifiedJavaType INTEGER_TYPE = new FullyQualifiedJavaType("java.lang.Integer");
    private static final String OFFSET = "offset";
    private static final String LIMIT = "limit";
    private static final String template =
            "<if test=\"offset != null and limit != null\">\n" +
            "      limit ${offset}, ${limit}\n" +
            "    </if>";
}
