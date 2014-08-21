package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

/**
 * @author Norman Fomferra
 */
class FormModel {
    private ProductSceneView productSceneView;
    private ImageInfo modifiedImageInfo;

    public ProductSceneView getProductSceneView() {
        return productSceneView;
    }

    public void setProductSceneView(ProductSceneView productSceneView) {
        this.productSceneView = productSceneView;
    }

    public RasterDataNode getRaster() {
        return getProductSceneView().getRaster();
    }

    public RasterDataNode[] getRasters() {
            return getProductSceneView().getRasters();
        }


    public void setRasters(RasterDataNode[] rasters) {
        getProductSceneView().setRasters(rasters);
    }

    public ImageInfo getOriginalImageInfo() {
        return getProductSceneView().getImageInfo();
    }

    /**
     * @return The image info being edited.
     */
    public ImageInfo getModifiedImageInfo() {
        return modifiedImageInfo;
    }

    /**
     * Sets modifiedImageInfo to a copy of the given imageInfo.
     * @param imageInfo The image info from which a copy is made which will then be edited.
     */
    public void setModifiedImageInfo(ImageInfo imageInfo) {
        this.modifiedImageInfo = imageInfo.createDeepCopy();
    }

    public void applyModifiedImageInfo() {
        getProductSceneView().setImageInfo(getModifiedImageInfo());
    }

    public String getModelName() {
        return getProductSceneView().getSceneName();
    }

    public Product getProduct() {
        return getProductSceneView().getProduct();
    }

    public boolean isValid() {
        return getProductSceneView() != null;
    }

    public boolean isContinuous3BandImage() {
        return isValid() && getProductSceneView().isRGB();
    }

    public boolean isContinuous1BandImage() {
        return isValid() && getRaster() instanceof Band && ((Band) getRaster()).getIndexCoding() == null;
    }

    public boolean isDiscrete1BandImage() {
        return isValid() && getRaster() instanceof Band && ((Band) getRaster()).getIndexCoding() != null;
    }

    public boolean canUseHistogramMatching() {
        return true;
    }

    // todo - bit weird to let the mode return a form, try to change this later, e.g. adapter pattern (nf)
    public void modifyMoreOptionsForm(MoreOptionsForm moreOptionsForm) {
    }
}
