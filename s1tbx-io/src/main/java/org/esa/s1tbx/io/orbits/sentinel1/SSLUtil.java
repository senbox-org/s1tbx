package org.esa.s1tbx.io.orbits.sentinel1;

import org.esa.snap.core.util.SystemUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Created by lveci on 3/28/2017.
 */
public class SSLUtil {

    private HostnameVerifier hostnameVerifier;

    public void disableSSLCertificateCheck() {
        hostnameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier(){

                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        if (hostname.equals("qc.sentinel1.eo.esa.int")) {
                            return true;
                        }
                        return false;
                    }
                });

        final TrustManager[] trustManager = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManager, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            SystemUtils.LOG.warning("disableSSLCertificateCheck failed: " + e);
        }
    }

    public void enableSSLCertificateCheck() {
        try {
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            SystemUtils.LOG.warning("enableSSLCertificateCheck failed: " + e);
        }
    }
}
