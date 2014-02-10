package org.esa.pfa.fe.op.out;

import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.FeatureType;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Norman Fomferra
 */
class PatchWriterHelpers {

    public static void copyResource(Class<?> aClass, String resourceName, File targetDir) throws IOException {
        final InputStream is = aClass.getResourceAsStream(resourceName);
        if (is == null) {
            throw new IllegalArgumentException(
                    String.format("resource not found: class %s: resource %s", aClass.getName(), resourceName));
        }
        try {
            final OutputStream os = new FileOutputStream(new File(targetDir, resourceName));
            try {
                byte[] bytes = new byte[16 * 1024];
                int len;
                while ((len = is.read(bytes)) > 0) {
                    os.write(bytes, 0, len);
                }
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
    }

    public static boolean isImageFeatureType(FeatureType featureType) {
        return RenderedImage.class.isAssignableFrom(featureType.getValueType());
    }

    public static boolean isProductFeatureType(FeatureType featureType) {
        return Product.class.isAssignableFrom(featureType.getValueType());
    }
}
