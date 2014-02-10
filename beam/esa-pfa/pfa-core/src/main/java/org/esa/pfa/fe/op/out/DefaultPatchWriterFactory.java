package org.esa.pfa.fe.op.out;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.Patch;

import javax.media.jai.operator.FileStoreDescriptor;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class DefaultPatchWriterFactory extends PatchWriterFactory {

    static {
        ExtensionManager.getInstance().register(Feature.class, new FeatureOutputFactory());
    }

    public static final String IMAGE_FORMAT_NAME = "PNG";
    public static final String IMAGE_FILE_EXT = ".png";
//    public static final String IMAGE_FORMAT_NAME = "JPEG";
//    public static final String IMAGE_FILE_EXT = ".jpg";

    @Override
    public PatchWriter createFeatureOutput(Product sourceProduct) throws IOException {
        return new DefaultPatchWriter(this, sourceProduct);
    }

    private static class FeatureOutputFactory implements ExtensionFactory {
        @Override
        public FeatureOutput getExtension(Object object, Class<?> extensionType) {
            Feature feature = (Feature) object;
            Object value = feature.getValue();
            if (value instanceof Product) {
                return new ProductFeatureOutput();
            } else if (value instanceof RenderedImage) {
                return new RenderedImageFeatureOutput();
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{FeatureOutput.class};
        }

        private static class ProductFeatureOutput implements FeatureOutput {
            @Override
            public String writeFeature(Patch patch, Feature feature, String dirPath) throws IOException {
                Product patchProduct = (Product) feature.getValue();
                String path = new File(dirPath, feature.getName() + ".dim").getPath();
                long t1 = System.currentTimeMillis();
                ProductIO.writeProduct(patchProduct, path, "BEAM-DIMAP");
                long t2 = System.currentTimeMillis();
                BeamLogManager.getSystemLogger().info(String.format("Written %s (%d ms)", path, t2 - t1));
                return path;
            }
        }

        private static class RenderedImageFeatureOutput implements FeatureOutput {
            @Override
            public String writeFeature(Patch patch, Feature feature, String dirPath) throws IOException {
                RenderedImage image = (RenderedImage) feature.getValue();
                File output = new File(dirPath, feature.getName() + IMAGE_FILE_EXT);
                long t1 = System.currentTimeMillis();
                // Note: ImageIO is VERY slow with 'PNG', it's 5x to 10x slower than the JAI codec (on Windows)!
                //ImageIO.write(image, IMAGE_FORMAT_NAME, output);
                FileStoreDescriptor.create(image, output.getPath(), IMAGE_FORMAT_NAME, null, null, null);
                long t2 = System.currentTimeMillis();
                BeamLogManager.getSystemLogger().info(String.format("Written %s (%d ms)", output, t2 - t1));
                return output.getPath();
            }
        }
    }
}
