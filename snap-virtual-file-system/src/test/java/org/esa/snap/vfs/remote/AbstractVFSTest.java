package org.esa.snap.vfs.remote;

import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.utils.TestUtil;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public abstract class AbstractVFSTest {

    protected Path vfsTestsFolderPath;
    private List<VFSRemoteFileRepository> vfsRepositories;

    protected AbstractVFSTest() {
    }

    @Before
    public final void setUp() {
        assumeTrue(TestUtil.testDataAvailable());
        checkTestDirectoryExists();
        initVFS();
    }

    private void checkTestDirectoryExists() {
        String testDirectoryPathProperty = System.getProperty(TestUtil.PROPERTY_NAME_DATA_DIR);
        assertNotNull("The system property '" + TestUtil.PROPERTY_NAME_DATA_DIR + "' representing the test directory is not set.", testDirectoryPathProperty);
        Path testFolderPath = Paths.get(testDirectoryPathProperty);
        if (!Files.exists(testFolderPath)) {
            fail("The test directory path " + testDirectoryPathProperty + " is not valid.");
        }

        this.vfsTestsFolderPath = testFolderPath.resolve("_virtual_file_system");
        if (!Files.exists(vfsTestsFolderPath)) {
            fail("The VFS test directory path " + vfsTestsFolderPath.toString() + " is not valid.");
        }
    }

    private void initVFS() {
        try {
            Path configFile = vfsTestsFolderPath.resolve("vfs.properties");
            vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(configFile);
            VFS.getInstance().initRemoteInstalledProviders(vfsRepositories);
        } catch (Exception exception) {
            fail("Failed to initialize VFS.");
        }
    }

    protected VFSRemoteFileRepository getHTTPRepo() {
        return vfsRepositories.get(0);
    }

    protected VFSRemoteFileRepository getS3Repo() {
        return vfsRepositories.get(1);
    }

    protected VFSRemoteFileRepository getSwiftRepo() {
        return vfsRepositories.get(2);
    }


}
