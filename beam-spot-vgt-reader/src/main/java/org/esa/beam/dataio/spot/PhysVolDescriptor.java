package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

final class PhysVolDescriptor {
    private final PropertySet propertySet;
    private final int physVolNumber;
    private final String productId;
    private final String formatReference;
    private final String logVolDirName;
    private String logVolDescrFileName;

    public static PhysVolDescriptor create(File file) throws IOException {
        FileReader reader = new FileReader(file);
        try {
            return new PhysVolDescriptor(reader);
        } finally {
            reader.close();
        }
    }

    public PhysVolDescriptor(Reader reader) throws IOException {
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(reader);

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
        logVolDirName = String.format("%04d", physVolNumber);
        logVolDescrFileName = String.format("%s/%s_LOG.TXT", logVolDirName, logVolDirName);
        productId = getValue(String.format("PRODUCT_#%s_ID", logVolDirName));
        formatReference = getValue("FORMAT_REFERENCE");

    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public String getValue(String key) {
        return (String) propertySet.getValue(key);
    }

    public int getPhysVolNumber() {
        return physVolNumber;
    }

    public String getLogVolDirName() {
        return logVolDirName;
    }

    public String getLogVolDescriptorFileName() {
        return logVolDescrFileName;
    }

    public String getProductId() {
        return productId;
    }

    public String getFormatReference() {
        return formatReference;
    }

}
