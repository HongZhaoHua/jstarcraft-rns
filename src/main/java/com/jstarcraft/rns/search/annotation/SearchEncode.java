package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jstarcraft.rns.search.converter.SearchConverter;

/**
 * 搜索保存
 * 
 * <pre>
 * 对于分词数据,排序没有意义
 * 对于不分词数据,词频/位置/偏移没有意义
 * </pre>
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface SearchEncode {

    /** 是否索引 */
    boolean index();

    /** 是否排序 */
    boolean sort();

    /** 是否存储 */
    boolean store();

    /** 是否分词(是否作用取决于{@link SearchConverter}) */
    boolean analyze() default false;

    /** 反向词向量 */
    SearchTerm negative() default @SearchTerm;

    /** 正向词向量 */
    SearchTerm positive() default @SearchTerm;

}
