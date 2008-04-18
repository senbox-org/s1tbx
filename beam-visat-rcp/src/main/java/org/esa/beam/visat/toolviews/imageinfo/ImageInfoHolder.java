package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;

public interface ImageInfoHolder {
    ImageInfo getCurrentImageInfo();
    void setCurrentImageInfo(ImageInfo imageInfo);

}
