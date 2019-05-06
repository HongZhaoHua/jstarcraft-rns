package com.jstarcraft.recommendation.data.processor;

import com.jstarcraft.ai.data.DataInstance;

/**
 * 数据选择器
 * 
 * @author Birdy
 *
 */
public interface DataSelector {

	boolean select(DataInstance instance);

}
