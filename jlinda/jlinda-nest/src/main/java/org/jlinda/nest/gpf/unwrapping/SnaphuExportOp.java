package org.jlinda.nest.gpf.unwrapping;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
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

import java.util.Arrays;
import java.util.List;

@OperatorMetadata(alias = "SnaphuExport",
        category = "InSAR\\Unwrapping",
        description = "Export data and prepare conf file for SNAPHU processing",
        internal = false)
public class SnaphuExportOp extends Operator {

    @SourceProducts(description = "The array of source product of InSAR bands.")
    private Product[] sourceProducts;

    private Product sourceProduct;

    @TargetProduct(description = "The target product that is parsed to SNAPHU.")
    private Product targetProduct;

    @Parameter(description = "The list of source bands.",
            alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {"TOPO", "DEFO", "SMOOTH", "NOSTATCOSTS"},
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "DEFO",
            label = "Statistical-cost mode")
    private String statCostMode = "DEFO";

    @Parameter(valueSet = {"MST", "MCF"},
            description = "Algorithm used for initialization of the wrapped phase values",
            defaultValue = "MST",
            label = "Initial method")
    private String initMethod = "MST";

    @Override
    public void initialize() throws OperatorException {

        if (sourceProducts.length != 2)
            throw new OperatorException("SnaphuExportOp requires two source products.");
        if (sourceBandNames.length == 1)
            throw new OperatorException("Please select max 2 bands, coherence and phase.");
        if (sourceBandNames.length > 2)
            throw new OperatorException("Maximum 2 products can be selected.");

        // assume first sourceProduct is the reference product
        sourceProduct = sourceProducts[0];

        // check whether products are "similar": on geocoding
//        validateSourceProducts();

        try {

            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            // work only if at least 2 bands
            if (sourceBandNames.length == 2) {

                // assume certain logic on band names
                String bandName_0 = sourceBandNames[0].trim().split("::")[0];
                String bandName_1 = sourceBandNames[1].trim().split("::")[0];

                // check whether good units are selected
                String unit_0 = sourceProducts[0].getBand(bandName_0).getUnit();
                String unit_1 = sourceProducts[1].getBand(bandName_1).getUnit();
                boolean check_unit_0 = unit_0.contains(Unit.COHERENCE) || unit_0.contains(Unit.PHASE);
                boolean check_unit_1 = unit_1.contains(Unit.COHERENCE) || unit_1.contains(Unit.PHASE);
                if (!check_unit_0 || !check_unit_1) {
                    throw new OperatorException("Please select bands of coherence and phase unit.");
                }

                // check from which band to pull which band
                List<String> bandNameList_0 = Arrays.asList(sourceProducts[0].getBandNames());
                List<String> bandNameList_1 = Arrays.asList(sourceProducts[1].getBandNames());

                if (bandNameList_0.contains(bandName_0)) {
                    final Band targetBand = ProductUtils.copyBand(bandName_0, sourceProducts[0], targetProduct);
                    targetBand.setSourceImage(sourceProducts[0].getBand(bandName_0).getSourceImage());
                }

                if (bandNameList_1.contains(bandName_1)) {
                    final Band targetBand = ProductUtils.copyBand(bandName_1, sourceProducts[1], targetProduct);
                    targetBand.setSourceImage(sourceProducts[1].getBand(bandName_1).getSourceImage());
                }

                // update metadata with SNAPHU processing flags: the only way to parse info to the writer
                try {
                    final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
                    AbstractMetadata.setAttribute(absTgt, AbstractMetadata.temp_1, statCostMode.toUpperCase());
                    AbstractMetadata.setAttribute(absTgt, AbstractMetadata.temp_2, initMethod.toUpperCase());
                } catch (Throwable e){
                    OperatorUtils.catchOperatorException(getId() + "Metadata of input product is not in the format compatible for SNAPHU export.", e);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

    }

    private void validateSourceProducts() {
        for (Product sourceProduct : getSourceProducts()) {
            if (!targetProduct.isCompatibleProduct(sourceProduct, 1.0E-5f)) {
                throw new OperatorException("Product '" + getSourceProductId(sourceProduct) + "' is not compatible to" +
                        " master product.");
            }
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnaphuExportOp.class);
        }
    }


}