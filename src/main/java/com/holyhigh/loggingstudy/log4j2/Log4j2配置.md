[官方文档](https://www.docs4dev.com/docs/zh/log4j2/2.x/all/manual-index.html)

```
Apache根据Logback的设计思想，将Log4j 1.x进行重写，并且引入了大量丰富的特性，推出Log4j2
如果要单独使用log4j2只需要引入log4j-core，会自动引入log4j-api，并配置log4j2.xml即可正常使用
如果要配合使用slf4j只需要引入log4j-slf4j-impl，会自动引入引入log4j-core和slf4j-api，并配置log4j2.xml即可正常使用
```

- 具体引入依赖如下

```xml
<!--如果要单独使用log4j2只需要引入log4j-core即可-->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-core</artifactId>
  <version>2.14.1</version>
</dependency>
<!--如果要配合使用slf4j 1.7.x或更早版本只需要引入log4j-slf4j-impl即可-->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-slf4j-impl</artifactId>
  <version>2.14.1</version>
</dependency>
<!--如果要配合使用slf4j 1.8.x或更高版本只需要引入log4j-slf4j-impl即可-->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-slf4j18-impl</artifactId>
  <version>2.14.1</version>
</dependency>
```

- 在`springBoot`项目中，也可以使用`spring-boot-starter-log4j2`依赖引入

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

- 测试代码如下

```java
package com.holyhigh.loggingstudy.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log4j2测试
 * Apache根据Logback的设计思想，将Log4j 1.x进行重写，并且引入了大量丰富的特性，推出Log4j2
 * 如果要单独使用log4j2只需要引入log4j-core，会自动引入log4j-api，并配置log4j2.xml才能正常使用
 * 如果要配合使用slf4j只需要引入log4j-slf4j-impl即可
 *
 * @author holyhigh
 * @version 0.0.1
 * @since 1/9/22 6:20 PM
 */
public class Log4j2Test {
    public static void main(String[] args) {
        // Log4j2自身也提供了日志门面，即LogManager，但一般不用，因为SLF4J才是主流，被大多数日志实现框架所支持
        Logger logger = LogManager.getLogger(Log4j2Test.class);
        logger.info("Hello Log4j2!");

        // 使用slf4j获取log4j2的日志实现
        org.slf4j.Logger log = LoggerFactory.getLogger(Log4j2Test.class);
        log.info("Hello Log4j2!");
    }
}
```

- 日志没有任何打印，是因为没有编写`log4j2.xml`配置文件。将其引入，并放到`resources`下即可。

```yaml
# 支持4种配置文件，不指定情况下优先级为寻找classpath下的：log4j2.properties > log4j2.yml(log4j2.yaml) > log4j2.jsn(log4j2.json) > log4j2.xml
Configuration:
  status: warn # status：用于设置log4j2自身内部的信息输出，可以不设置
  monitorInterval: 30 # monitorInterval：log4j2监测配置文件的时间间隔，如果文件被修改，则重新加载

  Properties: # 定义全局变量
    Property:
      - name: log.path # 日志文件保存位置
        value: logs
      - name: project.name # 日志文件名称
        value: logging-study
      - name: file.pattern # 日志文件的转移和重命名规则
        value: "${log.path}/$${date:yyyy-MM}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
      - name: log.pattern # 日志打印格式
        value: "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p ${PID:-} [%15.15t] %-30.30C{1.} : %m%n"
      - name: log.level.console
        value: info

  Appenders:
    Console:  # 输出到控制台
      name: CONSOLE
      target: SYSTEM_OUT
      PatternLayout:
        pattern: ${log.pattern}
    RollingFile: # 打印到文件
      - name: ROLLING_FILE
        fileName: ${log.path}/${project.name}.log # 指定当前日志文件的位置和文件名称
        filePattern: ${file.pattern} # 指定当发生Rolling时，文件的转移和重命名规则
        PatternLayout:
          pattern: ${log.pattern}
        Filters:
          ThresholdFilter:
            - level: ${sys:log.level.console} # "sys:"表示：如果VM参数中没指定这个变量值，则使用本文件中定义的缺省全局变量值。
              onMatch: ACCEPT
              onMismatch: DENY
        Policies:
          TimeBasedTriggeringPolicy:
            modulate: true
            interval: 1 # 保留1天
        DefaultRolloverStrategy:
          max: 100 # 保留100个文件

  Loggers: # 只有定义了Loggers，并引入Appenders才会生效
    Root:
      level: info # 共有8个级别，按照从低到高为：ALL < TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF。
      AppenderRef:
        - ref: CONSOLE
        - ref: ROLLING_FILE

    Logger: # 为com.***包配置特殊的Log级别，方便调试，trace会打印SQL具体结果，debug只打印SQL语句
      - name: com.***.mapper
        additivity: false
        level: trace
        AppenderRef:
          - ref: CONSOLE
          - ref: ROLLING_FILE
```

- 此时执行代码，将会控制台打印，并且会生成`logs/logging-study.log`文件

```
2022-01-09 20:09:48.452 - INFO  [           main] c.h.l.l.Log4j2Test             : Hello Log4j2!
2022-01-09 20:09:48.463 - INFO  [           main] c.h.l.l.Log4j2Test             : Hello Log4j2!
```

