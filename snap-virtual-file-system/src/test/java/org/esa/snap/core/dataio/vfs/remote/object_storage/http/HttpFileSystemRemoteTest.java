package org.esa.snap.core.dataio.vfs.remote.object_storage.http;

import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test: Remote File System for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class HttpFileSystemRemoteTest extends HttpFileSystemTest {

    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + File.separator + "creds_http.txt";

    private String address;
    private String user;
    private String password;

    @Override
    String getAddress() {
        return address;
    }

    String getUser() {
        setCredentials();
        return user;
    }

    String getPassword() {
        setCredentials();
        return password;
    }

    void setCredentials() {
        if (user == null || password == null) {
            FileInputStream fStream;
            try {
                fStream = new FileInputStream(CREDENTIALS_FILE);
                BufferedReader br = new BufferedReader(new InputStreamReader(fStream));
                address = br.readLine();
                user = br.readLine();
                password = br.readLine();
                br.close();
            } catch (Exception ignored) {
            }
        }
    }

    boolean isReady() {
        return address != null && user != null && password != null;
    }


    @Test
    public void testScanner() throws Exception {
        if (!isReady()) {
            return;
        }
        List<BasicFileAttributes> items;

        items = new HttpWalker().walk("", "/");
        assertEquals(2, items.size());

        items = new HttpWalker().walk("S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/", "/");
        assertEquals(10, items.size());

        items = new HttpWalker().walk("S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/", "/");
        assertEquals(6, items.size());
    }

    @Test
    public void testGET() throws Exception {
        if (!isReady()) {
            return;
        }
        URL url = new URL(getAddress() + "/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png");
        URLConnection connection = url.openConnection();
        ((HttpURLConnection) connection).setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.connect();

        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        System.out.println("responseCode = " + responseCode);
        String responseMessage = ((HttpURLConnection) connection).getResponseMessage();
        System.out.println("responseMessage = " + responseMessage);

        InputStream stream = connection.getInputStream();
        byte[] b = new byte[1024 * 1024];
        int read = stream.read(b);
        assertTrue(read > 0);
        ReadableByteChannel channel = Channels.newChannel(stream);
        channel.close();
        ((HttpURLConnection) connection).disconnect();
    }

    @Test
    @Ignore
    public void testPost() throws Exception {
        if (!isReady()) {
            return;
        }
        String url = "http://localhost:56789/";
        String charset = "UTF-8";
        String param = "value";
        File textFile = new File("C:\\Users\\adraghici\\Desktop\\test.txt");
        File binaryFile = new File("C:\\Users\\adraghici\\Desktop\\test.docx");
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (
                OutputStream output = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)
        ) {
            // Send normal param.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=").append(charset).append(CRLF);
            writer.append(CRLF).append(param).append(CRLF).flush();

            // Send text file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"").append(textFile.getName()).append("\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=").append(charset).append(CRLF); // Text file itself must be saved in this charset!
            writer.append(CRLF).flush();
            Files.copy(textFile.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // Send binary file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"").append(binaryFile.getName()).append("\"").append(CRLF);
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            Files.copy(binaryFile.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
        }

// Request is lazily fired whenever you need to obtain information about response.
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        System.out.println(responseCode); // Should be 200
    }

    @Test
    @Ignore
    public void testPut() {
        if (!isReady()) {
            return;
        }
        URLConnection urlconnection;
        try {
            File file = new File("C:\\Users\\adraghici\\Desktop\\test.docx");
            URL url = new URL("http://pcd3254.c-s.ro/snap/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/test.docx");
            urlconnection = url.openConnection();
            urlconnection.setDoOutput(true);
            urlconnection.setDoInput(true);
            if (urlconnection instanceof HttpURLConnection) {
                try {
                    ((HttpURLConnection) urlconnection).setRequestMethod("POST");
                    urlconnection.setRequestProperty("Content-type", "application/octet-stream");
                    urlconnection.connect();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
                BufferedOutputStream bos = new BufferedOutputStream(urlconnection
                                                                            .getOutputStream());
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                        file));
                int i;
                // read byte by byte until end of stream
                while ((i = bis.read()) > 0) {
                    bos.write(i);
                }
                System.out.println(((HttpURLConnection) urlconnection).getResponseMessage());
                try {
                    InputStream inputStream;
                    int responseCode = ((HttpURLConnection) urlconnection).getResponseCode();
                    if ((responseCode >= 200) && (responseCode <= 202)) {
                        inputStream = urlconnection.getInputStream();
                        int j;
                        while ((j = inputStream.read()) > 0) {
                            System.out.println(j);
                        }
                    } else {
                        throw new IOException(url + ": response code " + responseCode + ": " + ((HttpURLConnection) urlconnection).getResponseMessage());
                    }
                    ((HttpURLConnection) urlconnection).disconnect();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

