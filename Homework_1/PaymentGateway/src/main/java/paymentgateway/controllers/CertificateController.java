package homework_1.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pg.PaymentGateway;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@RestController
@RequestMapping("/pg")
public class CertificateController {
    @GetMapping("/pk")
    public Certificate getPublicKey() throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        Certificate certificate = new PaymentGateway().getCertificate();
        return certificate;
    }

    @GetMapping
    public String defaulty() {
        return "mama dumneavoastra";
    }
}
