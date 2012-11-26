package org.esa.beam.statistics.percentile.interpolated;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

public class Utils {

    public static Area createProductArea(Product product) {
        GeneralPath[] boundary = ProductUtils.createGeoBoundaryPaths(product);
        Area area = new Area();
        for (GeneralPath generalPath : boundary) {
            area.add(new Area(generalPath));
        }
        return area;
    }
}
