package com.jstarcraft.recommendation.data.processor;

import com.jstarcraft.recommendation.data.accessor.DataInstance;

/**
 * 数据选择器
 * 
 * @author Birdy
 *
 */
public interface DataSelector {

	boolean select(DataInstance instance);

}
