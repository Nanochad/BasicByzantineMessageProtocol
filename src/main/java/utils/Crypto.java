package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Properties;

public final class Crypto {

    public static final String CRYPTO_NAME_KEY = "crypto_name";
    public static final String KEY_STORE_LOCATION_KEY = "key_store_folder";
    public static final String KEY_STORE_PASSWORD_KEY = "key_store_password";
    public static final String TRUST_STORE_LOCATION_KEY = "trust_store";
    public static final String TRUST_STORE_PASSWORD_KEY = "trust_store_password";

    public static KeyPair getKeyPair(Properties props) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {

        String alias = Objects.requireNonNull(props.getProperty(CRYPTO_NAME_KEY), CRYPTO_NAME_KEY + " must be defined");

        String keyStoreLocation = props.getProperty(KEY_STORE_LOCATION_KEY) + "/" + alias + ".ks";
        char[] keyStorePassword = props.getProperty(KEY_STORE_PASSWORD_KEY).toCharArray();

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream fis = new FileInputStream(keyStoreLocation)) {
            keyStore.load(fis, keyStorePassword);
        }

        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyStorePassword);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, protParam);

        if (keyEntry == null) {
            throw new IllegalArgumentException("No private key entry found for alias: " + alias);
        }

        PrivateKey privateKey = keyEntry.getPrivateKey();
        Certificate cert = keyEntry.getCertificate();
        PublicKey publicKey = cert.getPublicKey();

        return new KeyPair(publicKey, privateKey);
    }

    public static KeyStore getTruststore(Properties props)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        String trustStoreLocation = props.getProperty(TRUST_STORE_LOCATION_KEY);
        char[] trustStorePassword = props.getProperty(TRUST_STORE_PASSWORD_KEY).toCharArray();

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream fis = new FileInputStream(trustStoreLocation)) {
            ks.load(fis, trustStorePassword);
        }
        return ks;
    }
}
