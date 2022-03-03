package merchant.controllers;

import com.google.gson.Gson;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;

@Controller
@RequestMapping("/setup")
public class Setup {
    private static final String[] myKeyPath = {"keys", "mprivkey.pem"};
    private PrivateKey myPrivateKey;

    @PostMapping
    public String initiateSetup(@RequestBody String jsonBody) {
        SealedObject[] keys = new Gson().fromJson(jsonBody, SealedObject[].class);
        PublicKey clientKey = getKey(keys);
        System.out.println(clientKey);
        return null;
    }

    public Setup() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, URISyntaxException {
        myPrivateKey = getMyPrivateKey();
    }

    private PublicKey getKey(SealedObject[] keys) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, myPrivateKey);
            SecretKey secretKey = (SecretKey) keys[1].getObject(cipher);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, generateIv());
            PublicKey clientPublicKey = (PublicKey) keys[0].getObject(cipher);
            return clientPublicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        return new IvParameterSpec(iv);
    }

    private PrivateKey getMyPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, URISyntaxException {
        byte[] keyBytes = Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(Paths.get(myKeyPath[0], myKeyPath[1]).toString()).toURI()));
        System.out.println(keyBytes.toString());

        RSAPrivateKeySpec spec =
                new RSAPrivateCrtKeySpec();
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
