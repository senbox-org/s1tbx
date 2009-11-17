package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicFormModel {

    private PropertyContainer container;
    public static final String PROPERTY_SOURCE_PRODUCT_FILES = "sourceProductFiles";
    public static final String PROPERTY_UPDATE_PRODUCT = "updateProduct";
    public static final String PROPERTY_UPDATE_MODE = "updateMode";
    private Product refProduct;
    private File refProductFile;

    MosaicFormModel() {
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic", parameterMap);
        final PropertyDescriptor sourceFilesDescriptor = new PropertyDescriptor(PROPERTY_SOURCE_PRODUCT_FILES,
                                                                                new File[0].getClass());
        container.addProperty(new Property(sourceFilesDescriptor,
                                           new MapEntryAccessor(parameterMap, PROPERTY_SOURCE_PRODUCT_FILES)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_PRODUCT, Product.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_PRODUCT)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_MODE, Boolean.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_MODE)));
        container.setValue(PROPERTY_UPDATE_MODE, false);
        try {
            container.setDefaultValues();
        } catch (ValidationException ignore) {
        }

    }

    public PropertyContainer getPropertyContainer() {
        return container;
    }

    public Object getPropertyValue(String propertyName) {
        return container.getValue(propertyName);
    }

    public void setPropertyValue(String propertyName, Object value) {
        container.setValue(propertyName, value);
    }

    public Product getReferenceProduct() throws IOException {
        if (container.getValue(PROPERTY_SOURCE_PRODUCT_FILES) != null) {
            final File[] files = (File[]) container.getValue(PROPERTY_SOURCE_PRODUCT_FILES);
            if (files.length > 0) {
                try {
                    if (!files[0].equals(refProductFile)) {
                        refProductFile = files[0];
                        if (refProduct != null) {
                            refProduct.dispose();
                            refProduct = null;
                        }
                        refProduct = ProductIO.readProduct(refProductFile, null);
                    }
                } catch (IOException e) {
                    final String msg = String.format("Cannot read product '%s'", files[0].getPath());
                    throw new IOException(msg, e);
                }
            } else {
                if (refProduct != null) {
                    refProduct.dispose();
                    refProduct = null;
                }
            }
        }
        if (refProduct == null) {
            final String msg = String.format("No reference product available.");
            throw new IOException(msg);
        }
        return refProduct;
    }

    public Product getBoundaryProduct() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = getCrs();
        if (mapCRS != null) {
            final double pixelSizeX = (Double) getPropertyValue("pixelSizeX");
            final double pixelSizeY = (Double) getPropertyValue("pixelSizeY");
            final GeneralEnvelope generalEnvelope = getGeoEnvelope();

            final Envelope targetEnvelope = CRS.transform(generalEnvelope, mapCRS);
            final int sceneRasterWidth = MathUtils.floorInt(targetEnvelope.getSpan(0) / pixelSizeX);
            final int sceneRasterHeight = MathUtils.floorInt(targetEnvelope.getSpan(1) / pixelSizeY);
            final Product outputProduct = new Product("mosaic", "MosaicBounds",
                                                      sceneRasterWidth, sceneRasterHeight);
            final Rectangle imageRect = new Rectangle(0, 0, sceneRasterWidth, sceneRasterHeight);
            final AffineTransform i2mTransform = new AffineTransform();
            i2mTransform.translate(targetEnvelope.getMinimum(0), targetEnvelope.getMinimum(1));
            i2mTransform.scale(pixelSizeX, pixelSizeY);
            i2mTransform.translate(-0.5, -0.5);
            outputProduct.setGeoCoding(new CrsGeoCoding(mapCRS, imageRect, i2mTransform));
            return outputProduct;
        }
        return null;
    }
    
    void setWkt(CoordinateReferenceSystem crs) {
        if (crs != null) {
            setPropertyValue("wkt", crs.toWKT());
            if (getPropertyValue("epsgCode") != null) {
                // clear default epsgCode, so wkt has precedence
                setPropertyValue("epsgCode", null);
            }
        }
    }

    private CoordinateReferenceSystem getCrs() throws FactoryException {
        final String crsCode = (String) getPropertyValue("epsgCode");
        if (crsCode != null) {
            return CRS.decode(crsCode, true);
        }
        final String wkt = (String) getPropertyValue("wkt");
        if (wkt != null) {
            return CRS.parseWKT(wkt);
        }

        return null;
    }

    GeneralEnvelope getGeoEnvelope() {
        final double west = (Double) getPropertyValue("westBound");
        final double north = (Double) getPropertyValue("northBound");
        final double east = (Double) getPropertyValue("eastBound");
        final double south = (Double) getPropertyValue("southBound");
        final Rectangle2D.Double geoBounds = new Rectangle2D.Double();
        geoBounds.setFrameFromDiagonal(west, north, east, south);
        final GeneralEnvelope generalEnvelope = new GeneralEnvelope(geoBounds);
        generalEnvelope.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
        return generalEnvelope;
    }


}
