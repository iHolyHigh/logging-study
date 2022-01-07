**Java简单日志门面**

**Simple Logging Facade for Java**

- `SpingBoot`项目必须带有`spring-boot-starter`依赖，其中包含如下日志依赖

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-logging</artifactId>
  <version>2.6.2</version>
  <scope>compile</scope>
</dependency>
```

- 而`spring-boot-starter-logging`包括以下依赖，每个依赖都引用了`slf4j-api`。所以`SpringBoot`支持`SLF4J`日志门面，`logback`/`log4j`/`juc`日志实现，默认采用`SLF4J`+`Logback`

```xml
<dependencies>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.2.9</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-to-slf4j</artifactId>
      <version>2.17.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jul-to-slf4j</artifactId>
      <version>1.7.32</version>
      <scope>compile</scope>
    </dependency>
</dependencies>
```

- 为了测试，需要将这些日志依赖去掉，使用如下方式，此时项目中就没有任何日志框架了。只能通过`System.out`打印日志，或采用`JDK1.4`原生的`JUL`

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

- 使用原生JUL测试，代码如下所示，只做简单测试

```java
package com.holyhigh.loggingstudy.jul;

import java.util.logging.Logger;

/**
 * JUL测试
 * JDK1.4原生日志框架Java Util Logging
 *
 * @author holyhigh
 * @version 0.0.1
 * @since 1/8/22 12:25 AM
 */
public class JulTest {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(JulTest.class.getName());
        logger.info("Hello JUL!");
    }
}
```

- 执行结果如下

```
一月 08, 2022 12:41:49 上午 com.holyhigh.loggingstudy.jul.JulTest main
信息: Hello JUL!
```

- 接下来演示`SLF4J`使用，首先需要引入依赖

```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.32</version>
</dependency>
```

- 测试代码如下

```java
package com.holyhigh.loggingstudy.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J测试
 * Java简单日志门面：Simple Logging Facade for Java
 *
 * @author holyhigh
 * @version 0.0.1
 * @since 1/8/22 12:39 AM
 */
public class Slf4jTest {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Slf4jTest.class);
        logger.info("Hello SLF4J!");
    }
}
```

- 测试结果如下，报错没有找到具体的日志实现，默认采用不打印的方式处理。也就是说`slf4j-api`并不能单独使用

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

- 此时需要绑定日志实现框架，官方有一个对应的简单日志实现，可以直接集成依赖

```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>1.7.32</version>
</dependency>
```

- 再次执行，打印如下

```
[main] INFO com.holyhigh.loggingstudy.slf4j.Slf4jTest - Hello SLF4J!
```

