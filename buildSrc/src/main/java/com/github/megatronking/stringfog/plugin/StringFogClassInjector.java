package com.github.megatronking.stringfog.plugin;

import com.github.megatronking.stringfog.IKeyGenerator;
import com.github.megatronking.stringfog.IStringFog;
import com.github.megatronking.stringfog.StringFogWrapper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class StringFogClassInjector {

    private final String[] mFogPackages;
    // 动态生成的StringFog.java，其内部会调用com.github.megatronking.stringfog.IStringFog.decrypt方法
    private final String mFogClassName;
    private final IKeyGenerator mKeyGenerator;
    private final IStringFog mStringFogImpl;
    private final StringFogMode mMode;
    private final StringFogMappingPrinter mMappingPrinter;

    public StringFogClassInjector(String[] fogPackages, IKeyGenerator kg, String implementation,
                                  StringFogMode mode, String fogClassName, StringFogMappingPrinter mappingPrinter) {
        this.mFogPackages = fogPackages;
        this.mKeyGenerator = kg;
        this.mStringFogImpl = new StringFogWrapper(implementation);
        this.mMode = mode;
        this.mFogClassName = fogClassName;
        this.mMappingPrinter = mappingPrinter;
    }

    public void doFog2Class(File fileIn, File fileOut) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(fileIn));
            os = new BufferedOutputStream(new FileOutputStream(fileOut));
            processClass(is, os);
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }

    public void doFog2Jar(File jarIn, File jarOut) throws IOException {
        boolean shouldExclude = shouldExcludeJar(jarIn);
        ZipInputStream zis = null;
        ZipOutputStream zos = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarIn)));
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jarOut)));
            ZipEntry entryIn;
            Map<String, Integer> processedEntryNamesMap = new HashMap<>();
            while ((entryIn = zis.getNextEntry()) != null) {
                final String entryName = entryIn.getName();
                if (!processedEntryNamesMap.containsKey(entryName)) {
                    ZipEntry entryOut = new ZipEntry(entryIn);
                    // Set compress method to default, fixed #12
                    if (entryOut.getMethod() != ZipEntry.DEFLATED) {
                        entryOut.setMethod(ZipEntry.DEFLATED);
                    }
                    entryOut.setCompressedSize(-1);
                    zos.putNextEntry(entryOut);
                    if (!entryIn.isDirectory()) {
                        if (entryName.endsWith(".class") && !shouldExclude) {
                            processClass(zis, zos);
                        } else {
                            copy(zis, zos);
                        }
                    }
                    zos.closeEntry();
                    processedEntryNamesMap.put(entryName, 1);
                }
            }
        } finally {
            closeQuietly(zos);
            closeQuietly(zis);
        }
    }

    /**
     * 核心函数，处理输入的.class文件，执行插桩后生成新的.class文件
     * */
    private void processClass(InputStream classIn, OutputStream classOut) throws IOException {
        ClassReader cr = new ClassReader(classIn);
        // skip module-info class, fixed #38
        if ("module-info".equals(cr.getClassName())) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = classIn.read(buffer)) >= 0) {
                classOut.write(buffer, 0, read);
            }
        } else {
            // 通过ASM执行字节码插桩
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = ClassVisitorFactory.create(mStringFogImpl, mMappingPrinter, mFogPackages,
                    mKeyGenerator, mFogClassName, cr.getClassName() , mMode, cw);
            cr.accept(cv, 0);
            classOut.write(cw.toByteArray());
            classOut.flush();
        }
    }

    private boolean shouldExcludeJar(File jarIn) throws IOException {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarIn)));
            ZipEntry entryIn;
            while ((entryIn = zis.getNextEntry()) != null) {
                final String entryName = entryIn.getName();
                if (entryName.contains("StringFog")) {
                    return true;
                }
            }
        } finally {
            closeQuietly(zis);
        }
        return false;
    }

    private void closeQuietly(Closeable target) {
        if (target != null) {
            try {
                target.close();
            } catch (Exception e) {
                // Ignored.
            }
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
    }

}