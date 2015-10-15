package org.esa.snap.core.datamodel.multisize;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class TestProductCreator {

    private enum GeoCodingType {CRS, TIE_POINT}
    private enum Size {ONE_SIZE, TWO_SIZES, THREE_SIZES}
    private enum Offset {SAME_OFFSET, CENTERING, NON_OVERLAPPING}

    private static Product[] createTestProducts() throws FactoryException, TransformException {
        List<Product> productList = new ArrayList<>();
        final GeoCodingType[] geoCodingTypes = GeoCodingType.values();
        final Size[] sizes = Size.values();
        final Offset[] offsets = Offset.values();
        for (GeoCodingType geoCodingType : geoCodingTypes) {
            for (Size size : sizes)
                for (Offset offset : offsets) {
                    productList.add(createTestProduct(geoCodingType, size, offset));
                }
        }
        return productList.toArray(new Product[productList.size()]);
    }

    private static Product createTestProduct(GeoCodingType geoCodingType, Size size, Offset offset) throws FactoryException, TransformException {
        int imageSize = 60;
        float pixelSize = 0.1f;
        final int scale = getScale(size);
        Band[] bands = new Band[3];
        final Product product = new Product("product", "type");
        for (int i = 0; i < 3; i++) {
            int bandImageSize = imageSize / ((i % scale) + 1);
            bands[i] = new Band("band_" + (i + 1), ProductData.TYPE_INT8, bandImageSize, bandImageSize);
            final float offsetFactor = getOffsetFactor(size, offset, i);
            if (geoCodingType == GeoCodingType.CRS) {
                bands[i].setGeoCoding(createCrsGeoCoding(bandImageSize, pixelSize, offsetFactor));
            } else {
                bands[i].setGeoCoding(createTiePointGeocoding(product, pixelSize, offsetFactor, scale, bandImageSize));
            }
        }
        return product;
    }

    private static float getOffsetFactor(Size size, Offset offset, int bandIndex) {
        if (offset == Offset.SAME_OFFSET) {
            return 0;
        } else if (bandIndex == 0) {
            return 0;
        } else if (offset == Offset.CENTERING) {
            if (bandIndex == 1) {
                return 0.25f;
            } else if (size == Size.THREE_SIZES) {
                return (1/3);
            } else {
                return 0;
            }
        } else {
            if (bandIndex == 1) {
                return 1;
            } else {
                return 1.5f;
            }
        }
    }

    private static int getScale(Size size) {
        if (size == Size.ONE_SIZE) {
            return 1;
        } else if (size == Size.TWO_SIZES) {
            return 2;
        } else {
            return 3;
        }
    }

    private static GeoCoding createCrsGeoCoding(int widthAndHeight, float pixelSize, float offsetFactor) throws FactoryException, TransformException {
        float offset = pixelSize * offsetFactor;
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, widthAndHeight, widthAndHeight, 50 + offset, 50 + offset, pixelSize, pixelSize);
    }

    private static GeoCoding createTiePointGeocoding(Product product, float pixelSize, float offsetFactor, int scale, int bandImageSize) {
        int gridWidthAndHeight = 6 / scale;
        int numberOfTiePoints = gridWidthAndHeight * gridWidthAndHeight;
        float[] latPoints = new float[numberOfTiePoints];
        float[] lonPoints = new float[numberOfTiePoints];
        float offset = pixelSize * offsetFactor;
        for (int j = 0; j < numberOfTiePoints; j++) {
            latPoints[j] = 50 + offset + (j / gridWidthAndHeight) * pixelSize;
            lonPoints[j] = 50 + offset + (j % gridWidthAndHeight) * pixelSize;
        }
        double subSampling = bandImageSize / gridWidthAndHeight;
        final TiePointGrid latGrid = new TiePointGrid("lat_" + gridWidthAndHeight, gridWidthAndHeight, gridWidthAndHeight, 0, 0, subSampling, subSampling, latPoints);
        final TiePointGrid lonGrid = new TiePointGrid("lon_" + gridWidthAndHeight, gridWidthAndHeight, gridWidthAndHeight, 0, 0, subSampling, subSampling, lonPoints);
        final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
        if (!product.containsTiePointGrid(latGrid.getName())) {
            product.addTiePointGrid(latGrid);
        }
        if (!product.containsTiePointGrid(lonGrid.getName())) {
            product.addTiePointGrid(lonGrid);
        }
        return tiePointGeoCoding;
    }

}
