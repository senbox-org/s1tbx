//package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;
//
//import org.junit.Test;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.channels.Channels;
//import java.nio.channels.ReadableByteChannel;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * Test: Remote File System for S3 Object Storage VFS.
// * Requirements:
// * - A file with name: creds_s3.txt, located in user directory.
// * The file must contains the following:
// * - The address of S3 service
// * - The access Key ID
// * - The secret Access Key
// * - The directory name (from root directory) used for tests
// * - The number of items (files and directories) on directory used for tests
// * - The file path used for tests
// * - The size of file used for tests
// *
// * @author Norman Fomferra
// * @author Adrian Drăghici
// */
//public class S3FileSystemRemoteTest extends S3FileSystemTest {
//
//    private static Logger logger = Logger.getLogger(S3FileSystemTest.class.getName());
//
//    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + File.separator + "creds_s3.txt";
//
//    private String bucketAddress;
//    private String accessKeyId;
//    private String secretAccessKey;
//    private String dirForTest;
//    private Long dirForTestItems;
//    private String fileForTest;
//    private Long fileForTestSize;
//
//    @Override
//    String getBucketAddress() {
//        return bucketAddress;
//    }
//
//    String getAccessKeyId() {
//        return accessKeyId;
//    }
//
//    String getSecretAccessKey() {
//        return secretAccessKey;
//    }
//
//    String getDirForTest() {
//        return dirForTest;
//    }
//
//    Long getDirForTestItems() {
//        return dirForTestItems;
//    }
//
//    String getFileForTest() {
//        return fileForTest;
//    }
//
//    Long getFileForTestSize() {
//        return fileForTestSize;
//    }
//
//    void setData() {
//        if (accessKeyId == null || secretAccessKey == null) {
//            FileInputStream fStream;
//            try {
//                fStream = new FileInputStream(CREDENTIALS_FILE);
//                BufferedReader br = new BufferedReader(new InputStreamReader(fStream));
//                bucketAddress = br.readLine();
//                accessKeyId = br.readLine();
//                secretAccessKey = br.readLine();
//                dirForTest = br.readLine();
//                dirForTestItems = Long.parseLong(br.readLine());
//                fileForTest = br.readLine();
//                fileForTestSize = Long.parseLong(br.readLine());
//                br.close();
//            } catch (Exception ex) {
//                logger.log(Level.SEVERE, "Unable to set test input data. Details: " + ex.getMessage());
//            }
//        }
//    }
//
//    boolean isReady() {
//        return bucketAddress != null && accessKeyId != null && secretAccessKey != null && dirForTest != null && fileForTest != null && dirForTestItems != null && fileForTestSize != null;
//    }
//
//
//    @Test
//    public void testScanner() throws Exception {
//        if (!isReady()) {
//            return;
//        }
//        List<BasicFileAttributes> items;
//
//        ;
//
//        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", getDirForTest().substring(0, getDirForTest().lastIndexOf("/"))).walk(getDirForTest().substring(getDirForTest().lastIndexOf("/") + 1) + "/");
//        assertEquals(11, items.size());
//
//        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", "").walk(".git/");
//        assertEquals(11, items.size());
//
//        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", "").walk("css/");
//        assertEquals(2, items.size());
//    }
//
//    @Test
//    public void testGET() throws Exception {
//        if (!isReady()) {
//            return;
//        }
//        URL url = new URL(getBucketAddress() + "/images/rpi.svg");
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//        connection.setDoInput(true);
//        connection.connect();
//
//        int responseCode = connection.getResponseCode();
//        System.out.println("responseCode = " + responseCode);
//        String responseMessage = connection.getResponseMessage();
//        System.out.println("responseMessage = " + responseMessage);
//
//        InputStream stream = connection.getInputStream();
//        byte[] b = new byte[1024 * 1024];
//        int read = stream.read(b);
//        assertTrue(read > 0);
//        ReadableByteChannel channel = Channels.newChannel(stream);
//        channel.close();
//        connection.disconnect();
//    }
//
//}
