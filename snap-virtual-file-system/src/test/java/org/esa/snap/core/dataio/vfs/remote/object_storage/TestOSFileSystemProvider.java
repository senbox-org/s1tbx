//package org.esa.snap.core.dataio.vfs.remote.object_storage;
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
//public class TestOSFileSystemProvider extends AbstractRemoteFileSystemProvider {
//
//    private static final String TEST_ROOT = "Test:/";
//    private final static String SCHEME = "test";
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
//        return new ObjectStorageFileSystem(this,
//                                           address,
//                                           delimiter != null ? delimiter.toString() : "/");
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
//    public String getRoot() {
//        return TEST_ROOT;
//    }
//
//    @Override
//    public HttpsURLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) {
//        return null;
//    }
//
//}
