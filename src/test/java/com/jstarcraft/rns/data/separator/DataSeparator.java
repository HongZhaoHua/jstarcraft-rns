package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.module.ReferenceModule;

/**
 * 数据分割器
 * 
 * <pre>
 * 数据模块分割为训练模块与测试模块.
 * </pre>
 * 
 * @author Birdy
 *
 */
public interface DataSeparator {

    /**
     * 获取分割数量
     * 
     * @return
     */
    int getSize();

    /**
     * 获取训练引用
     * 
     * @param index
     * @return
     */
    ReferenceModule getTrainReference(int index);

    /**
     * 获取测试引用
     * 
     * @param index
     * @return
     */
    ReferenceModule getTestReference(int index);

}
