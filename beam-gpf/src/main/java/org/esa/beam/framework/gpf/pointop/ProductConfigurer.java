package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

/**
 * A {@code ProductConfigurer} is used to configure a target product with respect to a current source product
 * which serves as a template.
 * Various {@code copy...()} methods may be used to copy parts of the source product into the target product.
 * <p/>
 * This interface is not intended to be implemented by clients.
 *
 * @author Olaf Danne
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since BEAM 4.9
 */
public interface ProductConfigurer {

    Product getSourceProduct();

    void setSourceProduct(Product sourceProduct);

    Product getTargetProduct();

    void copyMetadata();

    void copyTimeCoding();

    void copyGeoCoding();

    void copyTiePointGrids(String... gridName);

    void copyBands(String... bandName);

    void copyVectorData();

    Band addBand(String name, int dataType);

    Band addBand(String name, int dataType, double noDataValue);

    Band addBand(String name, String expression);

    Band addBand(String name, String expression, double noDataValue);
}
