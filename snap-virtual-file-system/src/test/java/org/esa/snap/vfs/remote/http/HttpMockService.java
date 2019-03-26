package org.esa.snap.vfs.remote.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assume.assumeTrue;

class HttpMockService {

    private HttpsServer mockServer;

    HttpMockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        mockServer = HttpsServer.create(new InetSocketAddress(serviceAddress.getHost(), serviceAddress.getPort()), 0);
        mockServer.createContext(serviceAddress.getPath(), new S3MockServiceHandler(serviceRootPath));
    }

    void start() {
        mockServer.start();
    }

    void stop() {
        mockServer.stop(1);
    }

    private class S3MockServiceHandler implements HttpHandler {

        static final String htmlFile = "index.html";
        private Path serviceRootPath;

        S3MockServiceHandler(Path serviceRootPath) {
            assumeTrue(Files.exists(serviceRootPath));
            this.serviceRootPath = serviceRootPath;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String urlPath = httpExchange.getRequestURI().getPath();
            Path responsePath = serviceRootPath.resolve(urlPath);
            if (Files.isDirectory(responsePath)) {
                responsePath = responsePath.resolve(htmlFile);

            }
            if (!Files.exists(responsePath)) {
                httpExchange.sendResponseHeaders(404, 1);
                return;
            }
            byte[] response = readFile(responsePath);
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response);
            os.close();
        }

        private byte[] readFile(Path inputFile) throws IOException {
            InputStream is = Files.newInputStream(inputFile);
            byte data[] = new byte[is.available()];
            is.read(data);
            is.close();
            return data;
        }
    }

}
