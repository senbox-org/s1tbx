package org.esa.snap.vfs.remote;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Test: File System Provider for Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class TestRemoteFileSystemProvider extends AbstractRemoteFileSystemProvider {

    private static final String TEST_ROOT = "Test:/";
    private final static String SCHEME = "test";

    public TestRemoteFileSystemProvider() {
        super();
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {

    }

    @Override
    protected AbstractRemoteFileSystem newFileSystem(String address, Map<String, ?> env) {
        return new TestRemoteFileSystem(this, TEST_ROOT);
    }

    @Override
    protected ObjectStorageWalker newObjectStorageWalker(String fileSystemRoot) {
        return null;
    }

    @Override
    public String getProviderAddress() {
        return AbstractRemoteFileSystemProvider.class.getName();
    }

    @Override
    public String getProviderFileSeparator() {
        return "/";
    }

    @Override
    public HttpsURLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) {
        return null;
    }

    private class TestRemoteFileSystem extends AbstractRemoteFileSystem {

        TestRemoteFileSystem(AbstractRemoteFileSystemProvider provider, String root) {
            super(provider, root);
        }
    }

}
