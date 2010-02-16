package org.esa.beam.dataio.rtp;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("product")
class ProductDescriptor {
    private String name;
    private String type;
    private String description;
    private int width;
    private int height;
    @XStreamAlias("bands")
    private BandDescriptor[] bandDescriptors;

    ProductDescriptor() {
    }

    ProductDescriptor(String name, String type, int width, int height, BandDescriptor[] bandDescriptors, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.width = width;
        this.height = height;
        this.bandDescriptors = bandDescriptors;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BandDescriptor[] getBandDescriptors() {
        return bandDescriptors;
    }

    public String getDescription() {
        return description;
    }
}
