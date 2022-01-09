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
