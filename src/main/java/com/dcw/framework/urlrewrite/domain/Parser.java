package com.dcw.framework.urlrewrite.domain;

/**
 * create by adao12.vip@gmail.com on 15/12/31
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public interface Parser<T> {

    void parse(T value);
}
