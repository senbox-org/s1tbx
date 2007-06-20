/*
 * $Id: AngleTiePointGrid.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.math.MathUtils;

/**
 * @deprecated use discontinuity mode of (@link #org.esa.beam.framework.datamodel.TiePointGrid) to handle cyclic
 *             behaviour.
 */
public class AngleTiePointGrid extends TiePointGrid {

    private TiePointGrid _sinGrid;
    private TiePointGrid _cosGrid;
    private float _offset;
    private float _scale;

    public AngleTiePointGrid(TiePointGrid base,
                             float offset,
                             float scale) {
        super(base.getName(),
              base.getRasterWidth(),
              base.getRasterHeight(),
              base.getOffsetX(),
              base.getOffsetY(),
              base.getSubSamplingX(),
              base.getSubSamplingY(),
              base.getTiePoints());
        _offset = offset;
        _scale = scale;
    }

    @Override
    public float getPixelFloat(int x, int y) {
        if (isNotInit()) {
            init();
        }
        final float sinAngle = _sinGrid.getPixelFloat(x, y);
        final float cosAngle = _cosGrid.getPixelFloat(x, y);
        final float v = MathUtils.RTOD_F * (float) Math.atan2(sinAngle, cosAngle);
        return _offset + _scale * v;
    }

    @Override
    public float[] getPixels(int x0, int y0, int w, int h, float[] pixels, ProgressMonitor pm) {
        if (isNotInit()) {
            init();
        }
        if (pixels == null) {
            pixels = new float[w * h];
        }
        int i = 0;
        pm.beginTask("Retrieving pixels...", h);
        try {
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {
                    pixels[i] = getPixelFloat(x, y);
                    i++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return pixels;
    }

    private boolean isNotInit() {
        return _sinGrid == null;
    }

    private void init() {
        TiePointGrid base = this;
        final float[] tiePoints = base.getTiePoints();
        final float[] sinTiePoints = new float[tiePoints.length];
        final float[] cosTiePoints = new float[tiePoints.length];
        for (int i = 0; i < tiePoints.length; i++) {
            float tiePoint = tiePoints[i];
            sinTiePoints[i] = (float) Math.sin(MathUtils.DTOR * tiePoint);
            cosTiePoints[i] = (float) Math.cos(MathUtils.DTOR * tiePoint);
        }
        _sinGrid = new TiePointGrid(base.getName(),
                                    base.getRasterWidth(),
                                    base.getRasterHeight(),
                                    base.getOffsetX(),
                                    base.getOffsetY(),
                                    base.getSubSamplingX(),
                                    base.getSubSamplingY(),
                                    sinTiePoints);
        _cosGrid = new TiePointGrid(base.getName(),
                                    base.getRasterWidth(),
                                    base.getRasterHeight(),
                                    base.getOffsetX(),
                                    base.getOffsetY(),
                                    base.getSubSamplingX(),
                                    base.getSubSamplingY(),
                                    cosTiePoints);
    }
}
