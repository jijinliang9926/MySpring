package com.kyrie.service;

import com.kyrie.spring.*;

/**
 * 继承了BeanNameAware
 * 继承了InitializingBean可以对当前类生成的Bean进行初始化操作。初始化操作再重写的方法里实现
 * 继承了IUserInterface为了生成代理对象，做AOP测试
 *
 */
@Component
@Scope
public class UserService implements BeanNameAware, InitializingBean,UserInterface{
    @AutoWired
    private OrderService orderService;
    private String beanName;

    @Override
    public void test(){
        System.out.println(orderService);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("初始化Bean...");
    }
}
