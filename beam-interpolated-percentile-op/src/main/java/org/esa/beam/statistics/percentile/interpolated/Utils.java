package org.esa.beam.statistics.percentile.interpolated;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

public class Utils {

    public static Area createProductArea(Product targetProduct) {
        GeneralPath[] targetBoundary = ProductUtils.createGeoBoundaryPaths(targetProduct);
        Area targetArea = new Area();
        for (GeneralPath generalPath : targetBoundary) {
            targetArea.add(new Area(generalPath));
        }
        return targetArea;
    }
}
