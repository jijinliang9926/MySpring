package com.kyrie.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 相当于是Spring容器
 */
public class KyrieApplicationContext {
    private Class configClass; //创建容器的配置类
    //存放BeanDefinition的Map集合
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionConcurrentHashMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();//单例池，存放单例Bean的

    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public KyrieApplicationContext(Class configClass) {
        this.configClass = configClass;
            //容器启动第一件事，先扫描Bean并创建添加到Map集合
            if (configClass.isAnnotationPresent(ComponentScan.class)) {//判断Config类如果有ComponentScan注解
                ComponentScan annotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);//用类获取注解对象
                String path = annotation.value();//获取到注解的值
                path = path.replace(".", "/");//把.替换成/
                ClassLoader classLoader = KyrieApplicationContext.class.getClassLoader();  //用类获取得到类加载器
                URL resource = classLoader.getResource(path);   //相当于从类加载器中获取path路径对应的资源
            String filePath = resource.getFile();   //得到目录路径
            File file = new File(filePath);   //File既可以是文件，也可以是目录
            if (file.isDirectory()) {  //判断file是不是目录
                File[] files = file.listFiles();//得到目录的所有文件
                for (File f : files) { //循环每个文件
                    String absolutePath = f.getAbsolutePath();//得到每个文件的绝对路径
                    if (absolutePath.endsWith(".class")) {//判断每个绝对路径的结尾是不是.class结尾
                        //截取后面的类的全限定名，但是\分割
                        String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                        className = className.replace("\\", ".");//再把\转换成.变成全限定名
                        try {
                            Class<?> clazz = classLoader.loadClass(className);//把类的全限定名传入方法里面，获得class对象。方法需要try catch
                            if (clazz.isAnnotationPresent(Component.class)) {//如果类上面存在Component注解，那么就是Bean

                                /*
                                 Bean初始化前后执行自定义代码
                                 写一个BeanPostProcessor接口，让Bean去继承
                                 Spring启动的时候就会检查加@Component注解的Bean里面有没有继承BeanPostProcessor
                                 如果有，就把Bean添加到beanPostProcessorList集合中
                                 调用createBean方法的时候会查看beanPostProcessorList集合的每一个类
                                 顺序执行   Bean初始化前的自定义代码
                                           Bean初始化操作
                                           Bean初始化后的自定义代码
                                 */

                                //先看这个类是不是实现了BeanPostProcessor接口
                                //意思就是clazz类是不是由BeanPostProcessor类派生的，也就是说判断是不是继承实现了BeanPostProcessor
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    BeanPostProcessor instance = (BeanPostProcessor) clazz.newInstance();//如果继承了，那就得到这个对象
                                    beanPostProcessorList.add(instance);//添加到集合中
                                }

                                /*
                                创建Bean的时候要考虑是单例Bean还是多例Bean
                                    单例就从容器中找，多例就创建
                                创建一个@Scope注解”prototype"
                                创建一个BeanDefinition类，里面定义的Bean的类型、范围、是懒加载还是延迟加载等
                                 */

                                BeanDefinition beanDefinition = new BeanDefinition();  //生成一个BeanDefinition对象
                                beanDefinition.setType(clazz);  //给对象赋值

                                if (clazz.isAnnotationPresent(Scope.class)) {   //如果这个Bean有@Scope注解
                                    Scope scope = clazz.getAnnotation(Scope.class); //用类获取注解对象
                                    beanDefinition.setScope(scope.value()); //如果有Scope注解就把注解值赋值到scope中
                                } else {//没有注解就是单例
                                    beanDefinition.setScope("singleton");
                                }
                                    Component component = clazz.getAnnotation(Component.class);//获取注解对象，准备生成Bean名字
                                    String beanName = component.value();//获取注解的value值
                                if (component.value().equals("")) {//如果没写值，就是默认的“”空，那么就生成由java规则生成的名字
                                    /*
                                    不写bean名称，那么这个方法就给你默认的bean名称。怎么给呢？
                                    如果你的类名只有首字母大写，bean名称就是类名首字母小写的；如果类名第一第二个字母都大写了
                                    bean名称就是类名，如果你的类首字母小写了，bean名称也是类名
                                     */
                                    beanName = Introspector.decapitalize(clazz.getSimpleName());
                                }
                                    beanDefinitionConcurrentHashMap.put(beanName, beanDefinition);//把对象存入Map集合
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        //前面是扫描，下面是根据扫描除来的Map集合去创建单例Bean
        for (String beanName : beanDefinitionConcurrentHashMap.keySet()) {//循环Map中的每个名字
            BeanDefinition beanDefinition = beanDefinitionConcurrentHashMap.get(beanName);//通过名字得到BeanDefinition对象
            if (beanDefinition.getScope().equals("singleton")) {//如果这个beanDefinition的作用域是单例
                Object bean = createBean(beanName,beanDefinition);//调用创建方法来创建Bean
                singletonObjects.put(beanName, bean);//将创建好的Bean放入单例池Map中
            }
        }
    }

    private Object createBean(String beanName,BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();//获取类的类型
        try {
            System.out.println("正在根据beanDefinition对象创建实例Bean... Bean:"+beanName);
            Object instance = clazz.getConstructor().newInstance();//利用反射调用无参构造方法创建实例对象

            //依赖注入
            for (Field field : clazz.getDeclaredFields()) {//反射获取所有成员变量，循环每一个变量
                if (field.isAnnotationPresent(AutoWired.class)) {//如果这个变量上面由AutoWried注解
                    field.setAccessible(true);//暴力反射，打开私有权限可以赋值
                    field.set(instance,getBean(field.getName()));//给对象赋值，set(实例对象，赋的值)
                    System.out.println("已通过暴力反射为加@AutoWired注解的变量赋值:"+field.getName());
                }
            }

            //Aware回调。告诉给你哪个值，直接回调
            if (instance instanceof BeanNameAware) {//如果这个实例实现了BeanNameAware接口
                ((BeanNameAware) instance).setBeanName(beanName);//那么就把传入的beanName给实例赋值
            }

            //BeanPostProcessor 初始化前 AOP
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(beanName,instance);
            }

            /*
            初始化。负责调用当前Bean的初始化方法，具体这个方法做什么，Spring不管，可以让程序员干预 初始化，继承接口就行
            继承接口，并重写初始化方法，创建Bean的时候会被执行
             */
            if (instance instanceof InitializingBean) {//如果这个实例实现了InitializingBean接口
                //进行Bean初始化...可初始化bean的值
                ((InitializingBean) instance).afterPropertiesSet();//就调用初始化方法来初始化bean
            }

            //BeanPostProcessor（Bean的后置处理器） 初始化后 AOP
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(beanName,instance);
                System.out.println("-------------------------------------------");
            }

            return instance;//返回Bean
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

        public Object getBean(String beanName) {
            BeanDefinition beanDefinition = beanDefinitionConcurrentHashMap.get(beanName);//通过字符串获取BeanDefinition对象
            if (beanDefinition == null) {//如果获取到的是null，说明没有这个Bean或者名字不对
                throw new NullPointerException();//抛异常
            } else { //如果找到了
                String scope = beanDefinition.getScope();//检查作用域，判断单例还是多例
                if ("singleton".equals(scope)) {//如果是单例的
                    Object bean = singletonObjects.get(beanName);//从单例池中查找
                    if (bean == null) {//如果找到是空
                        Object bean1 = createBean(beanName, beanDefinition);//那么就调用创建Bean方法来创建Bean
                        singletonObjects.put(beanName,bean1);//并放入单例池Map中
                    }
                    return bean;//找到就返回这个单例Bean对象
                } else {//否则就是多例的，直接创建并返回Bean对象
                    Object bean = createBean(beanName, beanDefinition);
                    return bean;
                }
            }
        }
    }
