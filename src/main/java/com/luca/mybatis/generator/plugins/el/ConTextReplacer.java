package com.luca.mybatis.generator.plugins.el;

import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.Stack;

/**
 * @author Vladimir Lokhov
 */
public class ConTextReplacer extends TextReplacer<Stack<XmlElement>> {
    private Predicate<Stack<XmlElement>> condition;
    public ConTextReplacer(String pattern, String to, Predicate<Stack<XmlElement>> condition) {
        super(pattern, to);
        this.condition = condition;
    }

    @Override
    public Pair<String, Boolean> replace(Stack<XmlElement> parents, String value) {
        if (!condition.test(parents))
            return UNCHANGED;
        return super.replace(parents, value);
    }
}
