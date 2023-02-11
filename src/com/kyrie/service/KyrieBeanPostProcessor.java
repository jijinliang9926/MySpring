package com.kyrie.service;

import com.kyrie.spring.BeanPostProcessor;
import com.kyrie.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 继承了BeanPostProcessor接口，可以对Bean方法执行切面
 * 创建Bean的时候可对Bean操作
 * 下面是如果Bean的名字是userService就返回代理对象
 */
@Component
public class KyrieBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        if (bean.getClass().equals(UserService.class)) {
            System.out.println(beanName + " AOP初始化前...");
        } else {
            System.out.println(beanName + " AOP初始化前未作操作");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (beanName.equals("userService")) {
            System.out.println(beanName + "初始化后...生成代理对象");
            Object instance = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object proxyBean = method.invoke(bean, args);
                    return proxyBean;
                }
            });
            System.out.println(beanName + " AOP初始化后完成");
            return instance;
        } else {
            System.out.println(beanName + " AOP初始化后未作操作");
        }
        return bean;
    }
}
