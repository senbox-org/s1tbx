package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileAttributes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Response Handler for Amazon AWS S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3ResponseHandler extends DefaultHandler {

    private static final String KEY = "Key";
    private static final String SIZE = "Size";
    private static final String CONTENTS = "Contents";
    private static final String LAST_MODIFIED = "LastModified";
    private static final String NEXT_CONTINUATION_TOKEN = "NextContinuationToken";
    private static final String IS_TRUNCATED = "IsTruncated";
    private static final String COMMON_PREFIXES = "CommonPrefixes";
    private static final String PREFIX = "Prefix";

    private LinkedList<String> elementStack = new LinkedList<>();
    private List<BasicFileAttributes> items;

    private String key;
    private long size;
    private String lastModified;
    private String nextContinuationToken;
    private boolean isTruncated;
    private String prefix;

    S3ResponseHandler(String prefix, List<BasicFileAttributes> items) {
        this.prefix = prefix;
        this.items = items;
    }

    private static String getAuthorizationToken(String awsAccessKeyId, String awsSecretAccessKey) {//not real S3 authentication - only for function definition
        return (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() && awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) ? Base64.getEncoder().encodeToString(("" + awsAccessKeyId + ":" + awsSecretAccessKey + "").getBytes()) : "";
    }

    static URLConnection getConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        String authorizationToken = getAuthorizationToken(S3FileSystemProvider.getAwsAccessKeyId(), S3FileSystemProvider.getAwsSecretAccessKey());
        HttpURLConnection connection;
        if (url.getProtocol().equals("https")) {
            connection = (HttpsURLConnection) url.openConnection();
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        method = method.toUpperCase();
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        if (authorizationToken != null && !authorizationToken.isEmpty())
            connection.setRequestProperty("authorization", "Basic " + authorizationToken);
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestProperties_set = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestProperties_set) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        connection.setRequestProperty("user-agent", "SNAP Virtual File System");
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            /* error from server */
            throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
        } else {
            return connection;
        }
    }

    private static String getTextValue(char[] ch, int start, int length) {
        return new String(ch, start, length).trim();
    }

    String getNextContinuationToken() {
        return nextContinuationToken;
    }

//    public static String getAuthorizationToken(String awsAccessKeyId, String awsSecretAccessKey, String date, String aws_region) {
//        String authToken = "";
//        if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() && awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {
//            String date=new SimpleDateFormat("YYYYMMDD").format(new Date());
//            authToken += "AWS4-HMAC-SHA256\nCredential=" + awsAccessKeyId + "/" + date + "/" + aws_region + "/s3/aws4_request";
//        }
//        return authToken;
//    }

    boolean getIsTruncated() {
        return isTruncated;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        try {
            String currentElement = localName.intern();
            elementStack.addLast(currentElement);
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            String currentElement = elementStack.removeLast();
            assert currentElement != null && currentElement.equals(localName);
            prefix = prefix.isEmpty() ? S3FileSystemProvider.S3_ROOT : prefix;
            if (currentElement.equals(PREFIX) && elementStack.size() == 2 && elementStack.get(1).equals(COMMON_PREFIXES)) {
                items.add(ObjectStorageFileAttributes.newDir(prefix + key));
            } else if (currentElement.equals(CONTENTS) && elementStack.size() == 1) {
                items.add(ObjectStorageFileAttributes.newFile(prefix + key, size, lastModified));
            }
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            String currentElement = elementStack.getLast();
            switch (currentElement) {
                case KEY:
                    key = getTextValue(ch, start, length);
                    String[] key_parts = key.split(new S3FileSystemProvider().getDelimiter());
                    key = key.endsWith(new S3FileSystemProvider().getDelimiter()) ? key_parts[key_parts.length - 1] + new S3FileSystemProvider().getDelimiter() : key_parts[key_parts.length - 1];
                    break;
                case SIZE:
                    size = getLongValue(ch, start, length);
                    break;
                case LAST_MODIFIED:
                    lastModified = getTextValue(ch, start, length);
                    break;
                case IS_TRUNCATED:
                    isTruncated = getBooleanValue(ch, start, length);
                    break;
                case NEXT_CONTINUATION_TOKEN:
                    nextContinuationToken = getTextValue(ch, start, length);
                    break;
                case PREFIX:
                    key = getTextValue(ch, start, length);
                    key_parts = key.split(new S3FileSystemProvider().getDelimiter());
                    key = key.endsWith(new S3FileSystemProvider().getDelimiter()) ? key_parts[key_parts.length - 1] + new S3FileSystemProvider().getDelimiter() : key_parts[key_parts.length - 1];
                    break;
            }
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    private boolean getBooleanValue(char[] ch, int start, int length) {
        return Boolean.parseBoolean(getTextValue(ch, start, length));
    }

    private long getLongValue(char[] ch, int start, int length) {
        return Long.parseLong(getTextValue(ch, start, length));
    }

}
