package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 词向量
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchTerm {

    /** 词频 */
    boolean frequency();

    /** 位置 */
    boolean position();

    /** 偏移 */
    boolean offset();

}
