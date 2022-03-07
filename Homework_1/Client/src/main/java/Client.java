import com.google.gson.Gson;
import crypto.Asymmetric;
import crypto.Hybrid;
import crypto.Symmetric;

import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    private static final String merchantPort = "9000";
    private final KeyPair kp;
    private String merchant;
    private final PublicKey merchantPublicKey;
    private final PublicKey pgPublicKey;
    private Integer amount;
    private Integer sid;
    private Integer nounce;

    public static void main(String[] args) throws Exception {
        Client myClient = new Client();
        myClient.buyFrom("http://127.0.0.1:" + merchantPort);
        try {
            myClient.setup();
            myClient.exchange();
        } catch (SignatureException e) {
            System.out.println("Oh no, somebody tries to mess with us..");
        } catch (HttpTimeoutException e) {
            System.out.println("That damn merchant..");
        }
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        return new IvParameterSpec(iv);
    }

    public Client() throws NoSuchAlgorithmException, FileNotFoundException, CertificateException, URISyntaxException {
        kp = getKeys();
        merchantPublicKey = getPublicKey("mcert.pem");
        pgPublicKey = getPublicKey("pgcert.pem");
    }

    public void setup() throws Exception {
        if (merchant == null) {
            throw new Exception("Set up the merchant first!!!");
        }
        byte[][] encryptedData = Hybrid.encryptData(new byte[][]{kp.getPublic().getEncoded()}, merchantPublicKey, Symmetric.getKey());
        var gson = new Gson();
        var body = gson.toJson(encryptedData);
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(merchant + "/setup"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        byte[][] res = gson.fromJson(response.body(), byte[][].class);
        res = Hybrid.decryptData(res, kp.getPrivate());
        Asymmetric.verifySignedData(res[0], res[1], merchantPublicKey);
        sid = ByteBuffer.wrap(res[0]).getInt();
    }

    public void exchange() throws Exception {
        var po = getPO();
        var pm = getPM(getPI());
        var toSend = new byte[po.length + pm.length + 1][];
        for(int i = 0; i < pm.length; i++){
            toSend[i] = pm[i];
        }
        for(int i = 0; i < po.length; i++){
            toSend[pm.length + i] = po[i];
        }
        toSend = Hybrid.encryptData(toSend, merchantPublicKey, Symmetric.getKey());

        var gson = new Gson();
        var body = gson.toJson(toSend);
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        var request = HttpRequest.newBuilder(URI.create(merchant + "/setup"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void buyFrom(String address) {
        merchant = address;
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

    private KeyPair getKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }
    private byte[][] getPO() throws Exception {
        byte[][] res = new byte[5][];
        String orderDescription = "a very generic order description";
        amount = ThreadLocalRandom.current().nextInt();
        nounce = ThreadLocalRandom.current().nextInt();
        res[0] = orderDescription.getBytes(StandardCharsets.UTF_8);
        res[1] = ByteBuffer.allocate(4).putInt(sid).array();
        res[2] = ByteBuffer.allocate(4).putInt(amount).array();
        res[3] = ByteBuffer.allocate(4).putInt(nounce).array();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int i = 0; i < 4; i++) {
            os.write(res[i]);
        }
        res[4] = Asymmetric.signData(os.toByteArray(), kp.getPrivate());
        System.out.println(res.length);
        return res;
    }

    private byte[][] getPI() {
        byte[][] res = new byte[8][];
        Long cardNumber = ThreadLocalRandom.current().nextLong();
        LocalDate cardExpDate = LocalDate.now().plusYears(5);
        Integer cvc = ThreadLocalRandom.current().nextInt(900) + 100;
        res[0] = ByteBuffer.allocate(8).putLong(cardNumber).array();
        res[1] = cardExpDate.toString().getBytes(StandardCharsets.UTF_8);
        res[2] = ByteBuffer.allocate(4).putInt(cvc).array();
        res[3] = ByteBuffer.allocate(4).putInt(sid).array();
        res[4] = ByteBuffer.allocate(4).putInt(amount).array();
        res[5] = kp.getPublic().getEncoded();
        res[6] = ByteBuffer.allocate(4).putInt(nounce).array();
        res[7] = "a very generic merchant".getBytes(StandardCharsets.UTF_8);
        return res;
    }
    private byte[][] getPM(byte[][] pi) throws Exception {
        byte[][] res = new byte[pi.length + 1][];
        for(int i = 0; i < pi.length; i++){
            res[i] = pi[i];
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int i = 0; i < pi.length; i++) {
            os.write(res[i]);
        }
        res[pi.length] = Asymmetric.signData(os.toByteArray(), kp.getPrivate());
        res = Hybrid.encryptData(res, pgPublicKey, Symmetric.getKey());
        return res;
    }
}
