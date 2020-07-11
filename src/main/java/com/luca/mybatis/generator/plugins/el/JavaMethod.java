package com.luca.mybatis.generator.plugins.el;

import org.mybatis.generator.api.dom.java.Method;

/**
 * @author Valdimir Lokhov
 */
public class JavaMethod extends Method {

    public JavaMethod javadoc(String ... lines) {
        addJavaDocLine("/**");
        for (String line : lines) if (line != null) addJavaDocLine(" * "+line);
        addJavaDocLine(" */");
        return this;
    }

}
