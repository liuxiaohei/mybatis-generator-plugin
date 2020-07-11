package com.luca.mybatis.generator.plugins;

import java.util.List;

import com.luca.mybatis.generator.plugins.el.DocumentFragment;
import com.luca.mybatis.generator.plugins.el.Messages;
import com.luca.mybatis.generator.plugins.el.TextReplacer;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.XmlElement;

import static org.mybatis.generator.api.dom.java.JavaVisibility.PRIVATE;
import static org.mybatis.generator.api.dom.java.JavaVisibility.PROTECTED;
import static org.mybatis.generator.api.dom.java.JavaVisibility.PUBLIC;
import static com.luca.mybatis.generator.plugins.el.MBGenerator.*;
import static com.luca.mybatis.generator.plugins.el.MBGenerator.FQJT.*;

/**
 * Provides the ability to use criteria like "... and (a or b or c) and ..."
 */
public class SimpleOrCriteriaPlugin extends PluginAdapter {

    public boolean validate(List<String> warnings) {
        return true;
    }

    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        InnerClass generatedCriteria = null;
        InnerClass criterion = null;
        InnerClass criteria = null;

        for (InnerClass innerClass : topLevelClass.getInnerClasses()) {
            String name = innerClass.getType().getShortName();
            if ("GeneratedCriteria".equals(name)) {
                generatedCriteria = innerClass;
            }
            else if ("Criterion".equals(name)) {
                criterion = innerClass;
            }
            else if ("Criteria".equals(name)) {
                criteria = innerClass;
            }
        }
        if (generatedCriteria == null || criterion == null || criteria == null)
            return false;

        boolean criteriaContextPlugin = false;
        for (Field f : criteria.getFields()) {
            if (criteriaContextPlugin = "_owner".equals(f.getName()))
                break;
        }
        if (!criteriaContextPlugin) {
            System.err.println(Messages.load(this).get("useCriteriaContextPlugin"));
            return false;
        }

        modifyCriterion(criterion, topLevelClass, introspectedTable);
        modifyCriteria(criteria, topLevelClass, introspectedTable);

        return true;
    }

    private void modifyCriteria(InnerClass criteria, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        criteria.addField(field(PRIVATE, criteria.getType(), "_parent"));

        criteria.addMethod(method(
            PUBLIC, CRITERIA, "andOr", body(
                "Criteria c = _owner.createCriteriaInternal();",
                "c._parent = this;",
                "criteria.add(new Criterion(c));",
                "return c;"
        )));

        criteria.addMethod(method(PUBLIC, CRITERIA, "endOr", body("return _parent != null ? _parent : this;")));
    }

    private void modifyCriterion(InnerClass criterion, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        criterion.addField(field(PRIVATE, CRITERIA, "sub"));

        criterion.addMethod(constructor(
            PROTECTED, "Criterion", param(CRITERIA, "sub"), body(
                "super();",
                "this.sub = sub;"
        )));

        criterion.addMethod(method(
            PUBLIC, new FullyQualifiedJavaType("Criteria"), "getSubCriteria", body(
                "return sub;"
        )));

        criterion.addMethod(method(PUBLIC, BOOL, "isComplex", body("return sub != null;")));
    }

    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        traverse(element, new FindElements() {
            @Override
            public boolean process(XmlElement parent, Element self, int position) {
                XmlElement foreach = ancestor(0);
                XmlElement trim = ancestor(1);
                if (indexOf(trim, foreach) != indexOf(trim, "foreach")) return false;

                XmlElement choose = traverse(trim, new CopyXml());
                traverse(choose, new ReplaceText(new TextReplacer("criterion\\.", "citem\\."), new TextReplacer("criterion(?=\\.|$)", "citem")));
                traverse(choose, new ReplaceText(new TextReplacer("criteria\\.", "criterion\\.subCriteria\\.")));
                traverse(choose, new ReplaceText(new TextReplacer("^\\s*and(?=\\s|\\(|$)", "or"), null));

                addLater((XmlElement)self, 0,
                    e("when", a("test", "criterion.complex"),
                        "and", e("trim", a("prefix", "("), a("suffix", ")"), a("prefixOverrides", "or"),
                            new DocumentFragment(choose)
                        )
                    )
                );
                return true;
            }
        }.when(ELEMENT.and(ancestorsAndSelf("where", "foreach", "if", "trim", "foreach", "choose"))));
        return true;
    }

}
