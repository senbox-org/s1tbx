/*
 * $Id: ProjectionUtils.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataio;

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.ProductUtils;


/**
 * A utility class for map-projections.
 * @deprecated most methods moved to {@link org.esa.beam.util.ProductUtils}
 */
public class ProjectionUtils {

    /**
     * @deprecated moved to {@link org.esa.beam.util.ProductUtils#computeSourcePixelCoordinates}
     */
    public static PixelPos[] computeSourcePixelCoordinates(final GeoCoding sourceGeoCoding,
                                                           final int sourceWidth,
                                                           final int sourceHeight,
                                                           final GeoCoding destGeoCoding,
                                                           final Rectangle destArea) {
        return ProductUtils.computeSourcePixelCoordinates(sourceGeoCoding,
                                                          sourceWidth,
                                                          sourceHeight,
                                                          destGeoCoding,
                                                          destArea);
    }

    /**
     * @deprecated moved to [@link ProductUtils#computeSourcePixelCoordinates}
     */
    public static float[] computeMinMaxY(PixelPos[] pixelPositions) {
        return ProductUtils.computeMinMaxY(pixelPositions);
    }

    /**
     * @deprecated no direct replacement, in BEAM {@link com.bc.util.CachingObjectArray} is used instead
     */
    public static class LineCache {

        private final Object[] _lines;
        private final int _x0;
        private final int _y0;
        private final int _lineSize;
        private final int _numLines;

        public LineCache(int x0, int y0, int lineSize, int numLines) {
            if (lineSize <= 0) {
                throw new IllegalArgumentException("invalid line size");
            }
            if (numLines <= 0) {
                throw new IllegalArgumentException("invalid line number");
            }
            _lines = new Object[numLines];
            _x0 = x0;
            _y0 = y0;
            _lineSize = lineSize;
            _numLines = numLines;
        }

        public void setLines(LineCache cache) {

            final int thisY0 = getY0();
            final int thisY1 = getY1();
            final int otherY0 = cache.getY0();
            final int otherY1 = cache.getY1();
            for (int y = otherY0; y <= otherY1; y++) {
                Object line = cache.getLine(y);
                if (line != null) {
                    if (y >= thisY0 && y <= thisY1) {
                        setLine(y, line);
                    }
                }
            }
        }

        public Object getLine(int y) {
            return _lines[y - _y0];
        }

        public void setLine(int y, Object line) {
            _lines[y - _y0] = line;
        }

        public int getX0() {
            return _x0;
        }

        public int getY0() {
            return _y0;
        }

        public int getY1() {
            return _y0 + _numLines - 1;
        }

        public int getLineSize() {
            return _lineSize;
        }

        public int getNumLines() {
            return _numLines;
        }

        public void dispose() {
            for (int i = 0; i < _lines.length; i++) {
                _lines[i] = null;
            }
        }
    }
}
