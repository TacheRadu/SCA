package crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;

public class Asymmetric {
    public static byte[] encryptData(byte[] data, PublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        cipher.update(data);
        return cipher.doFinal();
    }

    public static byte[] decryptData(byte[] encryptedData, PrivateKey decryptionKey) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        return cipher.doFinal(encryptedData);
    }

    public static byte[] signData(byte[] data, PrivateKey signingKey) throws Exception {
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(signingKey);
        dsa.update(data);
        return dsa.sign();
    }

    public static boolean verifySignedData(byte[]data, byte[] signedData, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signedData);
    }
}
