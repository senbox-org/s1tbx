package org.esa.pfa.fe.op;

import org.esa.beam.framework.datamodel.Product;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman Fomferra
 */
public final class Patch {

    private final int patchX;
    private final int patchY;
    private final Rectangle patchRegion;
    private final Product patchProduct;
    private final String patchName;
    private final List<Feature> featureList = new ArrayList<Feature>(10);

    private final int uid;
    private static int uidCnt = 0;

    private int label = -1;
    private double confidence;

    private String pathOnServer;
    private BufferedImage image = null;

    public Patch(int patchX, int patchY, Rectangle patchRegion, Product patchProduct) {
        uid = createUniqueID();
        this.patchX = patchX;
        this.patchY = patchY;
        this.patchName = String.format("x%02dy%02d", patchX, patchY);
        this.patchRegion = patchRegion;
        this.patchProduct = patchProduct;
    }

    private synchronized int createUniqueID() {
        return uidCnt++;
    }

    public int getID() {
        return uid;
    }

    public String getPatchName() {
        return patchName;
    }

    public int getPatchX() {
        return patchX;
    }

    public int getPatchY() {
        return patchY;
    }

    public Rectangle getPatchRegion() {
        return patchRegion;
    }

    public Product getPatchProduct() {
        return patchProduct;
    }

    public String getPathOnServer() {
        return pathOnServer;
    }

    public void setPathOnServer(final String path) {
        pathOnServer = path;
    }

    public void setImage(BufferedImage img) {
        image = img;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void addFeature(final Feature fea) {
        featureList.add(fea);
    }

    public Feature[] getFeatures() {
        return featureList.toArray(new Feature[featureList.size()]);
    }

    public void setConfidence(final double confidence) {
        this.confidence = confidence;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setLabel(final int label) {
        this.label = label;
    }

    public int getLabel() {
        return label;
    }
}
