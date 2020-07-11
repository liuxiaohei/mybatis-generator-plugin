package com.luca.mybatis.generator.plugins.el;

/**
 * Backport of java.util.function.BiPredicate
 * @author Vladimir Lokhov
 */
public interface BiPredicate<T,U> {

    public BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> other);

    public BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> other);

    public BiPredicate<T, U> negate();

    public boolean test(T t, U u);
}