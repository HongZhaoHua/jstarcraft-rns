package com.jstarcraft.rns.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 搜索保存
 * 
 * @author Birdy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface SearchEncode {

    SearchAnalyze analyze() default @SearchAnalyze;

    boolean index();

    boolean sort();

    boolean store();

}
