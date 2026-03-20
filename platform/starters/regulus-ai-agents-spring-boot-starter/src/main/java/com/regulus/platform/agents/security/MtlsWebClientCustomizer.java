package com.regulus.platform.agents.security;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Customizer for WebClient to enable mTLS.
 * Configures SSL context with client certificates and trust stores.
 */
public class MtlsWebClientCustomizer {

    private static final Logger log = LoggerFactory.getLogger(MtlsWebClientCustomizer.class);

    private final SecurityProperties.MtlsConfig config;

    public MtlsWebClientCustomizer(SecurityProperties.MtlsConfig config) {
        this.config = config;
        log.info("mTLS WebClient customizer initialized");
    }

    /**
     * Create a WebClient builder with mTLS configuration.
     */
    public WebClient.Builder createBuilder() {
        try {
            SslContext sslContext = buildSslContext();

            HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContext));

            ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

            log.info("Created mTLS-enabled WebClient builder");
            return WebClient.builder().clientConnector(connector);

        } catch (Exception e) {
            log.error("Failed to create mTLS WebClient: {}", e.getMessage(), e);
            throw new SecurityException("mTLS configuration failed", e);
        }
    }

    private SslContext buildSslContext() throws Exception {
        SslContextBuilder builder = SslContextBuilder.forClient();

        // Configure trust store
        if (config.getTrustStorePath() != null) {
            TrustManagerFactory tmf = loadTrustManagerFactory();
            builder.trustManager(tmf);
            log.debug("Configured trust store: {}", config.getTrustStorePath());
        }

        // Configure key store (client certificate)
        if (config.getKeyStorePath() != null) {
            KeyManagerFactory kmf = loadKeyManagerFactory();
            builder.keyManager(kmf);
            log.debug("Configured key store: {}", config.getKeyStorePath());
        }

        return builder.build();
    }

    private TrustManagerFactory loadTrustManagerFactory() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(config.getTrustStoreType());

        try (FileInputStream fis = new FileInputStream(config.getTrustStorePath())) {
            char[] password = config.getTrustStorePassword() != null
                ? config.getTrustStorePassword().toCharArray()
                : null;
            trustStore.load(fis, password);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        return tmf;
    }

    private KeyManagerFactory loadKeyManagerFactory() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(config.getKeyStoreType());

        try (FileInputStream fis = new FileInputStream(config.getKeyStorePath())) {
            char[] password = config.getKeyStorePassword() != null
                ? config.getKeyStorePassword().toCharArray()
                : null;
            keyStore.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        char[] keyPassword = config.getKeyStorePassword() != null
            ? config.getKeyStorePassword().toCharArray()
            : null;
        kmf.init(keyStore, keyPassword);

        return kmf;
    }
}
