package com.luca.mybatis.generator.plugins;

import com.luca.mybatis.generator.plugins.utils.PluginUtils;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;

import java.util.List;

/**
 * ##WhereSqlTextPlugin
 * ###Java sample
 *
 * AccountMapper mapper = ...
 * int v = 1;
 * AccountExample example = new AccountExample();
 * example.createCriteria().andAgeEqualTo(33).addConditionSql("version =" + v + " + 1");
 * List<Account> as = mapper.selectByExample(example);
 * 如果使用了SliceTablePlugin，别忘了对分表因子赋值：example.partitionFactorCreateTime(...)
 * WARNING：由于addConditionSql的参数是String类型，在 Mybatis3 的 mapper xml 中生成${}变量，这种变量不会做sql转义，而直接嵌入到sql语句中。如果以用户输入作为addConditionSql的参数，会导致潜在的SQL Injection攻击，需谨慎使用。
 */
public class WhereSqlTextPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        for (InnerClass innerClass : topLevelClass.getInnerClasses()) {
            if (FullyQualifiedJavaType.getGeneratedCriteriaInstance().equals(innerClass.getType())) {
                String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
                Method method = new Method();
                method.setName("addConditionSql");
                method.setVisibility(JavaVisibility.PUBLIC);
                method.setReturnType(FullyQualifiedJavaType.getCriteriaInstance());
                method.addBodyLine("addCriterion(conditionSql);");
                method.addBodyLine("return (Criteria) this;");
                method.addParameter(new Parameter(FullyQualifiedJavaType.getStringInstance(), "conditionSql"));
                PluginUtils.addDoc(this.getContext(), method, tableName);
                innerClass.getMethods().add(method);
                System.out.println("-----------------" + topLevelClass.getType().getShortName() + " add method=addConditionSql for custom sql statement in where clause.");
            }
        }
        return true;
    }
}
