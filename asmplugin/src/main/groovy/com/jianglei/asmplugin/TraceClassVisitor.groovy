package com.jianglei.asmplugin

import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter


public class TraceClassVisitor extends ClassVisitor {

    private String className

    TraceClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] exceptions) {
        super.visit(version, access, name, signature, superName, exceptions)
        this.className = name
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        def methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        return new TraceMethodVisitor(api, methodVisitor, access, name, desc, className)
    }
}

public class TraceMethodVisitor extends AdviceAdapter {

    private String className
    private String methodName
    //某个方法是否需要追踪
    private boolean isNeedTraceMethod

    protected TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc)
        this.className = className
        this.methodName = name
        isNeedTraceMethod = !(name == "<init>" || name == "<clinit>")
    }

    @Override
    void visitTypeInsn(int opcode, String type) {
        if (opcode == NEW && type == "java/lang/Thread") {
            //将原来的new Thread换成 new CustomThread
            mv.visitTypeInsn(NEW, "com/jianglei/testlibrary/CustomThread")
        } else {
            super.visitTypeInsn(opcode, type)
        }
    }

    @Override
    void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        //将所有对Thread的方法调用替换成对CustomThread方法的调用
        if (className != "com/jianglei/testlibrary/CustomThread"
                && opcode == INVOKESPECIAL && owner == "java/lang/Thread") {
            //CustomThread本身的方法不需要修改
            mv.visitMethodInsn(INVOKESPECIAL, "com/jianglei/testlibrary/CustomThread", name, desc, itf)
            LogUtils.i(String.format("replace thread :%s:%s:%s", className, methodName, name))
            return
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    private int timeLocalIndex = 0

    @Override
    protected void onMethodEnter() {
        if (!isNeedTraceMethod) {
            super.onMethodEnter()
            return
        }
        LogUtils.i(" insert method:" + className + " : " + methodName)
        super.onMethodEnter()
        timeLocalIndex = newLocal(Type.LONG_TYPE)
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        //将结果保存在变量表中
        mv.visitVarInsn(LSTORE, timeLocalIndex)
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (!isNeedTraceMethod) {
            return
        }
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        //将方法进入是的记录的时间入栈
        mv.visitVarInsn(LLOAD, timeLocalIndex)
        //相减
        mv.visitInsn(LSUB)
        //将相减的结果保存在变量表中
        mv.visitVarInsn(LSTORE, timeLocalIndex)

        //下面开始插入Log.d("longyi","$methodName:45 ms")

        mv.visitLdcInsn("longyi")
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        mv.visitLdcInsn(className + " : ")
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitLdcInsn(methodName + ": ")
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitVarInsn(LLOAD, timeLocalIndex)
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
        mv.visitInsn(POP)


    }
}