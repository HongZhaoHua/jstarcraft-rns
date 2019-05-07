package com.jstarcraft.rns.configurator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.jstarcraft.core.storage.annotation.StorageConfiguration;
import com.jstarcraft.core.storage.annotation.StorageId;

/**
 * 装配器
 * 
 * @author Birdy
 *
 * @param <T>
 */
@StorageConfiguration
public abstract class Configurator<T> {

    @StorageId
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
