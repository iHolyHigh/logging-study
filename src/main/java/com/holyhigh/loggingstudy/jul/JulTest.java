package com.holyhigh.loggingstudy.jul;

import java.util.logging.Logger;

/**
 * JUL测试
 * JDK1.4原生日志框架：Java Util Logging
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
