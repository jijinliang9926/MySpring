package com.kyrie.spring;

/**
 * Aware回调
 */
public interface BeanNameAware {
    void setBeanName(String beanName);
}
