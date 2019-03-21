//package org.esa.snap.core.dataio.vfs.remote.object_storage.swift;
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
// * Test: Remote File System for OpenStack Swift Object Storage VFS.
// *
// * @author Adrian Drăghici
// */
//public class SwiftFileSystemRemoteTest extends SwiftFileSystemTest {
//
//    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + File.separator + "creds_swift.txt";
//
//    private static Logger logger = Logger.getLogger(SwiftFileSystemTest.class.getName());
//
//    private String address;
//    private String authAddress;
//    private String container;
//    private String domain;
//    private String projectId;
//    private String user;
//    private String password;
//
//    String getAddress() {
//        return address;
//    }
//
//    String getAuthAddress() {
//        return authAddress;
//    }
//
//    String getContainer() {
//        return container;
//    }
//
//    String getDomain() {
//        return domain;
//    }
//
//    String getProjectId() {
//        return projectId;
//    }
//
//    String getUser() {
//        setCredentials();
//        return user;
//    }
//
//    String getPassword() {
//        setCredentials();
//        return password;
//    }
//
//    void setCredentials() {
//        if (user == null || password == null) {
//            FileInputStream fStream;
//            try {
//                fStream = new FileInputStream(CREDENTIALS_FILE);
//                BufferedReader br = new BufferedReader(new InputStreamReader(fStream));
//                address = br.readLine();
//                authAddress = br.readLine();
//                container = br.readLine();
//                domain = br.readLine();
//                projectId = br.readLine();
//                user = br.readLine();
//                password = br.readLine();
//                br.close();
//            } catch (Exception ex) {
//                logger.log(Level.SEVERE, "Unable to set test input data. Details: " + ex.getMessage());
//            }
//        }
//    }
//
//    boolean isReady() {
//        return container != null && domain != null && projectId != null && user != null && password != null;
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
//        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk("");
//        assertEquals(8, items.size());
//
//        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk("romania/");
//        assertEquals(5267, items.size());
//
//        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk("romania/LC08_L2A_180029_20161010_20170320_01_T1/");
//        assertEquals(9, items.size());
//    }
//
//    @Test
//    public void testGET() throws Exception {
//        if (!isReady()) {
//            return;
//        }
//        URL url = new URL(getAddress() + getContainer() + "/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
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
