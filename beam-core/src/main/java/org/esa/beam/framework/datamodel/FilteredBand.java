package org.esa.beam.framework.datamodel;

/**
 * @deprecated replaced by {@link FilterBand}
 */
public class FilteredBand extends Band {

    protected RasterDataNode _source;

    public FilteredBand(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
        setSynthetic(true);
    }

    public RasterDataNode getSource() {
        return _source;
    }
}
