package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class DefaultPatchWriter implements PatchWriter {

    private static final String PRODUCT_DIR_NAME_EXTENSION = ".fex";

    private final File productTargetDir;
    private final PatchWriter[] patchWriters;

    public DefaultPatchWriter(PatchWriterFactory patchWriterFactory, Product product) throws IOException {

        String targetPath = patchWriterFactory.getTargetPath();
        if (targetPath == null) {
            targetPath = ".";
        }

        File targetDir = new File(targetPath).getAbsoluteFile();

        boolean overwriteMode = patchWriterFactory.isOverwriteMode();
        if (targetDir.exists()) {
            if (!overwriteMode) {
                String[] contents = targetDir.list();
                if (contents != null && contents.length > 0) {
                    throw new IOException(String.format("Directory is not empty: '%s'", targetDir));
                }
            }
        } else {
            if (!overwriteMode) {
                throw new IOException(String.format("Directory does not exist: '%s'", targetDir));
            } else {
                if (!targetDir.mkdirs()) {
                    throw new IOException(String.format("Failed to create directory '%s'", targetDir));
                }
            }
        }

        File productTargetDir = new File(targetDir, product.getName() + PRODUCT_DIR_NAME_EXTENSION);
        if (!productTargetDir.exists()) {
            if (!productTargetDir.mkdir()) {
                throw new IOException(String.format("Failed to create directory '%s'", productTargetDir));
            }
        }

        this.productTargetDir = productTargetDir;

        patchWriters = new PatchWriter[]{
                new PropertiesPatchWriter(productTargetDir),
                new CsvPatchWriter(productTargetDir),
                new HtmlPatchWriter(productTargetDir),
                new XmlPatchWriter(productTargetDir),
                new KmlPatchWriter(productTargetDir),
        };
    }


    @Override
    public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
        for (PatchWriter patchWriter : patchWriters) {
            patchWriter.initialize(configuration, sourceProduct, featureTypes);
        }
    }

    @Override
    public void writePatch(Patch patch, Feature... features) throws IOException {

        final File patchTargetDir = new File(productTargetDir, patch.getPatchName());
        if (!patchTargetDir.exists()) {
            if (!patchTargetDir.mkdir()) {
                throw new IOException(String.format("Failed to create directory '%s'", patchTargetDir));
            }
        }

        for (Feature feature : features) {
            FeatureOutput featureOutput = feature.getExtension(FeatureOutput.class);
            if (featureOutput != null) {
                featureOutput.writeFeature(patch, feature, patchTargetDir.getPath());
            }
        }

        for (PatchWriter patchWriter : patchWriters) {
            patchWriter.writePatch(patch, features);
        }
    }

    @Override
    public void close() throws IOException {
        IOException firstIoe = null;
        for (PatchWriter patchWriter : patchWriters) {
            try {
                patchWriter.close();
            } catch (IOException e) {
                if (firstIoe == null) {
                    firstIoe = e;
                }
            }
        }
        if (firstIoe != null) {
            throw firstIoe;
        }
    }
}
