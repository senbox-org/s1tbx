package org.esa.beam.framework.datamodel;

/**
 * @deprecated replaced by {@link ConvolutionFilterBand}
 */
public class ConvolutedBand extends ConvolutionFilterBand {

    public ConvolutedBand(String name, RasterDataNode source, Kernel kernel) {
        super(name, source, kernel);
    }
}
