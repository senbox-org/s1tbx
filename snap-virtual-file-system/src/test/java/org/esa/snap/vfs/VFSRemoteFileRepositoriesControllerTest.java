package org.esa.snap.vfs;

import org.esa.snap.vfs.preferences.model.Property;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractVFSTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class VFSRemoteFileRepositoriesControllerTest extends AbstractVFSTest {

    private static final String REPOS_IDS = "1553076150344;1553076250344;1553076350344";

    private static final String REPO_1_NAME = "http_vfs_test";
    private static final String REPO_1_SCHEMA = "http";
    private static final String REPO_1_ADDRESS = "http://localhost:0/mock-api/";
    private static final String REPO_1_PROPS_IDS = "";

    private static final String REPO_2_NAME = "s3_vfs_test";
    private static final String REPO_2_SCHEMA = "s3";
    private static final String REPO_2_ADDRESS = "http://localhost:0/mock-api/";
    private static final String REPO_2_PROPS_IDS = "1553076251344;1553076252344;1553076253344";
    private static final String REPO_2_PROP_1_NAME = "bucket";
    private static final String REPO_2_PROP_1_VALUE = "vfs";
    private static final String REPO_2_PROP_2_NAME = "accessKeyId";
    private static final String REPO_2_PROP_2_VALUE = "AKIAIOSFODNN7EXAMPLE";
    private static final String REPO_2_PROP_3_NAME = "secretAccessKey";
    private static final String REPO_2_PROP_3_VALUE = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    private static final String REPO_3_NAME = "swift_vfs_test";
    private static final String REPO_3_SCHEMA = "oss";
    private static final String REPO_3_ADDRESS = "http://localhost:0/mock-api/";
    private static final String REPO_3_PROPS_IDS = "1553076351344;1553076352344;1553076353344;1553076354344;1553076355344;1553076356344";
    private static final String REPO_3_PROP_1_NAME = "container";
    private static final String REPO_3_PROP_1_VALUE = "vfs";
    private static final String REPO_3_PROP_2_NAME = "authAddress";
    private static final String REPO_3_PROP_2_VALUE = "http://localhost:0/mock-api/v3/auth/tokens";
    private static final String REPO_3_PROP_3_NAME = "domain";
    private static final String REPO_3_PROP_3_VALUE = "cloud_14547";
    private static final String REPO_3_PROP_4_NAME = "projectId";
    private static final String REPO_3_PROP_4_VALUE = "c4761f89c8d940cd9a6dbfa7b72d6cba";
    private static final String REPO_3_PROP_5_NAME = "user";
    private static final String REPO_3_PROP_5_VALUE = "swift_test";
    private static final String REPO_3_PROP_6_NAME = "password";
    private static final String REPO_3_PROP_6_VALUE = "SwIfT0#";

    private Path configFile;
    private Path newConfigFile;

    @Before
    public void setUpVFSRemoteFileRepositoriesControllerTest() {
        this.configFile = this.vfsTestsFolderPath.resolve("vfs.properties");
        Path tempFolderPath = Paths.get(System.getProperty("user.home"));
        this.newConfigFile = tempFolderPath.resolve("vfs_new.properties");
        assumeTrue(Files.exists(this.configFile));
        assumeFalse(Files.exists(this.newConfigFile));
    }

    @After
    public void clean() throws IOException {
        if (this.newConfigFile != null && Files.exists(this.newConfigFile)) {
            Files.delete(this.newConfigFile);
        }
    }

    @Test
    public void testLoad() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
    }

    @Test
    public void testCreate() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.newConfigFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertTrue(reposIds.getValue().isEmpty());
    }

    @Test
    public void testIsChanged() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());
        assertFalse(vfsRemoteFileRepositoriesController.isChanged());

        String newId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
        assertTrue(newId.matches("^([\\d]{13})$"));
        assertTrue(vfsRemoteFileRepositoriesController.isChanged());
    }

    @Test
    public void testGetRemoteRepositoriesIds() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());
    }

    @Test
    public void testGetRemoteRepositoryPropertiesIds() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[2]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_3_PROPS_IDS, repoPropsIds.getValue());
    }

    @Test
    public void testRegisterNewRemoteRepository() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.newConfigFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertTrue(reposIds.getValue().isEmpty());

        String newRepoId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
        assertTrue(newRepoId.matches("^([\\d]{13})$"));

        reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(newRepoId, reposIds.getValue());

        String newRepoId1 = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
        assertTrue(newRepoId1.matches("^([\\d]{13})$"));

        reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(newRepoId + VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR + newRepoId1, reposIds.getValue());
    }

    @Test
    public void testRegisterNewRemoteRepositoryProperty() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.newConfigFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertTrue(reposIds.getValue().isEmpty());

        String newRepoId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
        assertTrue(newRepoId.matches("^([\\d]{13})$"));

        String newRepoPropId = vfsRemoteFileRepositoriesController.registerNewRemoteRepositoryProperty(newRepoId);
        assertTrue(newRepoPropId.matches("^([\\d]{13})$"));

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(newRepoId);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(newRepoPropId, repoPropsIds.getValue());

        String newRepoPropId1 = vfsRemoteFileRepositoriesController.registerNewRemoteRepositoryProperty(newRepoId);
        assertTrue(newRepoPropId1.matches("^([\\d]{13})$"));

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(newRepoId);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(newRepoPropId + VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR + newRepoPropId1, repoPropsIds.getValue());

    }

    @Test
    public void testGetRemoteRepositoryName() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[0]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertEquals(REPO_1_NAME, repoName.getValue());

        repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[1]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertEquals(REPO_2_NAME, repoName.getValue());

        repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[2]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertEquals(REPO_3_NAME, repoName.getValue());
    }

    @Test
    public void testSetRemoteRepositoryName() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[0]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertEquals(REPO_1_NAME, repoName.getValue());

        String newRepoName = "new_name";
        vfsRemoteFileRepositoriesController.setRemoteRepositoryName(reposIdsList[0], newRepoName);

        repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[0]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertEquals(newRepoName, repoName.getValue());
    }

    @Test
    public void testGetRemoteRepositorySchema() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[0]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertEquals(REPO_1_SCHEMA, repoSchema.getValue());

        repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[1]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertEquals(REPO_2_SCHEMA, repoSchema.getValue());

        repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[2]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertEquals(REPO_3_SCHEMA, repoSchema.getValue());
    }

    @Test
    public void testSetRemoteRepositorySchema() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[0]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertEquals(REPO_1_SCHEMA, repoSchema.getValue());

        String newRepoSchema = "newschema";
        vfsRemoteFileRepositoriesController.setRemoteRepositorySchema(reposIdsList[0], newRepoSchema);

        repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[0]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertEquals(newRepoSchema, repoSchema.getValue());
    }

    @Test
    public void testGetRemoteRepositoryAddress() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[0]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertEquals(REPO_1_ADDRESS, repoAddress.getValue());

        repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[1]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertEquals(REPO_2_ADDRESS, repoAddress.getValue());

        repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[2]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertEquals(REPO_3_ADDRESS, repoAddress.getValue());
    }

    @Test
    public void testSetRemoteRepositoryAddress() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[0]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertEquals(REPO_1_ADDRESS, repoAddress.getValue());

        String newRepoAddress = "http://test.vfs";
        vfsRemoteFileRepositoriesController.setRemoteRepositoryAddress(reposIdsList[0], newRepoAddress);

        repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[0]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertEquals(newRepoAddress, repoAddress.getValue());
    }

    @Test
    public void testGetRemoteRepositoryPropertyName() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        String[] repoPropsIdsList = REPO_2_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_2_PROP_1_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[1]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_2_PROP_2_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[2]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_2_PROP_3_NAME, repoPropName.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[2]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_3_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIdsList = REPO_3_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[0]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_1_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[1]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_2_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[2]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_3_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[3]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_4_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[4]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_5_NAME, repoPropName.getValue());

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[2], repoPropsIdsList[5]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_3_PROP_6_NAME, repoPropName.getValue());
    }

    @Test
    public void testSetRemoteRepositoryPropertyName() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        String[] repoPropsIdsList = REPO_2_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(REPO_2_PROP_1_NAME, repoPropName.getValue());

        String newRepoPropName = "new_name";
        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[0], newRepoPropName);

        repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertEquals(newRepoPropName, repoPropName.getValue());
    }

    @Test
    public void testGetRemoteRepositoryPropertyValue() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        String[] repoPropsIdsList = REPO_2_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_1_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[1]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_2_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[2]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_3_VALUE, repoPropValue.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[2]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_3_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIdsList = REPO_3_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[0]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_1_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[1]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_2_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[2]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_3_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[3]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_4_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[4]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_5_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[2], repoPropsIdsList[5]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_3_PROP_6_VALUE, repoPropValue.getValue());
    }

    @Test
    public void testSetRemoteRepositoryPropertyValue() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        String[] repoPropsIdsList = REPO_2_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_1_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[1]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_2_VALUE, repoPropValue.getValue());

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[2]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(REPO_2_PROP_3_VALUE, repoPropValue.getValue());

        String newRepoPropValue = "new_value";
        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[0], newRepoPropValue);

        repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertEquals(newRepoPropValue, repoPropValue.getValue());
    }

    @Test
    public void testRemoveRemoteRepositoryProperty() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[0]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_1_PROPS_IDS, repoPropsIds.getValue());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());

        String[] repoPropsIdsList = REPO_2_PROPS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);
        vfsRemoteFileRepositoriesController.removeRemoteRepositoryProperty(reposIdsList[1], repoPropsIdsList[0]);

        Property repoPropName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropName);
        assertNotNull(repoPropName.getValue());
        assertTrue(repoPropName.getValue().isEmpty());

        Property repoPropValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(reposIdsList[1], repoPropsIdsList[0]);
        assertNotNull(repoPropValue);
        assertNotNull(repoPropValue.getValue());
        assertTrue(repoPropValue.getValue().isEmpty());

        repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertNotEquals(REPO_2_PROPS_IDS, repoPropsIds.getValue());
    }

    @Test
    public void testRemoveRemoteRepository() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.configFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(REPOS_IDS, reposIds.getValue());

        String[] reposIdsList = REPOS_IDS.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);
        vfsRemoteFileRepositoriesController.removeRemoteRepository(reposIdsList[1]);

        Property repoName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(reposIdsList[1]);
        assertNotNull(repoName);
        assertNotNull(repoName.getValue());
        assertTrue(repoName.getValue().isEmpty());

        Property repoSchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(reposIdsList[1]);
        assertNotNull(repoSchema);
        assertNotNull(repoSchema.getValue());
        assertTrue(repoSchema.getValue().isEmpty());

        Property repoAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(reposIdsList[1]);
        assertNotNull(repoAddress);
        assertNotNull(repoAddress.getValue());
        assertTrue(repoAddress.getValue().isEmpty());

        Property repoPropsIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(reposIdsList[1]);
        assertNotNull(repoPropsIds);
        assertNotNull(repoPropsIds.getValue());
        assertTrue(repoPropsIds.getValue().isEmpty());

        reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertNotEquals(REPOS_IDS, reposIds.getValue());
    }

    @Test
    public void testGetVFSRemoteFileRepositories() throws IOException {
        List<VFSRemoteFileRepository> reposList = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(this.configFile);
        assertNotNull(reposList);
        assertFalse(reposList.isEmpty());
        assertEquals(3, reposList.size());

        VFSRemoteFileRepository repo = reposList.get(0);
        assertNotNull(repo);
        assertNotNull(repo.getName());
        assertEquals(REPO_1_NAME, repo.getName());
        assertNotNull(repo.getScheme());
        assertEquals(REPO_1_SCHEMA, repo.getScheme());
        assertNotNull(repo.getAddress());
        assertEquals(REPO_1_ADDRESS, repo.getAddress());

        List<Property> repoProps = repo.getProperties();
        assertNotNull(repoProps);
        assertTrue(repoProps.isEmpty());

        repo = reposList.get(1);
        assertNotNull(repo);
        assertNotNull(repo.getName());
        assertEquals(REPO_2_NAME, repo.getName());
        assertNotNull(repo.getScheme());
        assertEquals(REPO_2_SCHEMA, repo.getScheme());
        assertNotNull(repo.getAddress());
        assertEquals(REPO_2_ADDRESS, repo.getAddress());

        repoProps = repo.getProperties();
        assertNotNull(repoProps);
        assertFalse(repoProps.isEmpty());
        assertEquals(3, repoProps.size());

        Property repoProp = repoProps.get(0);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_2_PROP_1_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_2_PROP_1_VALUE, repoProp.getValue());

        repoProp = repoProps.get(1);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_2_PROP_2_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_2_PROP_2_VALUE, repoProp.getValue());

        repoProp = repoProps.get(2);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_2_PROP_3_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_2_PROP_3_VALUE, repoProp.getValue());

        repo = reposList.get(2);
        assertNotNull(repo);
        assertNotNull(repo.getName());
        assertEquals(REPO_3_NAME, repo.getName());
        assertNotNull(repo.getScheme());
        assertEquals(REPO_3_SCHEMA, repo.getScheme());
        assertNotNull(repo.getAddress());
        assertEquals(REPO_3_ADDRESS, repo.getAddress());

        repoProps = repo.getProperties();
        assertNotNull(repoProps);
        assertFalse(repoProps.isEmpty());
        assertEquals(6, repoProps.size());

        repoProp = repoProps.get(0);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_1_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_1_VALUE, repoProp.getValue());

        repoProp = repoProps.get(1);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_2_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_2_VALUE, repoProp.getValue());

        repoProp = repoProps.get(2);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_3_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_3_VALUE, repoProp.getValue());

        repoProp = repoProps.get(3);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_4_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_4_VALUE, repoProp.getValue());

        repoProp = repoProps.get(4);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_5_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_5_VALUE, repoProp.getValue());

        repoProp = repoProps.get(5);
        assertNotNull(repoProp.getName());
        assertEquals(REPO_3_PROP_6_NAME, repoProp.getName());
        assertNotNull(repoProp.getValue());
        assertEquals(REPO_3_PROP_6_VALUE, repoProp.getValue());
    }

    @Test
    public void testSaveProperties() throws IOException {
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.newConfigFile);

        Property reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertTrue(reposIds.getValue().isEmpty());

        String newRepoId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
        assertTrue(newRepoId.matches("^([\\d]{13})$"));

        vfsRemoteFileRepositoriesController.saveProperties();
        vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(this.newConfigFile);

        reposIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds();
        assertNotNull(reposIds);
        assertNotNull(reposIds.getValue());
        assertEquals(newRepoId, reposIds.getValue());
    }

}
