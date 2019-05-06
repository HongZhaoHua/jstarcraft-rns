package com.jstarcraft.recommendation.configure;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 装配器
 * 
 * @author Birdy
 *
 * @param <T>
 */
public abstract class Configurer<T> {

    protected Class<?> clazz;

    public Configurer() {
        this.clazz = this.getClass();
        ParameterizedType type = (ParameterizedType) this.clazz.getGenericSuperclass();
        Type[] types = type.getActualTypeArguments();
        this.clazz = (Class) types[0];
    }

    /**
     * 配置
     * 
     * @return
     */
    public abstract T configure();

}
