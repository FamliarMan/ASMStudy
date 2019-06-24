package com.jianglei.asmplugin

class MethodTraceExtension implements Serializable {
    /**
     * 是否追踪第三方依赖的方法执行数据
     */
    boolean traceThirdLibrary = false

    public MethodTraceExtension() {

    }

    public MethodTraceExtension(MethodTraceExtension extension) {
        this.traceThirdLibrary = extension.traceThirdLibrary
    }


    boolean getTraceThirdLibrary() {
        return traceThirdLibrary
    }

    void setTraceThirdLibrary(boolean traceThirdLibrary) {
        this.traceThirdLibrary = traceThirdLibrary
    }
}