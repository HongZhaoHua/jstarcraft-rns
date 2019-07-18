package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jstarcraft.rns.search.index.IndexConverter;

/**
 * 搜索索引
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface SearchIndex {

    /** 是否分词 */
    boolean analyze() default false;

    /** 反向词向量 */
    SearchTerm negative() default @SearchTerm;

    /** 正向词向量 */
    SearchTerm positive() default @SearchTerm;

    /** 索引转换器 */
    Class<? extends IndexConverter> clazz() default IndexConverter.class;

}
