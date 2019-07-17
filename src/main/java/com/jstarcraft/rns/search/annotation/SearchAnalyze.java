package com.jstarcraft.rns.search.annotation;

import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.lucene.analysis.TokenStream;

/**
 * 分词
 * 
 * <pre>
 *  仅作用于{@link Reader},{@link String},{@link TokenStream}类型字段的注解.
 * </pre>
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchAnalyze {

    /** 反向词向量(取决于是否需要查询) */
    SearchTerm negative() default @SearchTerm;

    /** 正向词向量(取决于是否需要高亮) */
    SearchTerm positive() default @SearchTerm;

}
