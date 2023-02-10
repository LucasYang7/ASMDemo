package com.github.megatronking.stringfog.plugin;


import com.github.megatronking.stringfog.IKeyGenerator;
import com.github.megatronking.stringfog.IStringFog;
import com.github.megatronking.stringfog.plugin.utils.Log;
import com.github.megatronking.stringfog.plugin.utils.TextUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * A factory creates {@link ClassVisitor}.
 *
 * @author Megatron King
 * @since 2017/3/7 19:56
 */

public final class ClassVisitorFactory {

    private ClassVisitorFactory() {
    }

    public static ClassVisitor create(IStringFog stringFogImpl, StringFogMappingPrinter mappingPrinter,
                                      String[] fogPackages, IKeyGenerator kg, String fogClassName,
                                      String className, StringFogMode mode, ClassWriter cw) {
        if (WhiteLists.inWhiteList(className) || !isInFogPackages(fogPackages, className)) {
            Log.v("StringFog ignore: " + className);
            return createEmpty(cw);
        }
        Log.v("StringFog execute: " + className);
        return new StringFogClassVisitor(stringFogImpl, mappingPrinter, fogClassName, cw, kg, mode);
    }

    private static ClassVisitor createEmpty(ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM7, cw) {
        };
    }

    private static boolean isInFogPackages(String[] fogPackages, String className) {
        if (TextUtils.isEmpty(className)) {
            return false;
        }
        if (fogPackages == null || fogPackages.length == 0) {
            // default we fog all packages.
            return true;
        }
        for (String fogPackage : fogPackages) {
            if (className.replace('/', '.').startsWith(fogPackage + ".")) {
                return true;
            }
        }
        return false;
    }

}