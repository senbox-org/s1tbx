package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.SampleCoding;

import java.util.Random;

public class ExpectedContent {

    @JsonProperty(required = true)
    private String id;
    @JsonProperty
    private Integer sceneWidth;
    @JsonProperty
    private Integer sceneHeight;
    @JsonProperty
    private String startTime;
    @JsonProperty
    private String endTime;
    @JsonProperty
    private ExpectedGeoCoding geoCoding;
    @JsonProperty
    private ExpectedSampleCoding[] flagCodings;
    @JsonProperty
    private ExpectedSampleCoding[] indexCodings;
    @JsonProperty
    private ExpectedBand[] bands;
    @JsonProperty
    private ExpectedMask[] masks;
    @JsonProperty
    private ExpectedMetadata[] metadata;

    ExpectedContent() {
        metadata = new ExpectedMetadata[0];
        flagCodings = new ExpectedSampleCoding[0];
        indexCodings = new ExpectedSampleCoding[0];
        bands = new ExpectedBand[0];
        masks = new ExpectedMask[0];
    }

    public ExpectedContent(Product product, Random random) {
        this();
        this.id = product.getName();
        this.sceneWidth = product.getSceneRasterWidth();
        this.sceneHeight = product.getSceneRasterHeight();
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime != null) {
            this.endTime = endTime.format();
        }
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            this.startTime = startTime.format();
        }
        this.metadata = createExpectedMetadata(product);
        this.geoCoding = createExpectedGeoCoding(product, random);
        this.flagCodings = createExpectedSampleCodings(product.getFlagCodingGroup());
        this.indexCodings = createExpectedSampleCodings(product.getIndexCodingGroup());
        this.bands = createExpectedBands(product, random);
        this.masks = createExpectedMasks(product);
    }

    private ExpectedMetadata[] createExpectedMetadata(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        if (metadataRoot.getNumElements() > 0 ||
            metadataRoot.getNumAttributes() > 0) {
            final ExpectedMetadata[] expectedMetadata = new ExpectedMetadata[2];
            final Random random = new Random();
            for (int i = 0; i < expectedMetadata.length; i++) {
                MetadataElement currentElem = metadataRoot;
                while (currentElem != null && currentElem.getNumElements() > 0) {
                    currentElem = currentElem.getElementAt((int) (currentElem.getNumElements() * random.nextFloat()));
                }
                if (currentElem != null && currentElem.getNumAttributes() > 0) {
                    final MetadataAttribute attributeAt = currentElem.getAttributeAt((int) (currentElem.getNumAttributes() * random.nextFloat()));
                    expectedMetadata[i] = new ExpectedMetadata(attributeAt);
                }
            }
            return expectedMetadata;
        }else {
            return new ExpectedMetadata[0];
        }

    }

    private ExpectedGeoCoding createExpectedGeoCoding(Product product, Random random) {
        if (product.getGeoCoding() != null) {
            return new ExpectedGeoCoding(product, random);
        }
        return null;
    }

    private ExpectedMask[] createExpectedMasks(Product product) {
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        final ExpectedMask[] expectedMasks = new ExpectedMask[maskGroup.getNodeCount()];
        for (int i = 0; i < expectedMasks.length; i++) {
            final Mask mask = maskGroup.get(i);
            expectedMasks[i] = new ExpectedMask(mask);
        }
        return expectedMasks;


    }

    private ExpectedBand[] createExpectedBands(Product product, Random random) {
        final ExpectedBand[] expectedBands = new ExpectedBand[product.getNumBands()];
        for (int i = 0; i < expectedBands.length; i++) {
            expectedBands[i] = new ExpectedBand(product.getBandAt(i), random);
        }
        return expectedBands;
    }

    private ExpectedSampleCoding[] createExpectedSampleCodings(ProductNodeGroup<? extends SampleCoding> sampleCodingGroup) {
        ExpectedSampleCoding[] sampleCodings = new ExpectedSampleCoding[sampleCodingGroup.getNodeCount()];
        for (int i = 0; i < sampleCodings.length; i++) {
            final SampleCoding sampleCoding = sampleCodingGroup.get(i);
            sampleCodings[i] = new ExpectedSampleCoding(sampleCoding);
        }
        return sampleCodings;
    }

    String getId() {
        return id;
    }

    int getSceneWidth() {
        return sceneWidth;
    }

    @JsonIgnoreProperties
    public boolean isSceneWidthSet() {
        return sceneWidth != null;
    }

    int getSceneHeight() {
        return sceneHeight;
    }

    @JsonIgnoreProperties()
    public boolean isSceneHeightSet() {
        return sceneHeight != null;
    }

    String getStartTime() {
        return startTime;
    }

    boolean isStartTimeSet() {
        return startTime != null;
    }

    String getEndTime() {
        return endTime;
    }

    boolean isEndTimeSet() {
        return endTime != null;
    }

    ExpectedGeoCoding getGeoCoding() {
        return geoCoding;
    }

    @JsonIgnoreProperties
    boolean isGeoCodingSet() {
        return geoCoding != null;
    }

    ExpectedSampleCoding[] getFlagCodings() {
        return flagCodings;
    }

    ExpectedSampleCoding[] getIndexCodings() {
        return indexCodings;
    }

    ExpectedBand[] getBands() {
        return bands;
    }

    public ExpectedMask[] getMasks() {
        return masks;
    }

    public ExpectedMetadata[] getMetadata() {
        return metadata;
    }
}
