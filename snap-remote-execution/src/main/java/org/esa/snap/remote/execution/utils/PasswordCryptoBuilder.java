package org.esa.snap.remote.execution.utils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Created by jcoravu on 21/1/2019.
 */
public class PasswordCryptoBuilder {

    protected static final byte ITERATION_COUNT = 10;

    private PasswordCryptoBuilder() {
    }

    public static void main(String[] args) throws Exception {
        String originalPassword = "secret";
        System.out.println("Original password: " + originalPassword);

        String encryptedPassword = encrypt(originalPassword);
        System.out.println("Encrypted password: " + encryptedPassword);

        String decryptedPassword = decrypt(encryptedPassword);
        System.out.println("Decrypted password: " + decryptedPassword);
    }

    /**
     * Encodes the password.
     *
     * @return the String containing the encrypted password
     */
    public static String encrypt(String passwordToEncrypt) throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] salt = getSalt();
        int iterationCount = ITERATION_COUNT;
        int keyLength = 128;
        SecretKeySpec secretKey = buildSecretKey(salt, iterationCount, keyLength);

        Cipher pbeCipher = buildCipher();
        pbeCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        AlgorithmParameters parameters = pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = pbeCipher.doFinal(passwordToEncrypt.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();

        return iterationCount + ":" + convertToHexa(salt) + ":" + base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    /**
     * Decodes the password.
     *
     * @return the String containing the plain text password
     */
    public static String decrypt(String passwordToDecrypt) throws GeneralSecurityException, IOException {
        String[] parts = passwordToDecrypt.split(":");

        int iterationCount = Integer.parseInt(parts[0]);
        byte[] salt = convertFromHexa(parts[1]);
        String iv = parts[2];
        String property = parts[3];

        int keyLength = 128;
        SecretKeySpec secretKey = buildSecretKey(salt, iterationCount, keyLength);

        Cipher pbeCipher = buildCipher();
        pbeCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(base64Decode(iv)));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    /**
     * Create a cipher instance to encrypt/decrypt the passwords.
     *
     * @return the cipher instance to encrypt/decrypt the passwords.
     */
    private static Cipher buildCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    /**
     * Create a secret key instance useful for raw secret keys that can be
     * represented as a byte array and have no key parameters associated with
     * them.
     *
     * @return the secret key instance
     */
    private static SecretKeySpec buildSecretKey(byte[] salt, int iterationCount, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String password = "alabalaportocala";
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    /**
     * Encodes the specified byte array into a String using the Base64 encoding
     * scheme.
     *
     * @return the String containing the resulting Base64 encoded characters
     */
    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decodes a Base64 encoded String into a newly-allocated byte array using
     * the Base64 encoding scheme.
     *
     * @return the byte array containing the decoded bytes
     */
    private static byte[] base64Decode(String property) throws IOException {
        return Base64.getDecoder().decode(property);
    }

    /**
     * Convert a hexa value to byte array.
     *
     * @return the byte array
     */
    private static byte[] convertFromHexa(String hex) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    /**
     * Convert a byte array to a hexa value.
     *
     * @return the hexa value
     */
    private static String convertToHexa(byte[] array) throws NoSuchAlgorithmException {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        }
        return hex;
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @return the user-specified number of random bytes
     */
    private static byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }
}
