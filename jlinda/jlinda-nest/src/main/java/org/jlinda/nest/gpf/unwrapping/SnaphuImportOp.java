package org.jlinda.nest.gpf.unwrapping;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;

@OperatorMetadata(alias = "SnaphuImport",
        category = "InSAR\\Unwrapping",
        description = "Ingest SNAPHU results into NEST InSAR product.",
        internal = false)
public class SnaphuImportOp extends Operator {

    @SourceProducts(description = "The array of source product of InSAR bands.")
    private Product[] sourceProducts;

    private Product sourceProduct;

    @TargetProduct(description = "The target product for SNAPHU results.")
    private Product targetProduct;

    @Parameter(defaultValue="false", label="Do NOT save Wrapped interferogram in the target product")
    private boolean doNotKeepWrapped = false;

    @Override
    public void initialize() throws OperatorException {

        if (sourceProducts.length != 2) {
            throw new OperatorException("SnaphuImportOp requires EXACTLY two source products.");
        }

        sourceProduct = sourceProducts[0];

        try {

            final Product masterProduct;
            final Product slaveProduct;

            // check which one is the reference product:
            // ....check on geocodings, and pick 1st one that has them as 'master'...
            if (sourceProducts[0].getGeoCoding().canGetGeoPos()) {
                masterProduct = sourceProducts[0];
                slaveProduct = sourceProducts[1];
            } else if (sourceProducts[1].getGeoCoding().canGetGeoPos()) {
                masterProduct = sourceProducts[1];
                slaveProduct = sourceProducts[0];
            } else {
                throw new OperatorException("SnaphuImportOp requires at least one product with InSAR metadata.");
            }

            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            if (masterProduct.getSceneRasterHeight() != slaveProduct.getSceneRasterHeight()) {
                throw new OperatorException("SnaphuImportOp requires input products to be of the same HEIGHT dimension.");
            }

            if (masterProduct.getSceneRasterWidth() != slaveProduct.getSceneRasterWidth()) {
                throw new OperatorException("SnaphuImportOp requires input products to be of the same WIDTH dimension.");
            }

            // create target product
            // ....Note: the productType of target is of slaveProduct (it's about using the metadata of master,
            //           and bands of slave product)
            targetProduct = new Product(masterProduct.getName(),
                    slaveProduct.getProductType(),
                    masterProduct.getSceneRasterWidth(),
                    masterProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(masterProduct, targetProduct);

            // add target bands to the target
            Band[] bands;

            if (!doNotKeepWrapped) {
                bands = masterProduct.getBands();
                for (Band srcBand : bands) {
                    final Band targetBand = ProductUtils.copyBand(srcBand.getName(), masterProduct, targetProduct);
                    targetBand.setSourceImage(srcBand.getSourceImage());
                }
            }

            // assuming this is unwrapped phase result
            bands = slaveProduct.getBands();
            for (Band srcBand : bands) {

                final String masterDate = OperatorUtils.getAcquisitionDate(AbstractMetadata.getAbstractedMetadata(masterProduct));
                final String slaveDate = OperatorUtils.getAcquisitionDate(
                        masterProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0]);

                final Band targetBand = ProductUtils.copyBand(srcBand.getName(), slaveProduct, targetProduct);
                if (targetBand.getName().toLowerCase().contains("unw") || targetBand.getName().toLowerCase().contains("band")) {
                    targetBand.setUnit(Unit.ABS_PHASE); // if there is a band with "unw" set unit to ABS phase
                    targetBand.setName("Unw_Phase_ifg_" + masterDate + "_" + slaveDate); // set the name to Unw_Phase_ifg_masterDate_slaveDate
                }
                targetBand.setSourceImage(srcBand.getSourceImage());
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnaphuImportOp.class);
        }
    }
}