package org.esa.beam.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * todo - add API doc
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface GpfOp {
    Product init();

    void computeRect(Map<Band, WritableRaster> destTiles, Rectangle destRect);
}
