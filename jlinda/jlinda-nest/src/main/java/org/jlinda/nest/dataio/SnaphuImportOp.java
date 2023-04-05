package org.jlinda.nest.dataio;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

@OperatorMetadata(alias = "SnaphuImport",
        category = "Radar/Interferometric/Unwrapping",
        authors = "Petar Marinkovic, Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Ingest SNAPHU results into InSAR product.")
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

            final Product primaryProduct;
            final Product secondaryProduct;

            // check which one is the reference product:
            // ....check on geocodings, and pick 1st one that has them as 'master'...
            if (sourceProducts[0].getSceneGeoCoding() != null && sourceProducts[0].getSceneGeoCoding().canGetGeoPos()) {
                primaryProduct = sourceProducts[0];
                secondaryProduct = sourceProducts[1];
            } else if (sourceProducts[1].getSceneGeoCoding() != null && sourceProducts[1].getSceneGeoCoding().canGetGeoPos()) {
                primaryProduct = sourceProducts[1];
                secondaryProduct = sourceProducts[0];
            } else {
                throw new OperatorException("SnaphuImportOp requires at least one product with InSAR metadata.");
            }

            if (primaryProduct.getSceneRasterHeight() != secondaryProduct.getSceneRasterHeight()) {
                throw new OperatorException("SnaphuImportOp requires input products to be of the same HEIGHT dimension.");
            }

            if (primaryProduct.getSceneRasterWidth() != secondaryProduct.getSceneRasterWidth()) {
                throw new OperatorException("SnaphuImportOp requires input products to be of the same WIDTH dimension.");
            }

            // create target product
            // ....Note: the productType of target is of slaveProduct (it's about using the metadata of master,
            //           and bands of slave product)
            targetProduct = new Product(primaryProduct.getName(),
                    secondaryProduct.getProductType(),
                    primaryProduct.getSceneRasterWidth(),
                    primaryProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(primaryProduct, targetProduct);

            // add target bands to the target
            Band[] bands;

            if (!doNotKeepWrapped) {
                bands = primaryProduct.getBands();
                for (Band srcBand : bands) {
                    String sourceBandName = srcBand.getName();
                    ProductUtils.copyBand(sourceBandName, primaryProduct, sourceBandName, targetProduct, true);
                }
            }

            // assuming this is unwrapped phase result
            boolean unwrappedPhaseFound = false;
            bands = secondaryProduct.getBands();
            for (Band srcBand : bands) {

                String primaryDate = OperatorUtils.getAcquisitionDate(AbstractMetadata.getAbstractedMetadata(primaryProduct));
                String secondaryDate = "";
                String targetBandName = "";
                if(primaryProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements().length == 1){
                    secondaryDate = OperatorUtils.getAcquisitionDate(
                            primaryProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0]);
                    targetBandName = "Unw_Phase_ifg_" + primaryDate + "_" + secondaryDate;
                }else{
                    // We have multiple secondary metadata objects - cannot just blindly assign the first one!
                    targetBandName = srcBand.getName();
                }


                String sourceBandName = srcBand.getName();
                final Band targetBand = ProductUtils.copyBand(sourceBandName, secondaryProduct, sourceBandName, targetProduct, true);
                if (targetBand.getName().toLowerCase().contains("unw") || targetBand.getName().toLowerCase().contains("band")) {
                    targetBand.setUnit(Unit.ABS_PHASE); // if there is a band with "unw" set unit to ABS phase
                    targetBand.setName(targetBandName); // set the name to Unw_Phase_ifg_masterDate_slaveDate
                    targetProduct.setQuicklookBandName(targetBand.getName());
                    unwrappedPhaseFound = true;

                    targetProduct.setQuicklookBandName(targetBand.getName());
                }
            }
            if(!unwrappedPhaseFound) {
                throw new OperatorException("SnaphuImportOp requires an unwrapped phase product");
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
