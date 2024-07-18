package com.example.orion;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EncryptionModule {

    /**
     * Encrypts the given text using the provided public key.
     *
     * @param publicKey the public key used for encryption
     * @param text the plaintext to be encrypted
     * @return the encrypted text, encoded in Base64
     * @throws NoSuchPaddingException if the specified padding scheme is not available
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     * @throws InvalidKeyException if the provided key is invalid
     * @throws IllegalBlockSizeException if the plaintext size is invalid
     * @throws BadPaddingException if there's an issue with the padding
     */
    public static String runEncrypt(PublicKey publicKey, String text)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        PublicKey rsaPublicKey = publicKey;
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    /**
     * Decrypts the given text using the provided private key.
     *
     * @param privateKey the private key used for decryption
     * @param text the encrypted text, encoded in Base64
     * @return the decrypted text, or an error message if decryption fails
     * @throws NoSuchPaddingException if the specified padding scheme is not available
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     * @throws InvalidKeyException if the provided key is invalid
     * @throws IllegalBlockSizeException if the encrypted text size is invalid
     * @throws BadPaddingException if there's an issue with the padding
     */
    public static String runDecrypt(PrivateKey privateKey, String text)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        cipher1.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes;
        try {
            decryptedBytes = cipher1.doFinal(Base64.decode(text, Base64.DEFAULT));
        } catch (Exception e) {
            return "Private Key could not decrypt this input";
        }

        return new String(decryptedBytes);
    }
}
