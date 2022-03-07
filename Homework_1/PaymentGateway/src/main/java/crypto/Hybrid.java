package crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Hybrid {
    public static byte[][] encryptData(byte[][] data, PublicKey publicKey, SecretKey secretKey) throws Exception {
        byte[][] res = new byte[data.length + 1][];
        for (int i = 0; i < data.length; i++) {
            res[i] = Symmetric.encryptData(data[i], secretKey);
        }
        res[data.length] = Asymmetric.encryptData(secretKey.getEncoded(), publicKey);
        return res;
    }


    public static byte[][] decryptData(byte[][] data, PrivateKey privateKey) throws Exception {
        byte[][] res = new byte[data.length - 1][];
        byte[] decodedKey = Asymmetric.decryptData(data[data.length - 1], privateKey);
        SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        for (int i = 0; i < res.length; i++) {
            res[i] = Symmetric.decryptData(data[i], secretKey);
        }
        return res;
    }

}
