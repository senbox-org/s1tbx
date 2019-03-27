//package org.esa.snap.vfs.remote;
//
//import javax.net.ssl.HttpsURLConnection;
//import java.net.URL;
//import java.util.Map;
//
///**
// * Test: File System Provider for Object Storage VFS.
// *
// * @author Norman Fomferra
// * @author Adrian Drăghici
// */
//public class TestRemoteFileSystemProvider extends AbstractRemoteFileSystemProvider {
//
//    private static final String TEST_ROOT = "Test:/";
//    private final static String SCHEME = "test";
//
//    public TestRemoteFileSystemProvider() {
//        super(TEST_ROOT);
//    }
//
//    /**
//     * Returns the URI scheme that identifies this provider.
//     *
//     * @return The URI scheme
//     */
//    @Override
//    public String getScheme() {
//        return SCHEME;
//    }
//
//    @Override
//    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
//
//    }
//
//    @Override
//    protected ObjectStorageFileSystem newFileSystem(String address, Map<String, ?> env) {
//        Object delimiter = env.get("delimiter");
//        return new TestRemoteFileSystem(this, address, delimiter != null ? delimiter.toString() : "/");
//    }
//
//    @Override
//    protected ObjectStorageWalker newObjectStorageWalker() {
//        return null;
//    }
//
//    @Override
//    public String getProviderAddress() {
//        return AbstractRemoteFileSystemProvider.class.getName();
//    }
//
//    @Override
//    public HttpsURLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) {
//        return null;
//    }
//
//    private class TestRemoteFileSystem extends ObjectStorageFileSystem {
//
//        TestRemoteFileSystem(AbstractRemoteFileSystemProvider provider, String address, String separator) {
//            super(provider, address, separator);
//        }
//    }
//
//}
