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
