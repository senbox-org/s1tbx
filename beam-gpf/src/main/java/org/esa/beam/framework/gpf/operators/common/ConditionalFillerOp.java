package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConditionalFillerOp extends AbstractOperator implements ParameterConverter {

    public static class Configuration {

        private String flagName;
        private List<BandDesc> bands;

        public Configuration() {
            bands = new ArrayList<BandDesc>();
        }
    }

    @TargetProduct
    private Product targetProduct;
    @Parameter
    private Configuration config;

    private Map<String, List<BandSourceData>> bandSourceDataMap;
    private Map<String, Band> outputBands;
    private Map<String, Integer> defaultFlagValue;
    private Band flagBand;


    public ConditionalFillerOp(OperatorSpi spi) {
        super(spi);
        config = new Configuration();

        bandSourceDataMap = new HashMap<String, List<BandSourceData>>();
        outputBands = new HashMap<String, Band>();
        defaultFlagValue = new HashMap<String, Integer>();
    }

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom parameterDom) throws OperatorException {
        XStream xStream = new XStream();
        xStream.setClassLoader(this.getClass().getClassLoader());
        xStream.alias(parameterDom.getName(), Configuration.class);
        xStream.alias("band", BandDesc.class);
        xStream.addImplicitCollection(Configuration.class, "bands");
        xStream.alias("bandSource", BandSourceDesc.class);
        xStream.addImplicitCollection(BandDesc.class, "bandSources");
        xStream.unmarshal(new XppDomReader(parameterDom), config);
    }

    @Override
    public Product initialize(ProgressMonitor pm) throws OperatorException {

        BandDesc firstBandDesc = config.bands.get(0);
        BandSourceDesc firstBandSourceDesc = firstBandDesc.bandSources.get(0);
        Product firstProduct = getContext().getSourceProduct(firstBandSourceDesc.key);
        final int width = firstProduct.getSceneRasterWidth();
        final int height = firstProduct.getSceneRasterHeight();

        targetProduct = new Product("filled", "filled", width, height);
        for (BandDesc bandDesc : config.bands) {
            Band band = targetProduct.addBand(bandDesc.name, ProductData.TYPE_FLOAT32);
            outputBands.put(bandDesc.name, band);
        }
        flagBand = targetProduct.addBand(config.flagName, ProductData.TYPE_INT8);
        FlagCoding flagCoding = createFlagCoding();
        flagBand.setFlagCoding(flagCoding);
        targetProduct.addFlagCoding(flagCoding);
        return targetProduct;
    }

    private FlagCoding createFlagCoding() {
        final FlagCoding flagCoding = new FlagCoding(config.flagName);

        int i = 0;
        for (BandDesc bandDesc : config.bands) {
            for (BandSourceDesc bandSourceDesc : bandDesc.bandSources) {
                final String name = bandDesc.name + "___" + bandSourceDesc.key;
                final int flagMask = 1 << i++;
                final String description = "Value from Product (" + bandSourceDesc.key + ") Band(" + bandDesc.name + ") used.";
                flagCoding.addFlag(name, flagMask, description);
            }
            final String name = bandDesc.name + "___DEFAULT";
            final int flagMask = 1 << i++;
            final String description = "Default value (" + bandDesc.defaultValue + ") used.";
            flagCoding.addFlag(name, flagMask, description);
            defaultFlagValue.put(bandDesc.name, flagMask);
        }


        return flagCoding;
    }

    public void initSourceRetriever() {
        int flagIndex = 0;
        for (BandDesc bandDesc : config.bands) {
            List<BandSourceData> bandDataList = new ArrayList<BandSourceData>();
            bandSourceDataMap.put(bandDesc.name, bandDataList);

            for (BandSourceDesc bandSourceDesc : bandDesc.bandSources) {
                BandSourceData bandSourceData = new BandSourceData();
                bandDataList.add(bandSourceData);
                Product product = getContext().getSourceProduct(bandSourceDesc.key);
                Band band = product.getBand(bandSourceDesc.band);

//                bandSourceData.data = dataRetriever.connectFloat(band);
//                bandSourceData.valid = dataRetriever.connectBooleanExpression(product, bandSourceDesc.validExp);
                bandSourceData.flag = (byte) (1 << flagIndex++);
            }
            flagIndex++; //for the default flag
        }
    }

    @Override
    public void computeAllBands(Rectangle rectangle, ProgressMonitor pm) throws OperatorException {

        final int size = rectangle.height * rectangle.width;
        pm.beginTask("Processing frame...", 1 + size);
        try {
            byte[] flagData = (byte[]) getRaster(flagBand, rectangle).getDataBuffer().getElems();

            for (BandDesc bandDesc : config.bands) {
                final String bandName = bandDesc.name;

                List<BandSourceData> bandDataList = bandSourceDataMap.get(bandName);

                Band band = outputBands.get(bandName);
                float[] outData = (float[]) getRaster(band, rectangle).getDataBuffer().getElems();

                final float deaultValue = Float.parseFloat(bandDesc.defaultValue);
                byte defaultFlag = defaultFlagValue.get(bandName).byteValue();

                indexLoop:
                for (int i = 0; i < size; i++) {
                    for (BandSourceData bandSourceData : bandDataList) {
                        if (bandSourceData.valid[i]) {
                            outData[i] = bandSourceData.data[i];
                            flagData[i] += bandSourceData.flag;
                            continue indexLoop;
                        }
                    }
                    outData[i] = deaultValue;
                    flagData[i] += defaultFlag;
                }
            }

            pm.worked(1);
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    public class BandDesc {
        String name;
        String defaultValue;
        private List<BandSourceDesc> bandSources;

        public BandDesc() {
            bandSources = new ArrayList<BandSourceDesc>();
        }
    }

    public class BandSourceDesc {
        String key;
        String band;
        String validExp;
    }

    private class BandSourceData {
        boolean[] valid;
        float[] data;
        byte flag;
    }


    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(ConditionalFillerOp.class, "ConditionalFiller");
        }
    }
}
