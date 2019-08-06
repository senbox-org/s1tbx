package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import java.awt.*;
import java.util.Map;

/**
 * Interface for all SM dielectric models.
 * The methods are intended to be implemented or overridden.
 */
public interface DielectricModel {

    void initialize() throws OperatorException;

    void computeTileStack(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final ProgressMonitor pm)
            throws OperatorException;
}
