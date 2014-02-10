package org.esa.pfa.db;

import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.FexOperator;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * Created by Norman on 31.01.14.
 */
public class DatasetDescriptorTest {

    @Test
    public void testWriteAlgalBloomDs() throws Exception {
        final String R_EXPR = "log(0.05 + 0.35 * reflec_2 + 0.60 * reflec_5 + reflec_6 + 0.13 * reflec_7)";
        final String G_EXPR = "log(0.05 + 0.21 * reflec_3 + 0.50 * reflec_4 + reflec_5 + 0.38 * reflec_6)";
        final String B_EXPR = "log(0.05 + 0.21 * reflec_1 + 1.75 * reflec_2 + 0.47 * reflec_3 + 0.16 * reflec_4)";
        double minSampleFlh = 0;
        double maxSampleFlh = 1;
        double minSampleMci = 0;
        double maxSampleMci = 1;
        double minSampleChl = 0;
        double maxSampleChl = 1;
        FeatureType[] featureTypes = new FeatureType[]{
                        /*00*/ new FeatureType("patch", "Patch product", Product.class),
                        /*01*/ new FeatureType("rgb1_ql", "RGB quicklook for TOA reflectances (fixed range)", RenderedImage.class),
                        /*02*/ new FeatureType("rgb2_ql", "RGB quicklook for TOA reflectances (dynamic range, ROI only)", RenderedImage.class),
                        /*03*/ new FeatureType("flh_ql", "Grey-scale quicklook for 'flh' [" + minSampleFlh + ", " + maxSampleFlh + "]", RenderedImage.class),
                        /*04*/ new FeatureType("mci_ql", "Grey-scale quicklook for 'mci' [" + minSampleMci + ", " + maxSampleMci + "]", RenderedImage.class),
                        /*05*/ new FeatureType("chl_ql", "Grey-scale quicklook for 'chl' [" + minSampleChl + ", " + maxSampleChl + "]", RenderedImage.class),
                        /*06*/ new FeatureType("flh", "Fluorescence Line Height", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*07*/ new FeatureType("mci", "Maximum Chlorophyll Index", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*08*/ new FeatureType("chl", "Chlorophyll Concentration", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*09*/ new FeatureType("red", "Red channel (" + R_EXPR + ")", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*10*/ new FeatureType("green", "Green channel (" + G_EXPR + ")", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*11*/ new FeatureType("blue", "Blue channel (" + B_EXPR + ")", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*12*/ new FeatureType("coast_dist", "Distance from next coast pixel (km)", FexOperator.STX_ATTRIBUTE_TYPES),
                        /*13*/ new FeatureType("flh_hg_pixels", "FLH high-gradient pixel ratio", Double.class),
                        /*14*/ new FeatureType("valid_pixels", "Ratio of valid pixels in patch [0, 1]", Double.class),
                        /*15*/ new FeatureType("fractal_index", "Fractal index estimation [1, 2]", Double.class),
                        /*16*/ new FeatureType("clumpiness", "A clumpiness index [-1, 1]", Double.class),
        };

        DatasetDescriptor ad = new DatasetDescriptor("test_ds", "1.0", "Wraw!", featureTypes);
        StringWriter writer = new StringWriter();
        ad.write(writer);



        assertEquals(
                "<DatasetDescriptor>\n" +
                "  <name>test_ds</name>\n" +
                "  <version>1.0</version>\n" +
                "  <description>Wraw!</description>\n" +
                "  <featureTypes>\n" +
                "    <FeatureType>\n" +
                "      <name>patch</name>\n" +
                "      <description>Patch product</description>\n" +
                "      <valueType>org.esa.beam.framework.datamodel.Product</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>rgb1_ql</name>\n" +
                "      <description>RGB quicklook for TOA reflectances (fixed range)</description>\n" +
                "      <valueType>java.awt.image.RenderedImage</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>rgb2_ql</name>\n" +
                "      <description>RGB quicklook for TOA reflectances (dynamic range, ROI only)</description>\n" +
                "      <valueType>java.awt.image.RenderedImage</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>flh_ql</name>\n" +
                "      <description>Grey-scale quicklook for &apos;flh&apos; [0.0, 1.0]</description>\n" +
                "      <valueType>java.awt.image.RenderedImage</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>mci_ql</name>\n" +
                "      <description>Grey-scale quicklook for &apos;mci&apos; [0.0, 1.0]</description>\n" +
                "      <valueType>java.awt.image.RenderedImage</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>chl_ql</name>\n" +
                "      <description>Grey-scale quicklook for &apos;chl&apos; [0.0, 1.0]</description>\n" +
                "      <valueType>java.awt.image.RenderedImage</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>flh</name>\n" +
                "      <description>Fluorescence Line Height</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes>\n" +
                "        <AttributeType>\n" +
                "          <name>mean</name>\n" +
                "          <description>Mean value of valid feature pixels</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>stdev</name>\n" +
                "          <description>Standard deviation of valid feature pixels</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>cvar</name>\n" +
                "          <description>Coefficient of variation of valid feature pixels</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>min</name>\n" +
                "          <description>Minimim value of valid feature pixels</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>max</name>\n" +
                "          <description>Maximum value of valid feature pixels</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>p10</name>\n" +
                "          <description>The threshold such that 10% of the sample values are below the threshold</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>p50</name>\n" +
                "          <description>The threshold such that 50% of the sample values are below the threshold (=median)</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>p90</name>\n" +
                "          <description>The threshold such that 90% of the sample values are below the threshold</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>skewness</name>\n" +
                "          <description>A measure of the extent to which the histogram &quot;leans&quot; to one side of the mean. The skewness value can be positive or negative, or even undefined.</description>\n" +
                "          <valueType>java.lang.Double</valueType>\n" +
                "        </AttributeType>\n" +
                "        <AttributeType>\n" +
                "          <name>count</name>\n" +
                "          <description>Sample count (number of valid feature pixels)</description>\n" +
                "          <valueType>java.lang.Integer</valueType>\n" +
                "        </AttributeType>\n" +
                "      </attributeTypes>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>mci</name>\n" +
                "      <description>Maximum Chlorophyll Index</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>chl</name>\n" +
                "      <description>Chlorophyll Concentration</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>red</name>\n" +
                "      <description>Red channel (log(0.05 + 0.35 * reflec_2 + 0.60 * reflec_5 + reflec_6 + 0.13 * reflec_7))</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>green</name>\n" +
                "      <description>Green channel (log(0.05 + 0.21 * reflec_3 + 0.50 * reflec_4 + reflec_5 + 0.38 * reflec_6))</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>blue</name>\n" +
                "      <description>Blue channel (log(0.05 + 0.21 * reflec_1 + 1.75 * reflec_2 + 0.47 * reflec_3 + 0.16 * reflec_4))</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>coast_dist</name>\n" +
                "      <description>Distance from next coast pixel (km)</description>\n" +
                "      <valueType>void</valueType>\n" +
                "      <attributeTypes reference=\"../../FeatureType[7]/attributeTypes\"/>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>flh_hg_pixels</name>\n" +
                "      <description>FLH high-gradient pixel ratio</description>\n" +
                "      <valueType>java.lang.Double</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>valid_pixels</name>\n" +
                "      <description>Ratio of valid pixels in patch [0, 1]</description>\n" +
                "      <valueType>java.lang.Double</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>fractal_index</name>\n" +
                "      <description>Fractal index estimation [1, 2]</description>\n" +
                "      <valueType>java.lang.Double</valueType>\n" +
                "    </FeatureType>\n" +
                "    <FeatureType>\n" +
                "      <name>clumpiness</name>\n" +
                "      <description>A clumpiness index [-1, 1]</description>\n" +
                "      <valueType>java.lang.Double</valueType>\n" +
                "    </FeatureType>\n" +
                "  </featureTypes>\n" +
                "</DatasetDescriptor>", writer.toString());

    }


    @Test
    public void testReadAlgalBloomIO() throws Exception {

        File file = new File(getClass().getResource("ds-descriptor.xml").toURI());

        DatasetDescriptor datasetDescriptor = DatasetDescriptor.read(file);

        StringWriter writer = new StringWriter();
        datasetDescriptor.write(writer);
        String actualXml = writer.toString();

        int length = (int) file.length();
        char[] cbuf = new char[length];
        length = new FileReader(file).read(cbuf);
        String expectedXml = new String(cbuf, 0, length);

        assertEquals(expectedXml, actualXml);
    }
}
