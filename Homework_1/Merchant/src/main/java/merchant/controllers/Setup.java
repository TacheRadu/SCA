package merchant.controllers;

import com.google.gson.Gson;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        return new IvParameterSpec(iv);
    }

    private RSAPrivateKey getMyPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, URISyntaxException {
        File file = new File(getClass().getClassLoader()
                .getResource(Paths.get("keys", "mprivkey.pem").toString()).toURI());
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (FileReader keyReader = new FileReader(file);
             PemReader pemReader = new PemReader(keyReader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
        }
    }
}
