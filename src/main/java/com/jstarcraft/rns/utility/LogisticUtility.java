package com.jstarcraft.rns.utility;

/**
 * 似然工具
 * 
 * @author Birdy
 *
 */
public class LogisticUtility {

    /**
     * 获取似然值
     * 
     * <pre>
     * logistic(x) + logistic(-x) = 1
     * </pre>
     * 
     * @param value
     * @return
     */
    public static float getValue(float value) {
        return (float) (1F / (1F + Math.exp(-value)));
    }

    /**
     * 获取似然梯度
     * 
     * @param value
     * @return
     */
    public static float getGradient(float value) {
        return (float) (getValue(value) * getValue(-value));
    }

}
