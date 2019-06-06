package com.jianglei.asmplugin

import org.slf4j.LoggerFactory

import java.util.logging.Logger


class LogUtils {
    private static logger = LoggerFactory.getLogger("MethodTrace")

    def static i(String s) {
        logger.warn(s)
    }
    def static e(String s){
        logger.error(s)
    }

}