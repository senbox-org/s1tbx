package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;

import java.io.File;
import java.io.IOException;

final class PhysVolDescriptor {
    private final File file;
    private final PropertySet propertySet;
    private final int physVolNumber;
    private final String productId;
    private final String formatReference;
    private final File dataDir;

    public PhysVolDescriptor(File file) throws IOException {
        this.file = file;
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(file);

        String value = getValue("PHYS_VOL_NUMBER");
        if (value != null) {
            try {
                physVolNumber = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid SPOT VGT volume descriptor. Missing value for 'PHYS_VOL_NUMBER'.");
            }
        } else {
            throw new IOException("Invalid SPOT VGT volume descriptor. Missing value for 'PHYS_VOL_NUMBER'.");
        }
        dataDir = new File(file.getParent(), String.format("%04d", physVolNumber));
        productId = getValue(String.format("PRODUCT_#%04d_ID", physVolNumber));
        formatReference = getValue("FORMAT_REFERENCE");
    }

    public String getValue(String key) {
        return (String) propertySet.getValue(key);
    }

    public File getFile() {
        return file;
    }

    public int getPhysVolNumber() {
        return physVolNumber;
    }

    public File getDataDir() {
        return dataDir;
    }

    public String getProductId() {
        return productId;
    }

    public String getFormatReference() {
        return formatReference;
    }
}
