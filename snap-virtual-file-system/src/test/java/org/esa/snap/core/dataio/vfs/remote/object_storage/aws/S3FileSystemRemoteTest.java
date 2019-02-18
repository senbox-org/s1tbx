package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test: Remote File System for Amazon AWS S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemRemoteTest extends S3FileSystemTest {

    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + File.separator + "creds_aws.txt";

    private String awsBucketAddress;
    private String awsAccessKeyId;
    private String awsSecretAccessKey;

    @Override
    String getAwsBucketAddress() {
        return awsBucketAddress;
    }

    String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    void setCredentials() {
        if (awsAccessKeyId == null || awsSecretAccessKey == null) {
            FileInputStream fStream;
            try {
                fStream = new FileInputStream(CREDENTIALS_FILE);
                BufferedReader br = new BufferedReader(new InputStreamReader(fStream));
                awsBucketAddress = br.readLine();
                awsAccessKeyId = br.readLine();
                awsSecretAccessKey = br.readLine();
                br.close();
            } catch (Exception ignored) {
            }
        }
    }

    boolean isReady() {
        return awsBucketAddress != null && awsAccessKeyId != null && awsSecretAccessKey != null;
    }


    @Test
    public void testScanner() throws Exception {
        if (!isReady()) {
            return;
        }
        List<BasicFileAttributes> items;

        items = new S3Walker().walk("", "/");
        assertEquals(11, items.size());

        items = new S3Walker().walk(".git/", "/");
        assertEquals(11, items.size());

        items = new S3Walker().walk("css/", "/");
        assertEquals(2, items.size());
    }

    @Test
    public void testGET() throws Exception {
        if (!isReady()) {
            return;
        }
        URL url = new URL(getAwsBucketAddress() + "/images/rpi.svg");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        int responseCode = connection.getResponseCode();
        System.out.println("responseCode = " + responseCode);
        String responseMessage = connection.getResponseMessage();
        System.out.println("responseMessage = " + responseMessage);

        InputStream stream = connection.getInputStream();
        byte[] b = new byte[1024 * 1024];
        int read = stream.read(b);
        assertTrue(read > 0);
        ReadableByteChannel channel = Channels.newChannel(stream);
        channel.close();
        connection.disconnect();
    }

}
