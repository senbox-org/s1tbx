package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.gpf.OperatorUtils;

import java.io.File;
import java.io.IOException;

public class FileModel extends BaseFileModel implements FileTableModel {

        protected void setColumnData() {
            titles = new String[]{
                "File Name", "Type", "Acquisition", "Track", "Orbit"
            };

            types = new Class[]{
                String.class, String.class, String.class, String.class, String.class
            };

            widths = new int[]{
                75, 10, 20, 3, 5
            };
        }

        protected TableData createFileStats(final File file) {
            return new FileStats(file);
        }

        protected TableData createFileStats(final ProductEntry entry) {
            return new FileStats(entry);
        }

        private class FileStats extends TableData {

            FileStats(final File file) {
                super(file);
            }
            FileStats(final ProductEntry entry) {
                super(entry);
            }

            protected void updateData(final File file) throws IOException {
                data[0] = file.getName();
                final Product product = ProductIO.readProduct(file);
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

                data[0] = product.getName();
                data[1] = product.getProductType();
                data[2] = OperatorUtils.getAcquisitionDate(absRoot);
                data[3] = String.valueOf(absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, 0));
                data[4] = String.valueOf(absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0));
            }

            protected void updateData(final ProductEntry entry) {
                data[0] = entry.getName();
                data[1] = entry.getProductType();
                data[2] = entry.getFirstLineTime().format();

                final MetadataElement meta = entry.getMetadata();
                if(meta != null) {
                    data[3] = String.valueOf(meta.getAttributeInt(AbstractMetadata.REL_ORBIT, 0));
                    data[4] = String.valueOf(meta.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0));
                }
            }
        }
    }