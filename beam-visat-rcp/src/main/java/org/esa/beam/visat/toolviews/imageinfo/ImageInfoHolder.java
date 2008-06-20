package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;

interface ImageInfoHolder {
    // todo rename to getImageInfo()
    ImageInfo getCurrentImageInfo();
    // todo rename to setImageInfo()
    void setCurrentImageInfo(ImageInfo imageInfo);
}
