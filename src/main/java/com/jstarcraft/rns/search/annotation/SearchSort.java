package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jstarcraft.rns.search.converter.SortConverter;

/**
 * 搜索排序
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface SearchSort {

    /** 排序转换器 */
    Class<? extends SortConverter> clazz() default SortConverter.class;
    
}
