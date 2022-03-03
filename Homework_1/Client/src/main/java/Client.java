import com.google.gson.Gson;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Client {
    private static final String merchantPort = "9000";
    private final KeyPair kp;
    private String merchant;
    private final SecretKey secretKey;
    private final PublicKey merchantPublicKey;
    private Integer sid;

    public static void main(String[] args) throws Exception {
        Client myClient = new Client();
        myClient.buyFrom("http://127.0.0.1:" + merchantPort);
        myClient.setup();
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        return new IvParameterSpec(iv);
    }

    public Client() throws NoSuchAlgorithmException, FileNotFoundException, CertificateException, URISyntaxException {
        kp = getKeys();
        secretKey = getKey();
        merchantPublicKey = getMerchantPublicKey();
    }

    public void setup() throws Exception {
        if (merchant == null) {
            throw new Exception("Set up the merchant first!!!");
        }
        SealedObject[] encryptedData = encryptData(kp.getPublic());
        var body = new Gson().toJson(encryptedData);
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(merchant))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());

    }

    public void buyFrom(String address) {
        merchant = address;
    }

    private SealedObject[] encryptData(Serializable data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, generateIv());
        SealedObject encryptedData = new SealedObject(data, cipher);
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, merchantPublicKey);
        SealedObject encryptedSecret = new SealedObject(secretKey, cipher);
        return new SealedObject[]{encryptedData, encryptedSecret};
    }

    private PublicKey getMerchantPublicKey() throws FileNotFoundException, CertificateException, URISyntaxException {
        File certFile;
        certFile = new File(getClass().getClassLoader()
                .getResource(Paths.get("certs", "mcert.pem").toString()).toURI());
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

    private SecretKey getKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        return keyGen.generateKey();
    }
}
