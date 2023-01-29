package com.github.megatronking.stringfog;

/**
 * Interface of how to encrypt and decrypt a string.
 *
 * @author Megatron King
 * @since 2018/9/20 16:15
 */
public interface IStringFog {

    /**
     * Encrypt the data by the special key.
     *
     * @param data The original data.
     * @param key Encrypt key.
     * @return The encrypted data.
     */
    byte[] encrypt(String data, byte[] key);

    /**
     * Decrypt the data to origin by the special key.
     *
     * @param data The encrypted data.
     * @param key Encrypt key.
     * @return The original data.
     */
    String decrypt(byte[] data, byte[] key);

    /**
     * Whether the string should be encrypted.
     *
     * @param data The original data.
     * @return If you want to skip this String, return false.
     */
    boolean shouldFog(String data);

}