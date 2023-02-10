package com.github.megatronking.stringfog.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.api.BaseVariant
import com.android.utils.FileUtils
import com.github.megatronking.stringfog.IKeyGenerator
import com.github.megatronking.stringfog.plugin.utils.MD5
import com.google.common.collect.ImmutableSet
import com.google.common.io.Files
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task

abstract class StringFogTransform extends Transform {

    public static final String FOG_CLASS_NAME = 'StringFog'
    private static final String TRANSFORM_NAME = 'stringFog'

    protected StringFogClassInjector mInjector
    protected StringFogMappingPrinter mMappingPrinter

    protected String mImplementation
    protected StringFogMode mMode

    StringFogTransform(Project project, DomainObjectSet<BaseVariant> variants) {
        project.afterEvaluate {
            // 从build.gradle中读取stringfog插件的配置信息
            IKeyGenerator kg = project.stringfog.kg
            String[] fogPackages = project.stringfog.fogPackages
            String implementation = project.stringfog.implementation
            StringFogMode mode = project.stringfog.mode
            if (kg == null) {
                throw new IllegalArgumentException("Missing stringfog kg config")
            }
            if (implementation == null || implementation.length() == 0) {
                throw new IllegalArgumentException("Missing stringfog implementation config")
            }
            if (project.stringfog.enable) {
                def applicationId = variants.first().applicationId
                def manifestFile = project.file("src/main/AndroidManifest.xml")
                if (manifestFile.exists()) {
                    def parsedManifest = new XmlParser().parse(
                            new InputStreamReader(new FileInputStream(manifestFile), "utf-8"))
                    if (parsedManifest != null) {
                        def packageName = parsedManifest.attribute("package")
                        if (packageName != null) {
                            applicationId = packageName
                        }
                    }
                }
                createFogClass(project, fogPackages, kg, implementation, mode, variants, applicationId)
            } else {
                mMappingPrinter = null
                mInjector = null
            }
            mImplementation = implementation
            mMode = mode
        }
    }

    void createFogClass(def project, String[] fogPackages, IKeyGenerator kg, String implementation,
                        def mode, DomainObjectSet<BaseVariant> variants, def applicationId) {
        variants.all { variant ->
            def variantName = variant.name.toUpperCase()[0] + variant.name.substring(1, variant.name.length() - 1)
            Task generateTask = project.tasks.findByName(variantName)
            if (generateTask == null) {
                generateTask = project.tasks.create("generate${variantName}StringFog", DefaultTask)

                def stringfogDir = new File(project.buildDir, "generated" +
                        File.separatorChar + "source" + File.separatorChar + "stringfog" + File.separatorChar + variant.name)
                def stringfogFile = new File(stringfogDir, applicationId.replace((char)'.', File.separatorChar) + File.separator + "StringFog.java")
                // 注册动态生成Java代码的Task
                variant.registerJavaGeneratingTask(generateTask, stringfogDir)

                generateTask.doLast {
                    // 生成明文与密文对照的mapping文件
                    mMappingPrinter = new StringFogMappingPrinter(
                            new File(project.buildDir, "outputs/mapping/${variant.name.toLowerCase()}/stringfog.txt"))
                    // Create class injector
                    mInjector = new StringFogClassInjector(fogPackages, kg, implementation, mode,
                            applicationId + "." + FOG_CLASS_NAME, mMappingPrinter)

                    // Generate StringFog.java：用于调用com.github.megatronking.stringfog.xor.StringFogImpl.decrypt方法解密
                    StringFogClassGenerator.generate(stringfogFile, applicationId, FOG_CLASS_NAME,
                            implementation, mode)
                }
            }
        }
    }

    @Override
    String getName() {
        return TRANSFORM_NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        // 指定输入Transform输入的是class文件
        return ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return null
    }

    @Override
    boolean isIncremental() {
        // 支持增量
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        def dirInputs = new HashSet<>()
        def jarInputs = new HashSet<>()

        if (!transformInvocation.isIncremental()) {
            transformInvocation.getOutputProvider().deleteAll()
        }

        // Collecting inputs.
        transformInvocation.inputs.each { input ->
            input.directoryInputs.each { dirInput ->
                dirInputs.add(dirInput)
            }
            input.jarInputs.each { jarInput ->
                jarInputs.add(jarInput)
            }
        }

        if (mMappingPrinter != null) {
            mMappingPrinter.startMappingOutput(mImplementation, mMode)
        }

        if (!dirInputs.isEmpty() || !jarInputs.isEmpty()) {
            File dirOutput = transformInvocation.outputProvider.getContentLocation(
                    "classes", getOutputTypes(), getScopes(), Format.DIRECTORY)
            FileUtils.mkdirs(dirOutput)
            if (!dirInputs.isEmpty()) {
                dirInputs.each { dirInput ->
                    if (transformInvocation.incremental) {
                        dirInput.changedFiles.each { entry ->
                            File fileInput = entry.getKey()
                            File fileOutput = new File(fileInput.getAbsolutePath().replace(
                                    dirInput.file.getAbsolutePath(), dirOutput.getAbsolutePath()))
                            FileUtils.mkdirs(fileOutput.parentFile)
                            Status fileStatus = entry.getValue()
                            switch(fileStatus) {
                                case Status.ADDED:
                                case Status.CHANGED:
                                    if (fileInput.isDirectory()) {
                                        return // continue.
                                    }
                                    if (mInjector != null && fileInput.getName().endsWith('.class')) {
                                        // 输入.class文件进行字节码插桩操作
                                        mInjector.doFog2Class(fileInput, fileOutput)
                                    } else {
                                        Files.copy(fileInput, fileOutput)
                                    }
                                    break
                                case Status.REMOVED:
                                    if (fileOutput.exists()) {
                                        if (fileOutput.isDirectory()) {
                                            fileOutput.deleteDir()
                                        } else {
                                            fileOutput.delete()
                                        }
                                    }
                                    break
                            }
                        }
                    } else {
                        dirInput.file.traverse(type: FileType.FILES) { fileInput ->
                            File fileOutput = new File(fileInput.getAbsolutePath().replace(dirInput.file.getAbsolutePath(), dirOutput.getAbsolutePath()))
                            FileUtils.mkdirs(fileOutput.parentFile)
                            if (mInjector != null && fileInput.getName().endsWith('.class')) {
                                // 输入.class文件进行字节码插桩操作
                                mInjector.doFog2Class(fileInput, fileOutput)
                            } else {
                                Files.copy(fileInput, fileOutput)
                            }
                        }
                    }
                }
            }

            if (!jarInputs.isEmpty()) {
                jarInputs.each { jarInput ->
                    File jarInputFile = jarInput.file
                    File jarOutputFile = transformInvocation.outputProvider.getContentLocation(
                            getUniqueHashName(jarInputFile), getOutputTypes(), getScopes(), Format.JAR
                    )

                    FileUtils.mkdirs(jarOutputFile.parentFile)

                    switch (jarInput.status) {
                        case Status.NOTCHANGED:
                            if (transformInvocation.incremental) {
                                break
                            }
                        case Status.ADDED:
                        case Status.CHANGED:
                            if (mInjector != null) {
                                mInjector.doFog2Jar(jarInputFile, jarOutputFile)
                            } else {
                                Files.copy(jarInputFile, jarOutputFile)
                            }
                            break
                        case Status.REMOVED:
                            if (jarOutputFile.exists()) {
                                jarOutputFile.delete()
                            }
                            break
                    }
                }
            }
        }

        if (mMappingPrinter != null) {
            mMappingPrinter.endMappingOutput()
        }
    }

    static String getUniqueHashName(File fileInput) {
        final String fileInputName = fileInput.getName()
        if (fileInput.isDirectory()) {
            return fileInputName
        }
        final String parentDirPath = fileInput.getParentFile().getAbsolutePath()
        final String pathMD5 = MD5.getMessageDigest(parentDirPath.getBytes())
        final int extSepPos = fileInputName.lastIndexOf('.')
        final String fileInputNamePrefix =
                (extSepPos >= 0 ? fileInputName.substring(0, extSepPos) : fileInputName)
        return fileInputNamePrefix + '_' + pathMD5
    }

}
