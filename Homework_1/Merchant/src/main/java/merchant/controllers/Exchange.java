package merchant.controllers;

import com.google.gson.Gson;
import crypto.Asymmetric;
import crypto.Hybrid;
import crypto.Symmetric;
import merchant.entities.MerchantSessionsEntity;
import merchant.repos.MerchantSessionsRepo;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;

@Controller
@RequestMapping("/exchange")
public class Exchange {
    private final PrivateKey myPrivateKey;
    private static final String pgAddress = "http://127.0.0.1:9002";
    private int sid, nounce;
    private Double amount;
    private String orderDescription;
    private PublicKey clientPublicKey, myPublicKey, pgPublicKey;

    @Autowired
    private MerchantSessionsRepo merchantSessionsRepo;


    @PostMapping
    @ResponseBody
    public String doExchange(@RequestBody String jsonBody) throws Exception {
        var gson = new Gson();
        var data = gson.fromJson(jsonBody, byte[][].class);
        data = Hybrid.decryptData(data, myPrivateKey);
        var po = new byte[5][];
        var pm = new byte[10][];
        for(int i = 0; i < pm.length; i++){
            pm[i] = data[i];
        }
        for(int i = 0; i < po.length; i++){
            po[i] = data[i + pm.length];
        }
        try {
            var toSend = getMessageForPaymentGateway(pm, po);
            var body = gson.toJson(toSend);
            var client = HttpClient.newBuilder().build();
            var request = HttpRequest.newBuilder(URI.create(pgAddress + "/exchange"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            byte[][] res = gson.fromJson(response.body(), byte[][].class);
            res = Hybrid.encryptData(Hybrid.decryptData(res, myPrivateKey), clientPublicKey, Symmetric.getKey());
            // System.out.println("Stop me now");
            // Thread.sleep(10000);
            return gson.toJson(res);
        } catch(SignatureException e) {
            System.out.println("Oh no.. It wasn't the client that signed these things....");
        } catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("How did he get here? Didnt do the setup..");
        }
        return null;
    }

    public Exchange() throws Exception {
        myPrivateKey = getMyPrivateKey();
        myPublicKey = getPublicKey("mcert.pem");
        pgPublicKey = getPublicKey("pgcert.pem");
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

    private byte[][] getMessageForPaymentGateway(byte[][] pm, byte[][] po) throws Exception {
        orderDescription = new String(po[0], StandardCharsets.UTF_8);
        sid = ByteBuffer.wrap(po[1]).getInt();
        amount = ByteBuffer.wrap(po[2]).getDouble();
        nounce = ByteBuffer.wrap(po[3]).getInt();

        MerchantSessionsEntity merchantSession = merchantSessionsRepo.findById(sid).orElse(null);
        if(merchantSession == null){
            System.out.println("Oh no");
            throw new Exception();
        }
        System.out.println(merchantSession.getId());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int i = 0; i < po.length - 1; i++) {
            os.write(po[i]);
        }
        clientPublicKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(
                        Base64.decodeBase64(merchantSession.getPkc())));
        Asymmetric.verifySignedData(os.toByteArray(), po[po.length-1], clientPublicKey);
        merchantSession.setAmount(amount);
        merchantSession.setNonce(nounce);
        merchantSession.setOrderDesc(orderDescription);
        merchantSessionsRepo.save(merchantSession);
        os.reset();
        os.write(po[1]);
        os.write(clientPublicKey.getEncoded());
        os.write(po[2]);
        var toSend = new byte[pm.length + 1][];
        for(int i = 0; i < pm.length; i++){
            toSend[i] = pm[i];
        }
        toSend[pm.length] = Asymmetric.signData(os.toByteArray(), myPrivateKey);
        return Hybrid.encryptData(toSend, pgPublicKey, Symmetric.getKey());
    }
}
