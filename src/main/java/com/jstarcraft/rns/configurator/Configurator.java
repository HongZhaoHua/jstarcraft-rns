package com.jstarcraft.rns.configurator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.jstarcraft.core.resource.annotation.ResourceConfiguration;
import com.jstarcraft.core.resource.annotation.ResourceId;

/**
 * 装配器
 * 
 * @author Birdy
 *
 * @param <T>
 */
@ResourceConfiguration
public abstract class Configurator<T> {

    @ResourceId
    private String id;

    protected Class<?> clazz;

    public Configurator() {
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
