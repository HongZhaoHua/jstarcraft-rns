package com.jstarcraft.rns.model;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;

/**
 * 可更新
 * 
 * @author Birdy
 *
 */
public interface Updateable {

    void update(DataInstance instance);

    default void update(DataModule module) {
        for (DataInstance instance : module) {
            update(instance);
        }
    }

}
