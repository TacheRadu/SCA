package pg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class PaymentGateway {
    private static final String pgCertificateName = "certs/pgcert.pem";
    private static final Integer pkSize = 2048;

    public X509Certificate getCertificate() {
        return certificate;
    }

    private X509Certificate certificate;

    public PaymentGateway() throws CertificateException {
        loadCertificate();
    }

    private void loadCertificate() throws CertificateException {
        File certFile;
        try {
            certFile = new File(getClass().getClassLoader().getResource(pgCertificateName).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if (certFile.exists()) {
            try {
                certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new FileInputStream(certFile));
            } catch (CertificateException | FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            throw new CertificateException();
        }
    }
}
