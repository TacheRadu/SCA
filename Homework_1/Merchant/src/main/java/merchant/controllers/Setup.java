package merchant.controllers;

import com.google.gson.Gson;
import crypto.Asymmetric;
import crypto.Hybrid;
import crypto.Symmetric;
import merchant.entities.MerchantSessionsEntity;
import merchant.repos.MerchantSessionsRepo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Controller
@RequestMapping("/setup")
public class Setup {
    private static final String[] myKeyPath = {"keys", "mprivkey.pem"};
    private PrivateKey myPrivateKey;
    @Autowired
    private MerchantSessionsRepo merchantSessionsRepo;

    @PostMapping
    @ResponseBody
    public String initiateSetup(@RequestBody String jsonBody) throws Exception {
        var gson = new Gson();
        byte[][] keys = gson.fromJson(jsonBody, byte[][].class);
        PublicKey clientKey = getKey(keys);
        String publicKey = Base64.getEncoder().encodeToString(clientKey.getEncoded());
        System.out.println(publicKey);
        MerchantSessionsEntity merchantSession = new MerchantSessionsEntity();
        merchantSession.setPkc(publicKey);
        merchantSession = merchantSessionsRepo.save(merchantSession);
        System.out.println(merchantSession.getId());
        byte[][] res = new byte[2][];
        res[0] = ByteBuffer.allocate(4).putInt(merchantSession.getId()).array();
        res[1] = Asymmetric.signData(res[0], myPrivateKey);
        return gson.toJson(Hybrid.encryptData(res, clientKey, Symmetric.getKey()));
    }

    public Setup() throws Exception {
        myPrivateKey = getMyPrivateKey();
    }

    private PublicKey getKey(byte[][] keys) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Hybrid.decryptData(keys, myPrivateKey)[0])
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        return new IvParameterSpec(iv);
    }

    private RSAPrivateKey getMyPrivateKey() throws Exception {
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
