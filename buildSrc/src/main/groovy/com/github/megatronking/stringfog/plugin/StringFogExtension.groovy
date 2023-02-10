package com.github.megatronking.stringfog.plugin

import com.github.megatronking.stringfog.IKeyGenerator
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import com.github.megatronking.stringfog.plugin.StringFogMode

/**
 * StringFog extension.
 * <p>
 * <code>
 * apply plugin: 'stringfog'
 *
 * stringfog {
 *     implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
 * }
 * </code>
 *
 * @author Megatron King
 * @since 2017/3/7 17:44
 */

public class StringFogExtension {

    static def base64 = StringFogMode.base64
    static def bytes = StringFogMode.bytes

    /**
     * The algorithm implementation for String encryption and decryption.
     * It is required.
     */
    String implementation

    /**
     * A generator to generate a security key for the encryption and decryption.
     *
     * StringFog use a 8 length random key generator default.
     */
    IKeyGenerator kg = new RandomKeyGenerator()

    /**
     * How the encrypted string presents in java class, default is base64.
     */
    StringFogMode mode = base64

    /**
     * Enable or disable the StringFog plugin. Default is enabled.
     */
    boolean enable = true

    /**
     * Enable or disable the StringFog debug message print. Default is disabled.
     */
    boolean debug = false

    /**
     * The java packages will be applied. Default is effect on all packages.
     */
    String[] fogPackages = []

}