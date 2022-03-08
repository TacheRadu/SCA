package paymentgateway.controllers;

import com.google.gson.Gson;
import crypto.Asymmetric;
import crypto.Hybrid;
import crypto.Symmetric;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import paymentgateway.repos.PgEntriesRepo;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Controller
@RequestMapping("/resolution")
public class Resolution {
    private final PublicKey myPublicKey;
    private final PrivateKey myPrivateKey;
    private PublicKey clientKey;
    @Autowired
    private PgEntriesRepo pgEntriesRepo;

    @PostMapping
    @ResponseBody
    public String resolve(@RequestBody String jsonBody) throws Exception {
        var gson = new Gson();
        var body = gson.fromJson(jsonBody, byte[][].class);
        var req = Hybrid.decryptData(body, myPrivateKey);
        var os = new ByteArrayOutputStream();
        for(int i = 0; i < req.length - 1; i++){
            os.write(req[i]);
        }
        clientKey = getPublicKey(req[3]);
        try {
            Asymmetric.verifySignedData(os.toByteArray(), req[req.length - 1], clientKey);
            var sid = ByteBuffer.wrap(req[0]).getInt();
            var amount = ByteBuffer.wrap(req[1]).getDouble();
            var nonce = ByteBuffer.wrap(req[2]).getInt();
            var entry = pgEntriesRepo
                    .findByPk(sid, amount, nonce, Base64.getEncoder().encodeToString(clientKey.getEncoded()))
                    .orElse(null);
            var resp = "u good, bro";
            if (entry == null){
                resp = "sorry bro, no transaction here";
            }
            var res = new byte[3][];
            res[0] = resp.getBytes(StandardCharsets.UTF_8);
            res[1] = ByteBuffer.allocate(4).putInt(sid).array();
            os.reset();
            os.write(res[0]);
            os.write(res[1]);
            os.write(ByteBuffer.allocate(8).putDouble(amount).array());
            os.write(ByteBuffer.allocate(4).putInt(nonce).array());
            res[2] = Asymmetric.signData(os.toByteArray(), myPrivateKey);
            res = Hybrid.encryptData(res, clientKey, Symmetric.getKey());
            return new Gson().toJson(res);

        } catch (SignatureException e){
            System.out.println("The client is taped..");
        }
        return null;
    }
    public Resolution() throws Exception {
        myPrivateKey = getMyPrivateKey();
        myPublicKey = getPublicKey("pgcert.pem");
    }
    private RSAPrivateKey getMyPrivateKey() throws Exception {
        File file = new File(getClass().getClassLoader()
                .getResource(Paths.get("keys", "pgprivkey.pem").toString()).toURI());
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (FileReader keyReader = new FileReader(file);
             PemReader pemReader = new PemReader(keyReader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
        }
    }

    private PublicKey getPublicKey(String fromWho) throws FileNotFoundException, CertificateException, URISyntaxException {
        File certFile;
        certFile = new File(getClass().getClassLoader()
                .getResource(Paths.get("certs", fromWho).toString()).toURI());
        if (certFile.exists()) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new FileInputStream(certFile));
            return certificate.getPublicKey();
        }
        throw new CertificateException();
    }
    private PublicKey getPublicKey(byte[] pk) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pk));
    }
}
