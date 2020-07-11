package com.luca.mybatis.generator.plugins.el;

import java.util.List;

/**
 * @author Vladimir Lokhov
 */
public interface Tokenizer {
    List<String> tokenize(String expression);

    String unescape(String text);

    String escape(String text);
}
