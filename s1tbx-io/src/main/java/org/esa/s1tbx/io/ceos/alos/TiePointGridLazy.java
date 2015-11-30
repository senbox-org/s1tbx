package org.esa.s1tbx.io.ceos.alos;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;

/**
 * Created by lveci on 25/11/2015.
 */
public class TiePointGridLazy extends TiePointGrid {

    private boolean hasData;

    /**
     * Constructs a new <code>TiePointGrid</code> with the given tie point grid properties.
     *
     * @param name         the name of the new object
     * @param gridWidth    the width of the tie-point grid in pixels
     * @param gridHeight   the height of the tie-point grid in pixels
     * @param offsetX      the X co-ordinate of the first (upper-left) tie-point in pixels
     * @param offsetY      the Y co-ordinate of the first (upper-left) tie-point in pixels
     * @param subSamplingX the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to. Must not be less than one.
     * @param subSamplingY the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to. Must not be less than one.
     * @param tiePoints    the tie-point data values, must be an array of the size <code>gridWidth * gridHeight</code>
     */
    public TiePointGridLazy(String name,
                        int gridWidth,
                        int gridHeight,
                        double offsetX,
                        double offsetY,
                        double subSamplingX,
                        double subSamplingY,
                        float[] tiePoints) {
        super(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints, DISCONT_NONE);
        hasData = false;
    }

    @Override
    public int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) {
        if(!hasData) {
            try {
                // read all or nothing
                getProductReader().readTiePointGridRasterData(this, 0, 0, getGridWidth(), getGridHeight(), getGridData(), pm);
                hasData = true;
            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to load TPG: "+e.getMessage());
            }
        }
        return super.getPixels(x, y, w, h, pixels, pm);
    }

    @Override
    public double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) {
        if(!hasData) {
            try {
                // read all or nothing
                getProductReader().readTiePointGridRasterData(this, 0, 0, getGridWidth(), getGridHeight(), getGridData(), pm);
                hasData = true;
            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to load TPG: "+e.getMessage());
            }
        }
        return super.getPixels(x, y, w, h, pixels, pm);
    }

    @Override
    public float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) {
        if(!hasData) {
            try {
                // read all or nothing
                getProductReader().readTiePointGridRasterData(this, 0, 0, getGridWidth(), getGridHeight(), getGridData(), pm);
                hasData = true;
            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to load TPG: "+e.getMessage());
            }
        }
        return super.getPixels(x, y, w, h, pixels, pm);
    }
}
