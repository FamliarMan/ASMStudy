package com.jianglei.asmplugin

class LastConfig {

    private MethodTraceExtension extension
    /**
     * 用来保存上次编译中jar包的名称和输出路径对比，从而找出改变的jar
     */
    private HashMap<String,String> jarMap

    MethodTraceExtension getExtension() {
        return extension
    }

    void setExtension(MethodTraceExtension extension) {
        this.extension = extension
    }

    HashMap<String, String> getJarMap() {
        return jarMap
    }

    void setJarMap(HashMap<String, String> jarMap) {
        this.jarMap = jarMap
    }
}