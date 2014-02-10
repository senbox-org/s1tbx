package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a KML file "&lt;name&gt;-overview.kml" for each product and for each feature with name &lt;name&gt; of type RenderedImage.
 *
 * @author Norman Fomferra
 */
public class KmlPatchWriter implements PatchWriter {

    private final File productTargetDir;
    private List<KmlWriter> kmlWriters;

    public KmlPatchWriter(File productTargetDir) throws IOException {
        this.productTargetDir = productTargetDir;
    }

    @Override
    public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
        kmlWriters = new ArrayList<KmlWriter>();
        for (FeatureType featureType : featureTypes) {
            if (PatchWriterHelpers.isImageFeatureType(featureType)) {
                KmlWriter kmlWriter = new KmlWriter(new FileWriter(new File(productTargetDir, featureType.getName() + "-overview.kml")),
                                                    sourceProduct.getName(),
                                                    "RGB tiles from reflectances of " + sourceProduct.getName());
                kmlWriters.add(kmlWriter);
            }
        }
    }

    @Override
    public void writePatch(Patch patch, Feature... features) throws IOException {
        final Product patchProduct = patch.getPatchProduct();
        final GeoCoding geoCoding = patchProduct.getGeoCoding();
        int kmlWriterIndex = 0;
        for (Feature feature : features) {
            if (PatchWriterHelpers.isImageFeatureType(feature.getFeatureType())) {
                float w = patchProduct.getSceneRasterWidth();
                float h = patchProduct.getSceneRasterHeight();
                // quadPositions: counter clockwise lon,lat coordinates starting at lower-left
                GeoPos[] quadPositions = new GeoPos[]{
                        geoCoding.getGeoPos(new PixelPos(0, h), null),
                        geoCoding.getGeoPos(new PixelPos(w, h), null),
                        geoCoding.getGeoPos(new PixelPos(w, 0), null),
                        geoCoding.getGeoPos(new PixelPos(0, 0), null),
                };
                String imagePath = patch.getPatchName() + "/" + feature.getName() + ".png";
                kmlWriters.get(kmlWriterIndex).writeGroundOverlayEx(patch.getPatchName(), quadPositions, imagePath);
                kmlWriterIndex++;
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (KmlWriter kmlWriter : kmlWriters) {
            kmlWriter.close();
        }
    }

}
