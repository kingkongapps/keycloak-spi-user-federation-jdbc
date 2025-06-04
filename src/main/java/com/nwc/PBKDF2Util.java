package com.nwc;

import org.jboss.logging.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PBKDF2Util {
    private static final Logger logger = Logger.getLogger(PBKDF2Util.class);

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    public static void main(String[] args) throws Exception {
        String salt = "h3G9JKslWw4LJvFxDaWx3A==";
        String hashPwd = PBKDF2Util.hashPassword("inmokang11", salt);
        System.out.println(hashPwd);
    }
    /**
     * 비밀번호를 해시하는 함수
     */
    public static String hashPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        logger.debug("PBKDF2Util: hashPassword()...password=" + password + ", salt=" + salt);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 저장된 해시와 비교하는 함수
     */
    public static boolean verifyPassword(String inputPassword, String salt, String expectedHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        logger.debug("PBKDF2Util: verifyPassword()...inputPassword=" + inputPassword + ", salt=" + salt);

        String inputHash = hashPassword(inputPassword, salt);

        logger.debug("PBKDF2Util: verifyPassword()...inputHash=====" + inputHash);
        logger.debug("PBKDF2Util: verifyPassword()...expectedHash==" + expectedHash);

        boolean result = inputHash.equals(expectedHash);
        logger.debug("PBKDF2Util: verifyPassword()...result========" + result);

        return result;
    }
}
