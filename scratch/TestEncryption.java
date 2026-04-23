package com.vaultkey.scratch;

import com.vaultkey.utils.EncryptionManager;

public class TestEncryption {
    public static void main(String[] args) {
        String master = "MySecretPassword";
        String plain = "HelloWorld";
        String enc = EncryptionManager.encrypt(plain, master);
        System.out.println("Encrypted: " + enc);
        String dec = EncryptionManager.decrypt(enc, master);
        System.out.println("Decrypted: " + dec);
    }
}
