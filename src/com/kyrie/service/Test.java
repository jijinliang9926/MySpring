package com.kyrie.service;

import com.kyrie.spring.KyrieApplicationContext;


public class Test {
    public static void main(String[] args) {
        KyrieApplicationContext kyrieApplicationContext = new KyrieApplicationContext(Config.class);

        UserInterface userService = (UserInterface) kyrieApplicationContext.getBean("userService");
        userService.test();
    }
}
