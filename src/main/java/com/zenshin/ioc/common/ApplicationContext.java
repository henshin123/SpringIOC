package com.zenshin.ioc.common;


import com.zenshin.ioc.annotation.*;
import com.zenshin.ioc.tool.ConfigurationUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.zenshin.ioc.tool.ConfigurationUtils.getPropertiesByKey;

public class ApplicationContext {

    public ApplicationContext() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        init();
        // 初始化数据
        this.classLoader();
    }
    private void init()
    {
        classSet = new HashSet<>();
        iocBeanMap = new ConcurrentHashMap(32);
    }
    /**
     * 类集合--存放所有的全限制类名
     */
    private Set<String> classSet ;
    /**
     * IOC容器
     */
    private ConcurrentHashMap iocBeanMap;

    /**
     * 从容器中获取数据
     * @param beanName
     * @return
     */
    public Object getIocBean(String beanName) {
        if (iocBeanMap != null) {
            return iocBeanMap.get(toLowercaseIndex(beanName));
        } else {
            return null;
        }
    }
    //类加载器
    private void classLoader() throws ClassNotFoundException,IllegalAccessException,InstantiationException
    {
        // 加载配置文件所有配置信息
        new ConfigurationUtils(null);
        // 获取扫描包路径
        String classScanPath = (String) ConfigurationUtils.properties.get("ioc.scan.path");
        if (StringUtils.isNotEmpty(classScanPath)) {//如果获取到的不是空的，那么就分隔开
            classScanPath = classScanPath.replace(".", "/");
        } else {
            throw new RuntimeException("请配置项目包扫描路径 ioc.scan.path");
        }
        // 扫描项目根目录中所有的class文件
        getPackageClassFile(classScanPath);
        for (String className : classSet) {
            addServiceToIoc(Class.forName(className));
        }
        // 获取带有Service注解类的所有的带Autowired注解的属性并对其进行实例化
        Set<String> beanKeySet = iocBeanMap.keySet();
        for (String beanName : beanKeySet) {
            addAutowiredToField(iocBeanMap.get(beanName));//这里是为了
        }
    }

    /**
     * 依赖注入
     */
    public void addAutowiredToField(Object obj) throws IllegalAccessException,InstantiationException
    {
        Field[] fields = obj.getClass().getDeclaredFields();//获取所有的字段
        for (Field field : fields) {
            if (field.getAnnotation(Autowired.class) != null) {
                field.setAccessible(true);//private修饰的字段无法直接设置，必须先设置field.setAccessible(true) 才能设置。
                Autowired autowired = field.getAnnotation(Autowired.class);//获取到字段上的注解
                Class<?> fieldClass = field.getType();//获取到属性是一个啥样的值，也就是要注入的类
                // 接口不能被实例化，需要对接口进行特殊处理获取其子类，获取所有实现类
                if(fieldClass.isInterface())
                {
                    //如果有接口指定的类名，那么就根据类名进行获取
                    if(StringUtils.isNotEmpty(autowired.value()))
                    {
                        field.set(obj,iocBeanMap.get(autowired.value()));
                    }
                    else
                    {
                        // 当注入接口时，属性的名字与接口实现类名一致则直接从容器中获取
                        Object objByName = iocBeanMap.get(field.getName());
                        if (objByName != null) {
                            field.set(obj, objByName);
                            // 递归依赖注入
                            addAutowiredToField(field.getType());
                        }
                        else //这种时候就是接口名称与容器中的类不一致的时候，也就是接口与实现类不一致的时候
                        {
                            List<Object> list = findSuperInterfaceByIoc(field.getType());//找到该接口所有的实现类
                            if (list != null && list.size() > 0) {
                                if (list.size() > 1) {//实现类在容器中有很多的话，就直接报错，必须指定注入类
                                    throw new RuntimeException(obj.getClass() + "  注入接口 " + field.getType() + "   失败，请在注解中指定需要注入的具体实现类");
                                }
                                else
                                {
                                    field.set(obj, list.get(0));//如果只有一个那就直接注入就可以了
                                    // 递归依赖注入
                                    addAutowiredToField(field.getType());
                                }
                            }
                            else {//接口没有相应的实现类
                                throw new RuntimeException("当前类" + obj.getClass() + "  不能注入接口 " + field.getType().getClass() + "  ， 接口没有实现类不能被实例化");
                            }
                        }
                    }
                }
                else //当属性不是接口的时候，直接注入即可
                {
                    String beanName = StringUtils.isEmpty(autowired.value()) ? toLowercaseIndex(field.getName()) : toLowercaseIndex(autowired.value());//获取到beanname
                    Object beanObj = iocBeanMap.get(beanName);//从容器中找是否有相应的bean
                    field.set(obj, beanObj == null ? field.getType().newInstance() : beanObj);//如果有bean。那就获取到，如果没有bean就实例化
                    System.out.println("依赖注入" + field.getName());
                }
                addAutowiredToField(field.getType());
            }
            if(field.getAnnotation(Value.class) != null)//获取配置进行注入
            {
                field.setAccessible(true);
                Value value = field.getAnnotation(Value.class);
                field.set(obj, StringUtils.isNotEmpty(value.value()) ? getPropertiesByKey(value.value()) : null);
                System.out.println("注入配置文件  " + obj.getClass() + " 加载配置属性" + value.value());
            }
        }
    }

    /**
     * 控制反转，将标了注解的类加到容器中。
     */
    private void  addServiceToIoc(Class classZ) throws IllegalAccessException,InstantiationException
    {
        if(classZ.getAnnotation(Controller.class) != null)
        {
            iocBeanMap.put(toLowercaseIndex(classZ.getSimpleName()), classZ.newInstance());
            System.out.println("控制反转访问控制层:" + toLowercaseIndex(classZ.getSimpleName()));
        }
        else if(classZ.getAnnotation(Service.class) != null)
        {
            // 将当前类交由IOC管理
            Service service = (Service) classZ.getAnnotation(Service.class);
            iocBeanMap.put(StringUtils.isEmpty(service.value()) ? toLowercaseIndex(classZ.getSimpleName()) : toLowercaseIndex(service.value()), classZ.newInstance());
            System.out.println("控制反转服务层:" + toLowercaseIndex(classZ.getSimpleName()));
        }
        else if (classZ.getAnnotation(Mapping.class) != null) {
            Mapping myMapping = (Mapping) classZ.getAnnotation(Mapping.class);
            iocBeanMap.put(StringUtils.isEmpty(myMapping.value()) ? toLowercaseIndex(classZ.getSimpleName()) : toLowercaseIndex(myMapping.value()), classZ.newInstance());
            System.out.println("控制反转持久层:" + toLowercaseIndex(classZ.getSimpleName()));
        }
    }

    /**
     * 扫描项目根目录中所有的class文件
     * @param packageName : 包路径
     */
    private void getPackageClassFile(String packageName) {
        URL url = this.getClass().getClassLoader().getResource(packageName);
        File file = new File(url.getFile());
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileSon : files) {
                if (fileSon.isDirectory()) {
                    // 递归扫描
                    getPackageClassFile(packageName + "/" + fileSon.getName());
                } else {
                    // 是文件并且是以 .class结尾
                    if (fileSon.getName().endsWith(".class")) {
                        System.out.println("正在加载: " + packageName.replace("/", ".") + "." + fileSon.getName());
                        classSet.add(packageName.replace("/", ".") + "." + fileSon.getName().replace(".class", ""));
                    }
                }
            }
        } else {
            throw new RuntimeException("没有找到需要扫描的文件目录");
        }
    }

    /**
     * 判断需要注入的接口所有的实现类
     */
    private List<Object> findSuperInterfaceByIoc(Class classz) {
        Set<String> beanNameList = iocBeanMap.keySet();
        ArrayList<Object> objectArrayList = new ArrayList<>();
        for (String beanName : beanNameList) {
            Object obj = iocBeanMap.get(beanName);
            Class<?>[] interfaces = obj.getClass().getInterfaces();
            if (ArrayUtils.contains(interfaces, classz)) {
                objectArrayList.add(obj);
            }
        }
        return objectArrayList;
    }

    /**
     * 类首名字转小写
     * @param name
     * @return
     */
    private static String toLowercaseIndex(String name)
    {
        if (StringUtils.isNotEmpty(name)) {
            return name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        return name;
    }
    /**
     * 类名首字母转大写
     */
    public static String toUpperCaseIndex(String name) {
        if (StringUtils.isNotEmpty(name)) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }
}
