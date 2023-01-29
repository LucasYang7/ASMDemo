package com.github.megatronking.stringfog.plugin;

import com.google.common.collect.ImmutableSet;
import com.squareup.javawriter.JavaWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.lang.model.element.Modifier;

/**
 * Generate the <code>StringFog</code> class.
 *
 * @author Megatron King
 * @since 2018/9/20 17:41
 */
public final class StringFogClassGenerator {


    public static void generate(File outputFile, String packageName, String className,
                                String implementation, StringFogMode mode) throws IOException {
        File outputDir = outputFile.getParentFile();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Can not mkdirs the dir: " + outputDir);
        }

        int lastIndexOfDot = implementation.lastIndexOf(".");
        String implementationSimpleClassName = lastIndexOfDot == -1 ? implementation :
                implementation.substring(implementation.lastIndexOf(".") + 1);

        JavaWriter javaWriter = new JavaWriter(new FileWriter(outputFile));
        javaWriter.emitPackage(packageName);
        javaWriter.emitEmptyLine();
        javaWriter.emitImports(implementation);
        javaWriter.emitEmptyLine();

        javaWriter.emitJavadoc("Generated code from StringFog gradle plugin. Do not modify!");
        javaWriter.beginType(className, "class", ImmutableSet.of(Modifier.PUBLIC,
                Modifier.FINAL));

        javaWriter.emitField(implementationSimpleClassName, "IMPL",
                ImmutableSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL),
                "new " + implementationSimpleClassName + "()");

        javaWriter.emitEmptyLine();
        if (mode == StringFogMode.base64) {
            javaWriter.beginMethod(String.class.getSimpleName(), "decrypt",
                    ImmutableSet.of(Modifier.PUBLIC, Modifier.STATIC),
                    String.class.getSimpleName(), "value",
                    String.class.getSimpleName(), "key");
            javaWriter.emitStatement("return IMPL.decrypt(java.util.Base64.getDecoder().decode(value), " +
                    "java.util.Base64.getDecoder().decode(key))");
            javaWriter.endMethod();
        } else if (mode == StringFogMode.bytes) {
            javaWriter.beginMethod(String.class.getSimpleName(), "decrypt",
                    ImmutableSet.of(Modifier.PUBLIC, Modifier.STATIC),
                    byte[].class.getSimpleName(), "value",
                    byte[].class.getSimpleName(), "key");
            javaWriter.emitStatement("return IMPL.decrypt(value, key)");
            javaWriter.endMethod();
        }

        javaWriter.emitEmptyLine();
        javaWriter.endType();

        javaWriter.close();
    }
}