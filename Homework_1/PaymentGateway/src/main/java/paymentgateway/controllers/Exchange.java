package paymentgateway.controllers;

import com.google.gson.Gson;
import crypto.Asymmetric;
import crypto.Hybrid;
import crypto.Symmetric;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import paymentgateway.entities.MerchantPksEntity;
import paymentgateway.entities.PgEntriesEntity;
import paymentgateway.repos.MerchantPksRepo;
import paymentgateway.repos.PgEntriesRepo;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

@Controller
@RequestMapping("/exchange")
public class Exchange {
    private final PublicKey myPublicKey;
    private final PrivateKey myPrivateKey;
    private PublicKey merchantPublicKey, cPublicKey;
    private int sid, nounce;
    private Double amount;
    private String merchant;
    @Autowired
    private PgEntriesRepo pgEntriesRepo;
    @Autowired
    private MerchantPksRepo merchantPksRepo;

    @PostMapping
    @ResponseBody
    public String doExchange(@RequestBody String jsonBody) throws Exception {
        var gson = new Gson();
        var body = gson.fromJson(jsonBody, byte[][].class);
        try {
            // decrypt pm, can't check if signature is valid yet
            var pmWithSign = Hybrid.decryptData(body, myPrivateKey);
            var pm = new byte[pmWithSign.length - 1][];
            for (int i = 0; i < pm.length; i++) {
                pm[i] = pmWithSign[i];
            }
            // get pi, we need some data from there to check the validity of the pm signature
            // while at it, verify the validity of the pi signature as well
            var pi = getPi(pm);
            sid = ByteBuffer.wrap(pi[3]).getInt();
            amount = ByteBuffer.wrap(pi[4]).getDouble();
            nounce = ByteBuffer.wrap(pi[6]).getInt();
            merchant = new String(pi[7], Charset.defaultCharset());
            // now we have what we need to get the merchant's public key from the db
            merchantPublicKey = getMerchantPublicKey(merchant);
            verifyPm(pmWithSign[pmWithSign.length - 1], pi);
            saveData();
            return reply();
        } catch (SignatureException e) {
            System.out.println("Somewhere under the rainbow something was not valid");
        }

        return null;
    }

    public Exchange() throws Exception {
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

    private byte[][] getPi(byte[][] pm) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        var piWithSign = Hybrid.decryptData(pm, myPrivateKey);
        var pi = new byte[piWithSign.length - 1][];
        for (int i = 0; i < piWithSign.length - 1; i++) {
            os.write(piWithSign[i]);
            pi[i] = piWithSign[i];
        }
        cPublicKey = getPublicKey(piWithSign[5]);
        Asymmetric.verifySignedData(os.toByteArray(), piWithSign[piWithSign.length - 1], cPublicKey);
        return pi;
    }

    private PublicKey getMerchantPublicKey(String merchant) throws Exception {
        MerchantPksEntity merchantEntity = merchantPksRepo.findById(merchant).orElse(null);
        if (merchantEntity == null) {
            throw new Exception();
        }
        return getPublicKey(Base64.decodeBase64(merchantEntity.getPkm()));
    }

    private void verifyPm(byte[] signedData, byte[][] pi) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(pi[3]);
        os.write(pi[5]);
        os.write(pi[4]);
        Asymmetric.verifySignedData(os.toByteArray(), signedData, merchantPublicKey);
    }
    private void saveData(){
        PgEntriesEntity newEntity = new PgEntriesEntity();
        newEntity.setAmount(amount);
        newEntity.setMerchant(merchant);
        newEntity.setNounce(nounce);
        newEntity.setPkc(java.util.Base64.getEncoder().encodeToString(cPublicKey.getEncoded()));
        newEntity.setSid(sid);
        pgEntriesRepo.save(newEntity);
    }
    private String reply() throws Exception {
        var resp = "u good, bro";
        var res = new byte[3][];
        res[0] = resp.getBytes(StandardCharsets.UTF_8);
        res[1] = ByteBuffer.allocate(4).putInt(sid).array();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(res[0]);
        os.write(res[1]);
        os.write(ByteBuffer.allocate(8).putDouble(amount).array());
        os.write(ByteBuffer.allocate(4).putInt(nounce).array());
        res[2] = Asymmetric.signData(os.toByteArray(), myPrivateKey);
        res = Hybrid.encryptData(res, merchantPublicKey, Symmetric.getKey());
        return new Gson().toJson(res);
    }
}
