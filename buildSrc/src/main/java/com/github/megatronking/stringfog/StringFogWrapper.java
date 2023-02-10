package com.github.megatronking.stringfog;

/**
 * A wrapper for the real implementation of fogs.
 *
 * @author Megatron King
 * @since 2018/9/20 16:14
 */
public final class StringFogWrapper implements IStringFog {

    private final IStringFog mStringFogImpl;

    public StringFogWrapper(String impl) {
        try {
            mStringFogImpl = (IStringFog) Class.forName(impl).newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Stringfog implementation class not found: " + impl);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Stringfog implementation class new instance failed: "
                    + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Stringfog implementation class access failed: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] encrypt(String data, byte[] key) {
        return mStringFogImpl == null ? data.getBytes() : mStringFogImpl.encrypt(data, key);
    }

    @Override
    public String decrypt(byte[] data, byte[] key) {
        return mStringFogImpl == null ? new String(data) : mStringFogImpl.decrypt(data, key);
    }

    @Override
    public boolean shouldFog(String data) {
        return mStringFogImpl != null && mStringFogImpl.shouldFog(data);
    }

}