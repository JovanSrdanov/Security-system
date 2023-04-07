package pkibackend.pkibackend.keystore;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class KeyStoreWriter {
    private final KeyStore keyStore;

    public KeyStoreWriter() {
        try {
            keyStore = KeyStore.getInstance("JKS", "SUN");
        } catch (KeyStoreException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadKeyStore(String fileName, char[] password) {
//        try {
//            if(fileName != null) {
//                keyStore.load(new FileInputStream(fileName), password);
//            } else {
//                //Ako je cilj kreirati novi KeyStore poziva se i dalje load, pri cemu je prvi parametar null
//                keyStore.load(null, password);
//            }
//        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
//            throw new RuntimeException(e);
//        }
        // TODO Stefan: promeni
        try {
            FileInputStream fis = new FileInputStream(fileName);
            keyStore.load(fis, password);
        } catch (FileNotFoundException e) {
            try {
                keyStore.load(null, password);
            } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
                throw new RuntimeException(ex);
            }
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveKeyStore(String fileName, char[] password) {
        try {
            keyStore.store(new FileOutputStream(fileName), password);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String alias, PrivateKey privateKey, char[] password, java.security.cert.Certificate certificate) {
        try {
            keyStore.setKeyEntry(alias, privateKey, password, new Certificate[] {certificate});
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
