package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.utils.CommTools;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.internal.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ---------------------------------------------------------------------------
 * 存在即更新插件
 */
public class UpsertPlugin extends PluginAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BatchInsertPlugin.class);
    public static final String METHOD_UPSERT = "upsert";  // 方法名
    public static final String METHOD_UPSERT_SELECTIVE = "upsertSelective";  // 方法名

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate(List<String> warnings) {
        // 插件使用前提是targetRuntime为MyBatis3
        if (StringUtility.stringHasValue(getContext().getTargetRuntime()) && !"MyBatis3".equalsIgnoreCase(getContext().getTargetRuntime())) {
            logger.warn("chrm:插件" + "UpsertPlugin" + "要求运行targetRuntime必须为MyBatis3！");
            return false;
        }
        // 插件使用前提是数据库为MySQL
        if (!"com.mysql.jdbc.Driver".equalsIgnoreCase(this.getContext().getJdbcConnectionConfiguration().getDriverClass())) {
            logger.warn("chrm:插件" + "UpsertPlugin" + "插件使用前提是数据库为MySQL！");
            return false;
        }

        return true;
    }

    /**
     * Java Client Methods 生成
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     */
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // ====================================== 1. upsert ======================================
        Method mUpsert = new Method(METHOD_UPSERT);
        // 返回值类型
        mUpsert.setReturnType(FullyQualifiedJavaType.getIntInstance());
        // 添加参数
        mUpsert.addParameter(new Parameter(introspectedTable.getRules().calculateAllFieldsClass(), "record"));
        // interface 增加方法
        interfaze.addMethod(mUpsert);
        logger.debug("chrm(存在即更新插件):" + interfaze.getType().getShortName() + "增加upsert方法。");

        // ====================================== 2. upsertSelective ======================================
        Method mUpsertSelective = new Method(METHOD_UPSERT_SELECTIVE);
        // 返回值类型
        mUpsertSelective.setReturnType(FullyQualifiedJavaType.getIntInstance());
        // 添加参数
        mUpsertSelective.addParameter(new Parameter(introspectedTable.getRules().calculateAllFieldsClass(), "record"));
        // interface 增加方法
        interfaze.addMethod(mUpsertSelective);
        logger.debug("chrm(存在即更新插件):" + interfaze.getType().getShortName() + "增加upsertSelective方法。");

        return true;
    }

    /**
     * SQL Map Methods 生成
     * 具体执行顺序 http://www.mybatis.org/generator/reference/pluggingIn.html
     */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {

        // ====================================== 1. upsert ======================================
        XmlElement eleUpsert = new XmlElement("insert");
        eleUpsert.addAttribute(new Attribute("id", METHOD_UPSERT));

        // 参数类型
        eleUpsert.addAttribute(new Attribute("parameterType", introspectedTable.getRules().calculateAllFieldsClass().getFullyQualifiedName()));

        // 使用JDBC的getGenereatedKeys方法获取主键并赋值到keyProperty设置的领域模型属性中。所以只支持MYSQL和SQLServer
        CommTools.useGeneratedKeys(eleUpsert, introspectedTable);

        // insert
        eleUpsert.addElement(new TextElement("insert into " + introspectedTable.getFullyQualifiedTableNameAtRuntime()));
        eleUpsert.addElement(this.generateInsertClause(introspectedTable));
        eleUpsert.addElement(new TextElement("values"));
        eleUpsert.addElement(this.generateValuesClause(introspectedTable));
        eleUpsert.addElement(new TextElement("on duplicate key update "));
        eleUpsert.addElement(this.generateDuplicateClause(introspectedTable));

        document.getRootElement().addElement(eleUpsert);
        logger.debug("chrm(存在即更新插件):" + introspectedTable.getMyBatis3XmlMapperFileName() + "增加upsert实现方法。");

        // ====================================== 2. upsertSelective ======================================
        XmlElement eleUpsertSelective = new XmlElement("insert");
        eleUpsertSelective.addAttribute(new Attribute("id", METHOD_UPSERT_SELECTIVE));

        // 参数类型
        eleUpsertSelective.addAttribute(new Attribute("parameterType", introspectedTable.getRules().calculateAllFieldsClass().getFullyQualifiedName()));

        // 使用JDBC的getGenereatedKeys方法获取主键并赋值到keyProperty设置的领域模型属性中。所以只支持MYSQL和SQLServer
        CommTools.useGeneratedKeys(eleUpsertSelective, introspectedTable);

        // insert
        eleUpsertSelective.addElement(new TextElement("insert into " + introspectedTable.getFullyQualifiedTableNameAtRuntime()));
        eleUpsertSelective.addElement(this.generateInsertSelectiveClause(introspectedTable));
        eleUpsertSelective.addElement(new TextElement("values"));
        eleUpsertSelective.addElement(this.generateValuesSelectiveClause(introspectedTable));
        eleUpsertSelective.addElement(new TextElement("on duplicate key update "));
        eleUpsertSelective.addElement(this.generateDuplicateSelectiveClause(introspectedTable));

        document.getRootElement().addElement(eleUpsertSelective);
        logger.debug("chrm(存在即更新插件):" + introspectedTable.getMyBatis3XmlMapperFileName() + "增加upsertSelective实现方法。");

        return true;
    }

    /**
     * 普通insert
     */
    private Element generateInsertClause(IntrospectedTable introspectedTable) {
        StringBuilder insertClause = new StringBuilder();

        insertClause.append(" (");

        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn introspectedColumn = columns.get(i);
            insertClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            if (i + 1 < columns.size()) {
                insertClause.append(", ");
            }
        }

        insertClause.append(") ");

        return new TextElement(insertClause.toString());
    }

    /**
     *
     */
    private Element generateValuesClause(IntrospectedTable introspectedTable) {
        StringBuilder valuesClause = new StringBuilder();

        valuesClause.append(" (");

        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn introspectedColumn = columns.get(i);

            valuesClause.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
            if (i + 1 < columns.size()) {
                valuesClause.append(", ");
            }
        }

        valuesClause.append(") ");

        return new TextElement(valuesClause.toString());
    }

    /**
     * 普通duplicate
     */
    private Element generateDuplicateClause(IntrospectedTable introspectedTable) {
        StringBuilder duplicateClause = new StringBuilder();

        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn introspectedColumn = columns.get(i);

            duplicateClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            duplicateClause.append(" = ");
            duplicateClause.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));

            if (i + 1 < columns.size()) {
                duplicateClause.append(", ");
            }
        }

        return new TextElement(duplicateClause.toString());
    }

    /**
     * 普通insert
     */
    private Element generateInsertSelectiveClause(IntrospectedTable introspectedTable) {
        XmlElement insertTrimEle = new XmlElement("trim");
        insertTrimEle.addAttribute(new Attribute("prefix", "("));
        insertTrimEle.addAttribute(new Attribute("suffix", ")"));
        insertTrimEle.addAttribute(new Attribute("suffixOverrides", ","));

        StringBuilder sb = new StringBuilder();
        for (IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {

            XmlElement insertNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null");
            insertNotNullElement.addAttribute(new Attribute("test", sb.toString()));

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(',');
            insertNotNullElement.addElement(new TextElement(sb.toString()));
            insertTrimEle.addElement(insertNotNullElement);
        }

        return insertTrimEle;
    }

    /**
     * 普通 values
     */
    private Element generateValuesSelectiveClause(IntrospectedTable introspectedTable) {
        XmlElement valuesTrimEle = new XmlElement("trim");
        valuesTrimEle.addAttribute(new Attribute("prefix", "("));
        valuesTrimEle.addAttribute(new Attribute("suffix", ")"));
        valuesTrimEle.addAttribute(new Attribute("suffixOverrides", ","));

        StringBuilder sb = new StringBuilder();
        for (IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {

            XmlElement valuesNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null");
            valuesNotNullElement.addAttribute(new Attribute("test", sb.toString()));

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
            sb.append(',');
            valuesNotNullElement.addElement(new TextElement(sb.toString()));
            valuesTrimEle.addElement(valuesNotNullElement);
        }
        return valuesTrimEle;
    }

    /**
     * 普通duplicate
     */
    private Element generateDuplicateSelectiveClause(IntrospectedTable introspectedTable) {
        XmlElement duplicateTrimEle = new XmlElement("trim");
        duplicateTrimEle.addAttribute(new Attribute("suffixOverrides", ","));

        StringBuilder sb = new StringBuilder();
        for (IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
            XmlElement duplicateNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null");
            duplicateNotNullElement.addAttribute(new Attribute("test", sb.toString()));
            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(" = ");
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
            sb.append(",");

            duplicateNotNullElement.addElement(new TextElement(sb.toString()));
            duplicateTrimEle.addElement(duplicateNotNullElement);
        }
        return duplicateTrimEle;
    }

}