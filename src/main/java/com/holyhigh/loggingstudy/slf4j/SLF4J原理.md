- 对以下代码进行Debug；

```java
public class Slf4jTest {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Slf4jTest.class);
        logger.info("Hello SLF4J!");
    }
}
```

- 通过`LoggerFactory.getLogger()`返回具体的日志实现对象，具体实现如下；

```java
public static Logger getLogger(Class<?> clazz) {
    Logger logger = getLogger(clazz.getName());
    // 检测不匹配的记录器名称，通过-D参数开启
    if (DETECT_LOGGER_NAME_MISMATCH) {
        Class<?> autoComputedCallingClass = Util.getCallingClass();
        if (autoComputedCallingClass != null && nonMatchingClasses(clazz, autoComputedCallingClass)) {
            Util.report(String.format("Detected logger name mismatch. Given name: \"%s\"; computed name: \"%s\".", logger.getName(),
                            autoComputedCallingClass.getName()));
            Util.report("See " + LOGGER_NAME_MISMATCH_URL + " for an explanation");
        }
    }
    return logger;
}
```

- 其中`DETECT_LOGGER_NAME_MISMATCH`用于检测不匹配的记录器名称，如果不配置就不会检测；配置方式是增加虚拟机参数`VM options`。

```java
// Support for detecting mismatched logger names.
// 开启方式：-Dslf4j.detectLoggerNameMismatch=true，since 1.7.9
static final String DETECT_LOGGER_NAME_MISMATCH_PROPERTY = "slf4j.detectLoggerNameMismatch";
static boolean DETECT_LOGGER_NAME_MISMATCH = Util.safeGetBooleanSystemProperty(DETECT_LOGGER_NAME_MISMATCH_PROPERTY);
```

- 检测的原理是根据上下文自动计算调用类的全限定名，也就是调用`LoggerFactory.getLogger()`方法的类，和传入的`clazz`所对应的全限定名进行对比，不一致会通过控制台打印错误信息。也就是说如果写法如下，且在开启检测的情况下会报错；

```java
public class Slf4jTest {
    public static void main(String[] args) {
        // 此处传入String.class而非Slf4jTest.class
        Logger logger = LoggerFactory.getLogger(String.class);
        logger.info("Hello SLF4J!");
    }
}
```

<img src="/Users/holyhigh/Library/Application Support/typora-user-images/image-20220109005044684.png" alt="image-20220109005044684" style="zoom:50%;" />

```
SLF4J: Detected logger name mismatch. Given name: "java.lang.String"; computed name: "com.holyhigh.loggingstudy.slf4j.Slf4jTest".
SLF4J: See http://www.slf4j.org/codes.html#loggerNameMismatch for an explanation
[main] INFO java.lang.String - Hello SLF4J!
```

- 回到代码，接着来看`Logger logger = getLogger(clazz.getName());`这行代码，其返回的对象就是具体的实现；

```java
public static Logger getLogger(String name) {
    ILoggerFactory iLoggerFactory = getILoggerFactory();
    return iLoggerFactory.getLogger(name);
}
```

- 继续看`getILoggerFactory()`方法，可以看到有很多日志初始化状态的判断，通过`performInitialization()`方法进行日志实现的绑定；

```java
public static ILoggerFactory getILoggerFactory() {
    // 如果日志初始化状态==未初始化=0
    if (INITIALIZATION_STATE == UNINITIALIZED) {
        // 双重检查锁，防止并发
        synchronized (LoggerFactory.class) {
            if (INITIALIZATION_STATE == UNINITIALIZED) {
                // 日志初始化状态=正在初始化=1
                INITIALIZATION_STATE = ONGOING_INITIALIZATION;
                // 核心方法：初始化日志实现，内部会决定最终的日志初始化状态
                performInitialization();
            }
        }
    }
    // 处理最终的日志初始化状态
    switch (INITIALIZATION_STATE) {
        // 成功初始3
        case SUCCESSFUL_INITIALIZATION:
            // 已经进行了具体的日志实现绑定
            return StaticLoggerBinder.getSingleton().getLoggerFactory();
        // no-opertion 不进行任何操作=4
        case NOP_FALLBACK_INITIALIZATION:
            return NOP_FALLBACK_FACTORY;
        // 初始化失败=2
        case FAILED_INITIALIZATION:
            throw new IllegalStateException(UNSUCCESSFUL_INIT_MSG);
        // 正在初始化中
        case ONGOING_INITIALIZATION:
            // support re-entrant behavior.
            // See also http://jira.qos.ch/browse/SLF4J-97
            return SUBST_FACTORY;
        }
    		throw new IllegalStateException("Unreachable code");
}
```

- `performInitialization()`方法如下：

```java
private final static void performInitialization() {
    // 绑定日志实现
    bind();
    // 成功初始化
    if (INITIALIZATION_STATE == SUCCESSFUL_INITIALIZATION) {
        // 版本健全性检查
        versionSanityCheck();
    }
}
```

- 继续查看`bind()`方法

```java
private final static void bind() {
    try {
        Set<URL> staticLoggerBinderPathSet = null;
        // skip check under android, see also
        // http://jira.qos.ch/browse/SLF4J-328
        if (!isAndroid()) {
            // 非安卓情况下进入，找到日志实现，最核心的方法
            staticLoggerBinderPathSet = findPossibleStaticLoggerBinderPathSet();
            // 如果检测到的日志实现有多个，则会打印所有检测到的实现，如果只有1个，则不打印
            reportMultipleBindingAmbiguity(staticLoggerBinderPathSet);
        }
        // the next line does the binding
        // 最终进行绑定的方法，如果有多个日志实现，由其决定一个进行绑定 @1
        StaticLoggerBinder.getSingleton();
        // 初始化成功
        INITIALIZATION_STATE = SUCCESSFUL_INITIALIZATION;
        // 报告实际绑定的日志实现
        // 检查实际的绑定数是否大于1，大于1时会打印实际绑定的日志实现，其真正的绑定由@1处完成
        reportActualBinding(staticLoggerBinderPathSet);
    } catch (NoClassDefFoundError ncde) {
        // doSomething
    } catch (java.lang.NoSuchMethodError nsme) {
        // doSomething
    } catch (Exception e) {
        // doSomething
    } finally {
        // 发布绑定后的清理工作
        postBindCleanUp();
    }
}
```

- 接着看`findPossibleStaticLoggerBinderPathSet()`方法

```java
static Set<URL> findPossibleStaticLoggerBinderPathSet() {
    // use Set instead of list in order to deal with bug #138
    // LinkedHashSet appropriate here because it preserves insertion order
    // during iteration
    // 使用LinkedHashSet在迭代时可以保留插入顺序
    Set<URL> staticLoggerBinderPathSet = new LinkedHashSet<URL>();
    try {
        // 获取类加载器
        ClassLoader loggerFactoryClassLoader = LoggerFactory.class.getClassLoader();
        Enumeration<URL> paths;
        if (loggerFactoryClassLoader == null) {
            paths = ClassLoader.getSystemResources(STATIC_LOGGER_BINDER_PATH);
        } else {
            // STATIC_LOGGER_BINDER_PATH = "org/slf4j/impl/StaticLoggerBinder.class";
            // 查找实现了对应静态日志绑定的类路径
            paths = loggerFactoryClassLoader.getResources(STATIC_LOGGER_BINDER_PATH);
        }
        // 此时paths.index=1
        while (paths.hasMoreElements()) {
            // 首次获取的nextElement是index=1的path
            URL path = paths.nextElement();
            staticLoggerBinderPathSet.add(path);
        }
    } catch (IOException ioe) {
        Util.report("Error getting resources from path", ioe);
    }
    return staticLoggerBinderPathSet;
}
```

- 可以看到`paths`的实际类型为`CompoundEnumeration`，而`path`的值为`slf4j-simple-1.7.32.jar`下的`StaticLoggerBinder.class`文件，完整路径为：`jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-simple/1.7.32/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class`

<img src="/Users/holyhigh/Library/Application Support/typora-user-images/image-20220109151021143.png" alt="image-20220109151021143" style="zoom:50%;" />

- 此时已经自动判别日志实现为`slf4j-simple`，`SLF4J `发行版附带了几个称为“SLF4J 绑定”的 jar 文件，每个绑定对应一个受支持的框架，共有如下6个，都是最流行的日志框架，先于`slf4j`或同步发行。此时会如何进行选择？

```xml
<!--绑定NOP，默默地丢弃所有日志记录-->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-nop</artifactId>
  <version>1.7.32</version>
</dependency>
<!--将所有事件输出到System.err。只打印INFO级别和更高级别的消息-->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>1.7.32</version>
</dependency>
<!--log4j 1.2版的绑定，一个广泛使用的日志框架。使用时必须编写log4j.xml配置，否则报错-->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-log4j12</artifactId>
  <version>1.7.32</version>
</dependency>
<!--java.util.logging的绑定，也称为JDK 1.4日志记录-->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-jdk14</artifactId>
  <version>1.7.32</version>
</dependency>
<!--Jakarta Commons Logging的绑定。此绑定会将所有SLF4J日志记录委托给JCL-->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-jcl</artifactId>
  <version>1.7.32</version>
</dependency>
<!--Logback的类是SLF4J接口的直接实现-->
<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
  <version>1.2.7</version>
</dependency>
```

<img src="/Users/holyhigh/Library/Application Support/typora-user-images/image-20220109165946332.png" alt="image-20220109165946332" style="zoom:50%;" />

- 运行代码，可以看到按顺序识别到了上述6个日志实现，最终绑定了`slf4j-nop`作为日志实现，所以最终没有打印出日志

```
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-nop/1.7.32/slf4j-nop-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-simple/1.7.32/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-log4j12/1.7.32/slf4j-log4j12-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-jdk14/1.7.32/slf4j-jdk14-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/org/slf4j/slf4j-jcl/1.7.32/slf4j-jcl-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/holyhigh/.m2/repository/ch/qos/logback/logback-classic/1.2.7/logback-classic-1.2.7.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.helpers.NOPLoggerFactory]
```

- 打开<http://www.slf4j.org/codes.html#multiple_bindings可以看到在类路径上发现了多个绑定时，最终绑定是由`JVM`随机决定的。另外作者建议类库等嵌入式组件只应该声明，不应该声明具体的日志实现，应该交由用户自行决定使用哪个日志实现。对应已经指定的类库，使用时可以通过`<exclusions/>`标签排除依赖

```
The warning emitted by SLF4J is just that, a warning. Even when multiple bindings are present, SLF4J will pick one logging framework/implementation and bind with it. The way SLF4J picks a binding is determined by the JVM and for all practical purposes should be considered random. As of version 1.6.6, SLF4J will name the framework/implementation class it is actually bound to.

SLF4J 发出的警告就是一个警告。即使存在多个绑定，SLF4J 也会选择一个日志框架/实现并与之绑定。SLF4J 选择绑定的方式是由 JVM 决定的，并且出于所有实际目的应该被认为是随机的。从 1.6.6 版本开始，SLF4J 将命名它实际绑定到的框架/实现类。
```

- 虽然官方说日志实现是由`JVM`随机决定，但按照实际测试，发现最终加载的总是定义在最前面的日志实现。其真正的原理时通过`spi`技术，由日志实现框架定义`StaticLoggerBinder`并实现由`slf4j-api`定义的静态日志绑定接口`LoggerFactoryBinder`，在具体绑定时，通过调用类加载器的`ClassLoader.getResources("org/slf4j/impl/StaticLoggerBinder.class")`方法并传入指定的实现类全限定名，在类路径下查找并加载第一个找到的日志实现类`StaticLoggerBinder.class`，完成绑定操作。
- 以下为官方说明的依赖绑定关系图

<img src="/Users/holyhigh/Downloads/concrete-bindings.png" alt="concrete-bindings" style="zoom:75%;" />

- **注意！！！** `log4j-over-slf4j.jar`和`slf4j-log4j12.jar`不能同时存在，否则会导致`StackOverflowError`

```
The purpose of slf4j-log4j12 module is to delegate or redirect calls made to an SLF4J logger to log4j. The purpose of the log4j-over-slf4j module is to redirect calls made to a log4j logger to SLF4J. If SLF4J is bound withslf4j-log4j12.jar and log4j-over-slf4j.jar is also present on the class path, a StackOverflowError will inevitably occur immediately after the first invocation of an SLF4J or a log4j logger.

slf4j-log4j12 模块的目的是将对 SLF4J 记录器的调用委托或重定向到 log4j。log4j-over-slf4j 模块的目的是将对 log4j 记录器的调用重定向到 SLF4J。如果 SLF4J 与slf4j-log4j12.jar绑定， 并且log4j-over-slf4j.jar也存在于类路径中，StackOverflowError则在第一次调用 SLF4J 或 log4j 记录器后将不可避免地立即发生。
```

- 同样的，`jcl-over-slf4j.jar`和`slf4j-jcl.jar`不能同时存在，否则会导致`StackOverflowError`

```
The purpose of slf4j-jcl module is to delegate or redirect calls made to an SLF4J logger to jakarta commons logging (JCL). The purpose of the jcl-over-slf4j module is to redirect calls made to a JCL logger to SLF4J. If SLF4J is bound with slf4j-jcl.jar and jcl-over-slf4j.jar is also present on the class path, then a StackOverflowError will inevitably occur immediately after the first invocation of an SLF4J or a JCL logger.

slf4j-jcl 模块的目的是将对 SLF4J 记录器的调用委托或重定向到 jakarta commons logging (JCL)。jcl-over-slf4j 模块的目的是将对 JCL 记录器的调用重定向到 SLF4J。如果 SLF4J 与slf4j-jcl.jar绑定， 并且jcl-over-slf4j.jar也存在于类路径中，那么StackOverflowError 在第一次调用 SLF4J 或 JCL 记录器后将不可避免地立即发生。
```

