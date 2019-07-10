package com.xuexuan.traceplugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream

/**
 * create by xuexuan
 * time 2019/7/8 18:19
 */

class LogTransform : Transform() {
    override fun getName(): String {

        return "TracePlugin"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        println("==============================TracePlugin visit start========================================")

        var isIncremental = transformInvocation.isIncremental

        //OutputProvider管理输出路径，如果消费型输入为空，你会发现OutputProvider == null
        var outputProvider = transformInvocation.outputProvider

        if (!isIncremental) {
            //不需要增量编译，先清除全部
            outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                //处理Jar
                processJarInput(jarInput, outputProvider, isIncremental)
            }

            input.directoryInputs.forEach { directoryInput ->
                //处理文件
                processDirectoryInput(directoryInput, outputProvider, isIncremental)
            }
        }
        println("==============================TracePlugin visit end========================================")

    }

    //============================================jar文件修改总入口=======================================================================
    //jar输入文件 修改
    private fun processJarInput(jarInput: JarInput, outputProvider: TransformOutputProvider?, isIncremental: Boolean) {

        var dest = outputProvider?.getContentLocation(
                jarInput.file.absolutePath,
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR)
        if (isIncremental) {
            //处理增量编译
            processJarInputIncremental(jarInput, dest)
        } else {
            //不处理增量编译
            processJarInputNoIncremental(jarInput, dest)
        }
    }

    //jar 没有增量的修改
    private fun processJarInputNoIncremental(jarInput: JarInput, dest: File?) {
        transformJarInput(jarInput, dest)
    }

    //jar 增量的修改
    private fun processJarInputIncremental(jarInput: JarInput, dest: File?) {

        when (jarInput.status) {
            Status.NOTCHANGED -> {
            }

            Status.ADDED -> {
                //真正transform的地方
                transformJarInput(jarInput, dest)
            }
            Status.CHANGED -> {
                //Changed的状态需要先删除之前的
                if (dest?.exists() == true) {
                    FileUtils.forceDelete(dest)
                }
                //真正transform的地方
                transformJarInput(jarInput, dest)

            }
            Status.REMOVED ->
                //移除Removed
                if (dest?.exists() == true) {
                    FileUtils.forceDelete(dest)
                }
        }
    }


    //真正执行jar修改的函数
    private fun transformJarInput(jarInput: JarInput, dest: File?) {


        FileUtils.copyFile(jarInput.file, dest)


//        //重命名输出文件，因为可能同名，会覆盖
//        var jarName = jarInput?.name
//        var md5Name = DigestUtils.md5Hex(jarInput?.file?.absolutePath)
//        if (jarName?.endsWith(".jar") == true) {
//            jarName = jarName.substring(0, jarName.length - 1)
//        }
//        var tmpFile: File? = null
//
//        if (jarInput?.file?.absoluteFile?.endsWith(".jar") == true) {
//            var jarFile = JarFile(jarInput.file)
//
//            var enumeration = jarFile.entries()
//            tmpFile = File(jarInput.file.parent.toString() + File.separator + "classes_trace.jar")
//
//            //避免上次的缓存被重复插入
//            if (tmpFile.exists()) {
//                tmpFile.delete()
//            }
//
//            var jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
//            //用于保存
//
//            var processorList = arrayListOf<String>()
//
//            while (enumeration.hasMoreElements()) {
//                var jarEntry = enumeration.nextElement()
//                var entryName = jarEntry.name
//                var zipEntry = ZipEntry(entryName)
//                var inputStream = jarFile.getInputStream(jarEntry)
//
//                //如果是inject文件就跳过
//
//                if (entryName.endsWith(".class") && !entryName.contains("R\$") &&
//                        !entryName.contains("R.class") && !entryName.contains("BuildConfig.class")) {
//                    //class文件处理
//                    jarOutputStream.putNextEntry(zipEntry)
//                    var classReader = ClassReader(IOUtils.toByteArray(inputStream))
//                    var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
//                    var className = entryName.split(".class")[0]
//                    var cv = TraceVisitor(className, classWriter)
//                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
//                    var code = classWriter.toByteArray()
//                    jarOutputStream.write(code)
//
//                } else if (entryName.contains("META-INF/services/javax.annotation.processing.Processor")) {
//                    if (!processorList.contains(entryName)) {
//                        processorList.add(entryName)
//                        jarOutputStream.putNextEntry(zipEntry);
//                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
//                    } else {
//                        println("duplicate entry:$entryName")
//                    }
//                } else {
//                    jarOutputStream.putNextEntry(zipEntry)
//                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
//                }
//                jarOutputStream.closeEntry()
//            }
//
//            //结束
//            jarOutputStream.close()
//            jarFile.close()
//        }
//
//        if (tmpFile == null) {
//            //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
//            FileUtils.copyFile(jarInput.file, dest)
//        } else {
//            FileUtils.copyFile(tmpFile, dest)
//            tmpFile.delete()
//        }
    }

    //============================================================文件及文件夹修改总入口======================================================================
    private fun processDirectoryInput(directoryInput: DirectoryInput, outputProvider: TransformOutputProvider, isIncremental: Boolean) {
        var dest = outputProvider.getContentLocation(
                directoryInput.file.absolutePath,
                directoryInput.contentTypes,
                directoryInput.scopes,
                Format.DIRECTORY)
        if (isIncremental) {
            //处理增量编译
            processDirectoryInputIncremental(directoryInput, dest)
        } else {
            processDirectoryInputNoIncremental(directoryInput, dest)
        }
    }

    //文件无增量修改
    private fun processDirectoryInputNoIncremental(directoryInput: DirectoryInput, dest: File) {
        transformDirectoryInput(directoryInput, dest)
    }

    //文件增量修改
    private fun processDirectoryInputIncremental(directoryInput: DirectoryInput, dest: File) {
        FileUtils.forceMkdir(dest)
        val srcDirPath = directoryInput.file.absolutePath
        val destDirPath = dest.absolutePath
        val fileStatusMap = directoryInput.changedFiles
        fileStatusMap.forEach { entry ->
            val inputFile = entry.key
            val status = entry.value
            val destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
            val destFile = File(destFilePath)

            when (status) {
                Status.NOTCHANGED -> {
                }

                Status.ADDED -> {
                    //真正transform的地方
                    transformDirectoryInput(directoryInput, dest)
                }

                Status.CHANGED -> {
                    //处理有变化的
                    FileUtils.touch(destFile)
                    //Changed的状态需要先删除之前的
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest)
                    }
                    //真正transform的地方
                    transformDirectoryInput(directoryInput, dest)
                }

                Status.REMOVED ->
                    //移除Removed
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
            }
        }
    }

    //真正执行文件修改的地方
    private fun transformDirectoryInput(directoryInput: DirectoryInput, dest: File) {

//        directoryInput.forEach { directoryInput: DirectoryInput? ->
        //是否是目录
        if (directoryInput.file?.isDirectory == true) {
            val fileTreeWalk = directoryInput.file.walk()
            fileTreeWalk.forEach { file ->
                var name = file.name
                //在这里进行代码处理
                if (name.endsWith(".class") && !name.startsWith("R\$")
                        && "R.class" != name && "BuildConfig.class" != name) {

                    val classReader = ClassReader(file.readBytes())
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    val className = name.split(".class")[0]
                    val classVisitor = TraceVisitor(className, classWriter)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    val code = classWriter.toByteArray()
                    val fos = FileOutputStream(file.parentFile.absoluteFile.toString() + File.separator + name)
                    fos.write(code)
                    fos.close()
                }
            }
        }

        //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    private fun transformSingleFile(inputFile: File, destFile: File, srcDirPath: String) {
        FileUtils.copyFile(inputFile, destFile)
    }
}