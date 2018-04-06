package com.example.SSL;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SSLConfig {

	//private static final Logger logger = LoggerFactory.getLogger(SSLConfig.class);

	@Value(value = "${keystore.location}")
	private String keystoreLocation;

	@Value(value = "${keystore.password}")
	private String keystorePassword;

	@Value(value = "${key.alias}")
	private String keyAlias;

	@Value(value = "${key.password}")
	private String keyPassword;

	@Value(value = "${truststore.location}")
	private String truststoreLocation;

	@Value(value = "${truststore.password}")
	private String truststorePassword;

	@Bean
	public RestTemplate restTemplate() {
		System.out.println("SSLConfig");
		return new RestTemplate(clientHttpRequestFactory(keyAlias));
	}

	@Bean
	public CloseableHttpClient httpClient() {
		return createHttpClient(keyAlias);
	}

	private boolean loadKeyAndTrustStore(KeyStore keyStore, KeyStore trustStore, InputStream keystoreStream,
			InputStream truststoreStream) throws IOException {
		try {
			keyStore.load(keystoreStream, keystorePassword.toCharArray());
			trustStore.load(truststoreStream, truststorePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			//logger.error("Error while loading the key store. ", e);
			System.out.println("Error while loading the key store: " + e.toString());
			return true;
		} 
		return false;
	}

	private SSLContext sslContext(final String alias) {

		SSLContext sslcontext = null;

		try {
			KeyStore keyStore;
			KeyStore trustStore;

			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

			try (InputStream keystoreStream = this.getClass().getResourceAsStream(keystoreLocation);
					InputStream truststoreStream = this.getClass().getResourceAsStream(truststoreLocation)) {

				final boolean threwError = loadKeyAndTrustStore(keyStore, trustStore, keystoreStream, truststoreStream);
				if (threwError) {
					System.out.println("loadKeyAndTrustStore: threw an error");
					return null;
				}

				final PrivateKeyStrategy aliasStrategy = (aliases, socket) -> alias;

				sslcontext = SSLContexts.custom().useProtocol("TLS")
						.loadKeyMaterial(keyStore, keystorePassword.toCharArray(), aliasStrategy)
						.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
			}
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
				| IOException e) {
			
			//logger.error("Error while building SSL Context", e);
			System.out.println("Error while building SSL Context: " + e.toString());
			throw new RuntimeException(e.getMessage());
			
		}
		return sslcontext;
	}

	private CloseableHttpClient createHttpClient(String alias) {

		SSLContext sslcontext = sslContext(alias);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
		if (sslcontext != null) {
			socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
							SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
					.build();
		} else {
			socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
		}

		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		return HttpClients.custom().setConnectionManager(connManager).disableAutomaticRetries()
				.disableRedirectHandling().build();
	}

	public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(String alias) {
		return new HttpComponentsClientHttpRequestFactory(createHttpClient(alias));

	}
}