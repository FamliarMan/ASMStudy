package com.jianglei.asmplugin


import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class ASMUtils {

    public static ClassWriter insertByteCode(File inputFile) {
        def inputStream = new FileInputStream(inputFile)
        def classReader = new ClassReader(inputStream)
        def classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        def classVisitor = new TraceClassVisitor(Opcodes.ASM5, classWriter)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classWriter

    }

}