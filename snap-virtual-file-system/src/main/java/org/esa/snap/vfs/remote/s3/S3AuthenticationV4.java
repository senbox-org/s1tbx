package org.esa.snap.vfs.remote.s3;

import org.apache.commons.codec.binary.Hex;
import org.esa.snap.vfs.preferences.model.Property;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S3AuthenticationV4 class for S3 VFS.
 * Provides implementation of AWS Signature Version 4.
 * @see  <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">AWS Signature Version 4</a>
 *
 * @author Adrian Drăghici
 */
class S3AuthenticationV4 {

    /**
     * The ISO date-time format used.
     */
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    /**
     * The ISO date format used.
     */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final long SIGNING_KEY_VALIDITY = 14L;

    private static final String HTTP_VERB_NAME = "<HTTPMethod>";
    private static final String CANONICAL_URI_NAME = "<CanonicalURI>";
    private static final String CANONICAL_QUERY_STRING_NAME = "<CanonicalQueryString>";
    private static final String CANONICAL_HEADERS_NAME = "<CanonicalHeaders>";
    private static final String SIGNED_HEADERS_NAME = "<SignedHeaders>";
    private static final String HASHED_PAYLOAD_NAME = "<HashedPayload>";
    private static final String CANONICAL_REQUEST_HASH_NAME = "<CanonicalRequest>";
    private static final String CANONICAL_REQUEST_VALUE = "" + HTTP_VERB_NAME + "\n" + CANONICAL_URI_NAME + "\n" + CANONICAL_QUERY_STRING_NAME + "\n" + CANONICAL_HEADERS_NAME + "\n" + SIGNED_HEADERS_NAME + "\n" + HASHED_PAYLOAD_NAME;
    private static final String AWS_SIGNATURE_ALGORITHM_NAME = "<Algorithm>";
    private static final String AWS_SIGNATURE_ALGORITHM_VALUE = "AWS4-HMAC-SHA256";
    private static final String TIMESTAMP_NAME = "<TimeStamp>";
    private static final String SCOPE_NAME = "<Scope>";
    private static final String DATE_STAMP_NAME = "<DateStamp>";
    private static final String AWS_REGION_NAME = "<AWSRegion>";
    private static final String AWS_SERVICE_VALUE = "s3";
    private static final String SCOPE_VALUE = DATE_STAMP_NAME + "/" + AWS_REGION_NAME + "/" + AWS_SERVICE_VALUE + "/aws4_request";
    private static final String STRING_TO_SIGN_VALUE = AWS_SIGNATURE_ALGORITHM_NAME + "\n" + TIMESTAMP_NAME + "\n" + SCOPE_NAME + "\n" + CANONICAL_REQUEST_HASH_NAME;
    private static final String SIGNING_KEY_DATA_VALUE = "aws4_request";
    private static final String AWS_HOST_HEADER_NAME = "host";
    private static final String AWS_CONTENT_SHA256_HEADER_NAME = "x-amz-content-sha256";
    private static final String AWS_CONTENT_SHA256_HEADER_VALUE = "UNSIGNED-PAYLOAD";
    private static final String AWS_DATE_HEADER_NAME = "x-amz-date";
    private static final String AWS_ACCESS_KEY_ID_NAME = "<Credential>";
    private static final String AWS_SIGNATURE_NAME = "<Signature>";
    private static final String AWS_AUTHORIZATION_TOKEN_VALUE = AWS_SIGNATURE_ALGORITHM_NAME + " Credential=" + AWS_ACCESS_KEY_ID_NAME + "/" + SCOPE_NAME + ", SignedHeaders=" + SIGNED_HEADERS_NAME + ", Signature=" + AWS_SIGNATURE_NAME;

    private final String httpVerb;
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;

    private LocalDateTime creationDate;
    private LocalDateTime expirationDate = null;
    private Map<String, String> awsHeaders;
    private byte[] signingKey;

    /**
     * Initializes the S3AuthenticationV4 class.
     *
     * @param httpVerb        The HTTP verb
     * @param region          The region
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     */
    S3AuthenticationV4(String httpVerb, String region, String accessKeyId, String secretAccessKey) {
        this.httpVerb = httpVerb;
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.creationDate = LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String lowercase(String value) {
        String lowercase = "";
        if (value != null) {
            lowercase = value.toLowerCase();
        }
        return lowercase;
    }

    private static String trim(String value) {
        String trim = "";
        if (value != null) {
            trim = value.trim();
        }
        return trim;
    }

    private static String hex(byte[] data) {
        String hex = "";
        if (data != null) {
            hex = String.valueOf(Hex.encodeHex(data));
        }
        return hex;
    }

    private static byte[] sha256Hash(byte[] data) {
        byte[] sha256Hash = new byte[0];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            sha256Hash = digest.digest(data);
        } catch (Exception ignored) {
            //
        }
        return sha256Hash;
    }

    private static byte[] hmacSHA256Hash(byte[] data, byte[] secretKey) {
        byte[] hmacSHA256Hash = new byte[0];
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            hmacSHA256Hash = mac.doFinal(data);
        } catch (Exception ignored) {
            //
        }
        return hmacSHA256Hash;
    }

    private static String uriEncode(String input, boolean encodeSlash) {
        StringBuilder result = new StringBuilder();
        try {
            for (int i = 0; i < input.length(); i++) {
                char ch = input.charAt(i);
                if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '~' || ch == '.' || ch == '%') {
                    result.append(ch);
                } else if (ch == '/') {
                    result.append(encodeSlash ? "%2F" : ch);
                } else {
                    result.append(URLEncoder.encode(String.valueOf(ch), "UTF8"));
                }
            }
        } catch (Exception ignored) {
            //
        }
        return result.toString();
    }

    private static List<Property> getRequestParametersList(String query) {
        List<Property> requestParameters = new ArrayList<>();
        if (query != null && !query.isEmpty()) {
            query = query.endsWith("&") ? query : query + "&";
            String requestParametersString = query.replaceAll("(.*?)=(.*?)&", "$1=$2\n");
            String[] requestParametersArray = requestParametersString.split("\n");
            for (String requestParameter : requestParametersArray) {
                String requestParameterName = requestParameter.replaceAll("(.*)=(.*)", "$1");
                String requestParameterValue = requestParameter.replaceAll("(.*)=(.*)", "$2");
                requestParameters.add(new Property(requestParameterName, requestParameterValue));
            }
            requestParameters.sort(new PropertyCodePointSorter());
        }
        return requestParameters;
    }

    private Map<String, String> buildAwsHeaders(URL url) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(AWS_HOST_HEADER_NAME, url.getHost());
        headers.put(AWS_CONTENT_SHA256_HEADER_NAME, AWS_CONTENT_SHA256_HEADER_VALUE);
        headers.put(AWS_DATE_HEADER_NAME, getISODateTime());
        return headers;
    }

    private String buildCanonicalQueryString(List<Property> requestParameters) {
        StringBuilder canonicalQueryString = new StringBuilder();
        boolean building = false;

        for (Property requestParameter : requestParameters) {
            if (building) {
                canonicalQueryString.append("&");
            } else {
                building = true;
            }
            canonicalQueryString.append(uriEncode(requestParameter.getName(), true));
            canonicalQueryString.append("=");
            canonicalQueryString.append(uriEncode(requestParameter.getValue(), true));
        }
        return canonicalQueryString.toString();
    }

    private String buildCanonicalHeaders() {
        StringBuilder canonicalHeaders = new StringBuilder();
        Set<Map.Entry<String, String>> awsHeadersSet = this.awsHeaders.entrySet();
        for (Map.Entry<String, String> awsHeader : awsHeadersSet) {
            canonicalHeaders.append(lowercase(awsHeader.getKey()));
            canonicalHeaders.append(':');
            canonicalHeaders.append(trim(awsHeader.getValue()));
            canonicalHeaders.append('\n');
        }
        return canonicalHeaders.toString();
    }

    private String buildSignedHeaders() {
        StringBuilder signedHeaders = new StringBuilder();
        Set<Map.Entry<String, String>> awsHeadersSet = this.awsHeaders.entrySet();
        boolean building = false;
        for (Map.Entry<String, String> awsHeader : awsHeadersSet) {
            if (building) {
                signedHeaders.append(";");
            } else {
                building = true;
            }
            signedHeaders.append(lowercase(awsHeader.getKey()));
        }
        return signedHeaders.toString();
    }

    private String buildHashedPayload() {
        return AWS_CONTENT_SHA256_HEADER_VALUE;
    }

    private String buildScope() {
        String scope = SCOPE_VALUE;
        scope = scope.replace(DATE_STAMP_NAME, getISODate());
        scope = scope.replace(AWS_REGION_NAME, this.region);
        return scope;
    }

    private String buildCanonicalRequestHash(String canonicalRequest) {
        byte[] canonicalRequestHash = sha256Hash(canonicalRequest.getBytes());
        return hex(canonicalRequestHash);
    }

    private String buildCanonicalRequest(URL url) {
        String canonicalURI = uriEncode(url.getPath(), false);
        String canonicalQueryString = buildCanonicalQueryString(getRequestParametersList(url.getQuery()));
        String canonicalHeaders = buildCanonicalHeaders();
        String signedHeaders = buildSignedHeaders();
        String hashedPayload = buildHashedPayload();
        String canonicalRequest = CANONICAL_REQUEST_VALUE;
        canonicalRequest = canonicalRequest.replace(HTTP_VERB_NAME, httpVerb);
        canonicalRequest = canonicalRequest.replace(CANONICAL_URI_NAME, canonicalURI);
        canonicalRequest = canonicalRequest.replace(CANONICAL_QUERY_STRING_NAME, canonicalQueryString);
        canonicalRequest = canonicalRequest.replace(CANONICAL_HEADERS_NAME, canonicalHeaders);
        canonicalRequest = canonicalRequest.replace(SIGNED_HEADERS_NAME, signedHeaders);
        canonicalRequest = canonicalRequest.replace(HASHED_PAYLOAD_NAME, hashedPayload);
        return canonicalRequest;
    }

    private String buildStringToSign(String canonicalRequest) {
        String timeStamp = getISODateTime();
        String scope = buildScope();
        String canonicalRequestHash = buildCanonicalRequestHash(canonicalRequest);
        String stringToSign = STRING_TO_SIGN_VALUE;
        stringToSign = stringToSign.replace(AWS_SIGNATURE_ALGORITHM_NAME, AWS_SIGNATURE_ALGORITHM_VALUE);
        stringToSign = stringToSign.replace(TIMESTAMP_NAME, timeStamp);
        stringToSign = stringToSign.replace(SCOPE_NAME, scope);
        stringToSign = stringToSign.replace(CANONICAL_REQUEST_HASH_NAME, canonicalRequestHash);
        return stringToSign;
    }

    private byte[] buildSigningKey() {
        String secretKey = "AWS4" + this.secretAccessKey;
        byte[] dateKey = hmacSHA256Hash(getISODate().getBytes(), secretKey.getBytes());
        byte[] regionKey = hmacSHA256Hash(this.region.getBytes(), dateKey);
        byte[] serviceKey = hmacSHA256Hash(AWS_SERVICE_VALUE.getBytes(), regionKey);
        return hmacSHA256Hash(SIGNING_KEY_DATA_VALUE.getBytes(), serviceKey);
    }

    private String buildSignature(URL url) {
        String canonicalRequest = buildCanonicalRequest(url);
        String stringToSign = buildStringToSign(canonicalRequest);
        byte[] signature = hmacSHA256Hash(stringToSign.getBytes(), this.signingKey);
        return hex(signature);
    }

    private String getISODate() {
        return ISO_DATE.format(this.creationDate);
    }

    private String getISODateTime() {
        return ISO_DATE_TIME.format(this.creationDate);
    }

    /**
     * Check whether the signing key is valid (not expired)
     *
     * @return {@code true} if the signing key is valid (expiration date is after now)
     */
    boolean isValid() {
        return this.expirationDate != null && this.expirationDate.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void ensureValid() {
        if (!isValid()) {
            this.creationDate = LocalDateTime.now(ZoneOffset.UTC);
            this.expirationDate = this.creationDate.plusMinutes(SIGNING_KEY_VALIDITY);
            this.signingKey = buildSigningKey();
        }
    }

    /**
     * Creates the authorization token used for S3 authentication.
     * @see  <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">AWS Signature Version 4</a>
     *
     * @return The S3 authorization token
     */
    String getAuthorizationToken(URL url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        ensureValid();
        this.awsHeaders = buildAwsHeaders(url);
        if (this.accessKeyId == null || this.accessKeyId.isEmpty() || this.secretAccessKey == null || this.secretAccessKey.isEmpty()) {
            return null;// is public
        }
        String scope = buildScope();
        String signedHeaders = buildSignedHeaders();
        String signature = buildSignature(url);
        String authorizationToken = AWS_AUTHORIZATION_TOKEN_VALUE;
        authorizationToken = authorizationToken.replace(AWS_SIGNATURE_ALGORITHM_NAME, AWS_SIGNATURE_ALGORITHM_VALUE);
        authorizationToken = authorizationToken.replace(AWS_ACCESS_KEY_ID_NAME, this.accessKeyId);
        authorizationToken = authorizationToken.replace(SCOPE_NAME, scope);
        authorizationToken = authorizationToken.replace(SIGNED_HEADERS_NAME, signedHeaders);
        authorizationToken = authorizationToken.replace(AWS_SIGNATURE_NAME, signature);
        return authorizationToken;
    }

    /**
     * Gets the special headers of S3 service
     *
     * @return the special headers of S3 service
     */
    Map<String, String> getAwsHeaders() {
        return awsHeaders;
    }

    private static final class PropertyCodePointSorter implements Comparator<Property> {

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * <p>
         * In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.<p>
         * <p>
         * The implementor must ensure that <tt>sgn(compare(x, y)) ==
         * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>compare(x, y)</tt> must throw an exception if and only
         * if <tt>compare(y, x)</tt> throws an exception.)<p>
         * <p>
         * The implementor must also ensure that the relation is transitive:
         * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
         * <tt>compare(x, z)&gt;0</tt>.<p>
         * <p>
         * Finally, the implementor must ensure that <tt>compare(x, y)==0</tt>
         * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
         * <tt>z</tt>.<p>
         * <p>
         * It is generally the case, but <i>not</i> strictly required that
         * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."
         *
         * @param p1 the first object to be compared.
         * @param p2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(Property p1, Property p2) {
            for (int i = 0; i < Math.min(p1.getName().length(), p2.getName().length()); i++) {
                if (p1.getName().codePointAt(i) != p2.getName().codePointAt(i)) {
                    if (p1.getName().codePointAt(i) < p2.getName().codePointAt(i)) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
            if (p1.getName().length() != p2.getName().length()) {
                if (p1.getName().length() < p2.getName().length()) {
                    return -1;
                } else {
                    return 1;
                }
            }
            for (int i = 0; i < Math.min(p1.getValue().length(), p2.getValue().length()); i++) {
                if (p1.getValue().codePointAt(i) != p2.getValue().codePointAt(i)) {
                    if (p1.getValue().codePointAt(i) < p2.getValue().codePointAt(i)) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
            if (p1.getValue().length() != p2.getValue().length()) {
                if (p1.getValue().length() < p2.getValue().length()) {
                    return -1;
                } else {
                    return 1;
                }
            }
            return 0;
        }
    }

}
