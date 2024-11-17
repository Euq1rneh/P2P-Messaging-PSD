package common;

import java.io.Serializable;

public class EncryptedPacket implements Serializable {
	private final byte[] encryptedData;
    private final byte[] encryptedAESKey;
    private final byte[] iv;

    public EncryptedPacket(byte[] encryptedData, byte[] encryptedAESKey, byte[] iv) {
        this.encryptedData = encryptedData;
        this.encryptedAESKey = encryptedAESKey;
        this.iv = iv;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }

    public byte[] getIv() {
        return iv;
    }
}
