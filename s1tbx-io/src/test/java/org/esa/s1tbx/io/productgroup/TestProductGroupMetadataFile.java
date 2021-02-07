package org.esa.s1tbx.io.productgroup;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.esa.snap.core.util.Debug.assertTrue;

public class TestProductGroupMetadataFile {

    @Test
    public void testWrite() throws Exception {
        final ProductGroupMetadataFile metadataFile = new ProductGroupMetadataFile();

        metadataFile.addAsset(new ProductGroupMetadataFile.Asset(
                    "name", "path_to_file", "BEAM-DIMAP"));

        final File file = new File(Files.createTempDirectory("productgroups").toFile(), "product_group.json");
        metadataFile.write(file);
        assertTrue(file.exists());

        file.delete();
    }
}
