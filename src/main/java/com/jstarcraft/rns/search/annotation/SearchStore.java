package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jstarcraft.rns.search.store.StoreConverter;

/**
 * 搜索存储
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface SearchStore {

    /** 存储转换器 */
    Class<? extends StoreConverter> clazz() default StoreConverter.class;

}
