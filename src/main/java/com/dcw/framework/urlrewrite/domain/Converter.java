package com.dcw.framework.urlrewrite.domain;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public interface Converter<F, T> {

    T convert(F value);

    abstract class Factory {

        public Converter<String, ?> from(String url) {
            return null;
        }

        public Converter<?, String> to(Rule value) {
            return null;
        }
    }
}
