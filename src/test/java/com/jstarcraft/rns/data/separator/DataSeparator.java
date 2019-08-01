package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.module.ReferenceModule;

/**
 * 数据处理器
 * 
 * @author Birdy
 *
 */
// TODO 负责将行为模型调整为训练模型+标记,测试模型+标记.
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
