## 概念
### IOC(Inversion of Control)
IOC-控制反转，这不是什么技术，这是一种设计思想，在Spring框架中，我们所有要使用到的类都交由Spring去创建，我们想要使用的时候只需要注入就可以了。要理解IOC的关键就是明白"谁控制了谁，控制什么，为何反转(有反转就应该有正转)，在那些方面反转了",下面我们来看一下：
* 谁控制了谁，控制了什么：
在传统的开发中我们直接在对象内部通过new进行创建对象，是程序主动去创建依赖的对象；而IOC是由专门一个容器来创建这些对象，即由IOC容器来控制对象的创建而不在显式地使用new；
**谁控制谁**： IOC控制了对象，**控制了什么**：主要控制了外部资源获取和声明周期(不只是对象也包括文件等)
* 为何是反转，哪些方面反转了
有反转就有正转，传统应用中是由我们自己在对象中主动控制去直接获取依赖对象，也就是正转；而反转则是由容器来帮忙创建以及注入依赖的对象；
为何是反转？因为由容器帮我们查找及注入依赖对象，对象只是被动的接受依赖对象，所以是反转了
哪些方面反转了：依赖对象的获取被反转了
* 传统方式：
<img src="https://picture.zlh.giserhub.com/%E4%BC%A0%E7%BB%9F.png">

* 当有了IOC的容器后，在客户端类中不再主动去创建这些对象了
<img src="https://picture.zlh.giserhub.com/IOC%E6%96%B9%E5%BC%8F.png">

### IOC能做什么
IOC不是一种技术，只是一种思想，一个重要的面向对象编程的法则，它能指导我们如何设计出松耦合、更优良的程序。传统应用程序都是由我们在类内部主动创建依赖对象，从而导致类与类之间高耦合，难于测试；有了IOC容器后，把创建和查找依赖对象的控制权交给了容器，由容器进行注入组合对象，所以对象与对象之间是松散耦合，这样也方便测试，利于功能复用，更重要的是使得程序的整个体系结构变得非常灵活。
其实IOC对编程带来的最大改变不是从代码上，而是从思想上，发生了“主从换位”的变化。应用程序原本是老大，要获取什么资源都是主动出击，但是在IOC/DI思想中，应用程序就变成被动的了，被动的等待IOC容器来创建并注入它所需要的资源了。

### IOC和DI
DI-Dependency Injection，即“依赖注入”：是组件之间依赖关系由容器在运行期决定，形象的说，即由容器动态的将某个依赖关系注入到组件之中。依赖注入的目的并非为软件系统带来更多功能，而是为了提升组件重用的频率，并为系统搭建一个灵活、可扩展的平台。通过依赖注入机制，我们只需要通过简单的配置，而无需任何代码就可指定目标需要的资源，完成自身的业务逻辑，而不需要关心具体的资源来自何处，由谁实现。
理解DI的关键是：“谁依赖谁，为什么需要依赖，谁注入谁，注入了什么”，那我们来深入分析一下：
* 谁依赖于谁：应用程序依赖于IOC容器；
* 为什么需要依赖：应用程序需要IOC容器来提供对象需要的外部资源；
* 谁注入谁：IOC容器注入应用程序某个对象，应用程序依赖的对象；
* 注入了什么：就是注入某个对象所需要的外部资源（包括对象、资源、常量数据）。

## 手写实现IOC容器
根据[手写IOC容器](https://zhuanlan.zhihu.com/p/60229771)实现
这个IOC容器是基于注解来实现的。
我们在手写IOC容器的时候需要掌握一些java的基础的知识点，分别是：注解、反射、IO流
我们看一下OIOC容器的整体流程：
* 配置文件配置包扫描路径
* 递归包扫描回去.class文件
* 反射确定需要较给IOC管理的类
* 对需要注入的类进行依赖注入

### 创建项目
首先，我们先创建一个Maven项目，然后在项目的resources目录下添加一个配置文件application.properties，在配置文件中指定需要扫描的包路径
```properties
# 扫描包路径
ioc.scan.path=com.zenshin.ioc
```
这个配置主要为了后面我们在获取包内所有类的时候找路径时需要的。

### 创建注解类
在日常开发中我们一般有三层，数据层，服务层，控制层这三层，就基于这三层我们定义几个注解,首先我们在根目录创建一个`annotation`包
#### @Controller
`Controller`层是一个顶层，一般不需要加什么属性，并且是作用在类上的
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Controller {

}
```
#### @Service
Service层注解是为了注入到Controller层的，所以需要一个属性，明确告知一下注入的是哪个类
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Service {
    String value() default "";
}
```
#### @Mapping
Mappingc层就是数据操作层，该层是注入到Service的，所以定义和Service注解是一样的
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Mapping {
    String value() default "";
}
```
#### @Autowired
注入注解，我们前面的注解都是在类上面标注的，目的是在容器启动的时候可以被容器扫描到，添加到容器中，但是还需要一个注入注解来将容器中如何要求的类注入进去。
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Autowired {
    String value() default "";
}
```
#### @Value
Value注解主要是为了获取配置文件中的内容
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Value {
    String value() default "";
}
```
那么我们的注解就完成了，注解通过反射获取到以后来进行判断注入。

### 创建一个配置文件帮助类
这个类主要是为了读取配置文件以及读取配置文件中的节点
```java
public class ConfigurationUtils {
    /**
     * 项目配置文件信息
     */
    public static Properties properties;

    public ConfigurationUtils(String propertiesPath) {
        properties = this.getBeanScanPath(propertiesPath);
    }
    /**
     读取配置文件
     */
    private Properties getBeanScanPath(String propertiesPath) {
        if (StringUtils.isEmpty(propertiesPath)) {
            propertiesPath = "/application.properties";
        }
        Properties properties = new Properties();
        // 通过类的加载器获取具有给定名称的资源
        InputStream in = ConfigurationUtils.class.getResourceAsStream(propertiesPath);
        try {
            System.out.println("正在加载配置文件application.properties");
            properties.load(in);
            return properties;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    /**
     根据配置文件的key获取value的值
     */
    public static Object getPropertiesByKey(String propertiesKey) {
        if (properties.size() > 0) {
            return properties.get(propertiesKey);
        }
        return null;
    }

}
```


### 重头戏--创建IOC容器类
我们的思路是这样的，首先我们需要将在配置文件中的路径下的所有class文件扫描进一个HashSet中，然后扫描所有的类，如果类中有Autowired注解的属性，通过反射注入，我们的IOC容器就实现了，IOC用ConcurrentHashMap实现

#### 扫描所有的class文件
```java
/**
 * 类集合--存放所有的全限制类名
*/
private Set<String> classSet ;
//扫描class文件
private void getPackageClassFile(String packageName) {
    //获取到全路径，packageName是通过读取配置文件获取的
    URL url = this.getClass().getClassLoader().getResource(packageName);
    //获取到整个文件夹
    File file = new File(url.getFile());
    //如果文件存在，并且是文件夹就遍历这里面的类文件
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
                    //将.class文件用字符串的 com.zenshin.ioc.annotation.Autowired
                    classSet.add(packageName.replace("/", ".") + "." + fileSon.getName().replace(".class", ""));
                }
            }
        }
    } else {
        throw new RuntimeException("没有找到需要扫描的文件目录");
    }
}
```
#### 扫描后实例化类，塞进IOC容器
```java
 /**
* IOC容器
*/
private ConcurrentHashMap iocBeanMap;

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
```
#### 将需要注入的类，注入到类中
```java
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
```

#### 初始化容器时，对三个方法进行加载
```java
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
        addAutowiredToField(iocBeanMap.get(beanName));
    }
}
```

#### IOC容器全部的代码
```java
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
```

## 测试IOC容器
我们模拟实际开发中三层模型，接口注入
### 创建dao层
编写一个接口类
```java
public interface LoginMapping {
    String login();
}
```
编写一个实现类
```java
@Mapping
public class LoginMappingImp implements LoginMapping {
    @Override
    public String login() {
        return "项目启动成功";
    }
}
```
### 创建service层
编写一个service接口
```java
public interface ILoginService {
    String login();
}
```
编写实现接口的业务类
```java
@Service
public class LoginService implements ILoginService {

    @Autowired
    private LoginMapping loginMapping; //注入dao层

    @Override
    public String login() {
        return loginMapping.login();
    }
}
```
### 创建controller层
```java
@Controller
public class LoginController {
    @Value(value = "ioc.scan.pathTest")
    private String pathtest; //配置文件注入

    @Autowired
    private ILoginService loginService;//service注入

    public String login() {
        return loginService.login();
    }
}
```
### 编写一个主入口
```java
public class PlatformApplication {
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ApplicationContext();//初始化容器
        LoginController loginController = (LoginController) applicationContext.getIocBean("LoginController");//从容器中取出
        String login = loginController.login();
        System.out.println(login);
    }
}
```
### 运行结果
```java
正在加载配置文件application.properties
正在加载: com.zenshin.ioc.annotation.Autowired.class
正在加载: com.zenshin.ioc.annotation.Controller.class
正在加载: com.zenshin.ioc.annotation.Mapping.class
正在加载: com.zenshin.ioc.annotation.Service.class
正在加载: com.zenshin.ioc.annotation.Value.class
正在加载: com.zenshin.ioc.common.ApplicationContext.class
正在加载: com.zenshin.ioc.modular.controller.LoginController.class
正在加载: com.zenshin.ioc.modular.dao.imp.LoginMappingImp.class
正在加载: com.zenshin.ioc.modular.dao.LoginMapping.class
正在加载: com.zenshin.ioc.modular.service.ILoginService.class
正在加载: com.zenshin.ioc.modular.service.imp.LoginService.class
正在加载: com.zenshin.ioc.PlatformApplication.class
正在加载: com.zenshin.ioc.tool.ConfigurationUtils.class
控制反转持久层:loginMappingImp
控制反转服务层:loginService
控制反转访问控制层:loginController
注入配置文件  class com.zenshin.ioc.modular.controller.LoginController 加载配置属性ioc.scan.pathTest
项目启动成功
```

至此我们整个IOC容器算是实现了。

## 小结
* IOC容器主要是为了类之间解耦，类之间在通过接口隔离，运行时通过IOC容器主动注入，将类与类之间的耦合大大降低
* IOC容器其实就是一个集合，里面装了整个包的类，需要的时候注入进来
* 全部的工程项目在我的github上面[](https://github.com/henshin123/SpringIOC/tree/master)
