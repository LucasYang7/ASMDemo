package com.github.megatronking.stringfog.plugin;

import com.github.megatronking.stringfog.plugin.utils.Log;
import com.github.megatronking.stringfog.plugin.utils.TextUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Output StringFog mapping to file.
 *
 * @author Megatron King
 * @since 2018/9/21 11:07
 */
/* package */ final class StringFogMappingPrinter {

    private final File mMappingFile;
    private BufferedWriter mWriter;

    private String mCurrentClassName;

    /* package */ StringFogMappingPrinter(File mappingFile) {
        this.mMappingFile = mappingFile;
    }

    /* package */ void startMappingOutput(String implementation, StringFogMode mode) {
        try {
            if (mMappingFile.exists() && !mMappingFile.delete()) {
                throw new IOException("delete stringfog mappingFile failed");
            }
            File dir = mMappingFile.getParentFile();
            if (dir.exists() || dir.mkdirs()) {
                mWriter = new BufferedWriter(new FileWriter(mMappingFile));
            } else {
                throw new IOException("Failed to create dir: " + dir.getPath());
            }
            mWriter.write("stringfog impl: " + implementation);
            mWriter.newLine();
            mWriter.write("stringfog mode: " + mode);
            mWriter.newLine();
        } catch (IOException e) {
            Log.e("Create stringfog mapping file failed.");
        }
    }

    /* package */ void output(String line) {
        try {
            mWriter.write(line);
            mWriter.newLine();
        } catch (IOException e) {
            // Ignore
        }
    }

    /* package */ void output(String className, String originValue, String encryptValue) {
        if (TextUtils.isEmpty(className)) {
            return;
        }
        try {
            if (!className.equals(mCurrentClassName)) {
                mWriter.newLine();
                mWriter.write("[" + className + "]");
                mWriter.newLine();
                mCurrentClassName = className;
            }
            mWriter.write(originValue + " -> " + encryptValue);
            mWriter.newLine();
        } catch (IOException e) {
            // Ignore
        }
    }

    /* package */ void endMappingOutput() {
        if (mWriter != null) {
            IOUtils.closeQuietly(mWriter);
        }
    }

}