/*
 * Copyright (C) 2020 Sensar B.V. http://www.sensar.nl
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.sentinel1.gpf.util;

import org.esa.s1tbx.commons.Sentinel1Utils;

import java.awt.*;


/**
 * Utility class for managing burst overlaps.
 *
 * @author David A. Monge
 */
public class OverlapUtils {


    public static void getOverlappedRectangles(final int overlapIndex,
                                               final Rectangle overlapInBurstOneRectangle,
                                               final Rectangle overlapInBurstTwoRectangle,
                                               Sentinel1Utils.SubSwathInfo subSwath) {

        final int firstValidPixelOfBurstOne = getBurstFirstValidPixel(overlapIndex, subSwath);
        final int lastValidPixelOfBurstOne = getBurstLastValidPixel(overlapIndex, subSwath);
        final int firstValidPixelOfBurstTwo = getBurstFirstValidPixel(overlapIndex + 1, subSwath);
        final int lastValidPixelOfBurstTwo = getBurstLastValidPixel(overlapIndex + 1, subSwath);
        final int firstValidPixel = Math.max(firstValidPixelOfBurstOne, firstValidPixelOfBurstTwo);
        final int lastValidPixel = Math.min(lastValidPixelOfBurstOne, lastValidPixelOfBurstTwo);
        final int x0 = firstValidPixel;
        final int w = lastValidPixel - firstValidPixel + 1;

        final int numOfInvalidLinesInBurstOne = subSwath.linesPerBurst -
                subSwath.lastValidLine[overlapIndex] - 1;

        final int numOfInvalidLinesInBurstTwo = subSwath.firstValidLine[overlapIndex + 1];

        final int numOverlappedLines = computeBurstOverlapSize(overlapIndex, subSwath);

        final int h = numOverlappedLines - numOfInvalidLinesInBurstOne - numOfInvalidLinesInBurstTwo;

        final int y0BurstOne =
                subSwath.linesPerBurst * (overlapIndex + 1) - numOfInvalidLinesInBurstOne - h;

        final int y0BurstTwo =
                subSwath.linesPerBurst * (overlapIndex + 1) + numOfInvalidLinesInBurstTwo;

        overlapInBurstOneRectangle.setBounds(x0, y0BurstOne, w, h);
        overlapInBurstTwoRectangle.setBounds(x0, y0BurstTwo, w, h);
    }

    public static int getBurstFirstValidPixel(final int burstIndex, Sentinel1Utils.SubSwathInfo subSwath) {
        for (int lineIdx = 0; lineIdx < subSwath.firstValidSample[burstIndex].length; lineIdx++) {
            if (subSwath.firstValidSample[burstIndex][lineIdx] != -1) {
                return subSwath.firstValidSample[burstIndex][lineIdx];
            }
        }

        return -1;
    }

    public static int getBurstLastValidPixel(final int burstIndex, Sentinel1Utils.SubSwathInfo subSwath) {
        for (int lineIdx = 0; lineIdx < subSwath.lastValidSample[burstIndex].length; lineIdx++) {
            if (subSwath.lastValidSample[burstIndex][lineIdx] != -1) {
                return subSwath.lastValidSample[burstIndex][lineIdx];
            }
        }

        return -1;
    }

    /**
     * Compute the number of lines in the overlapped area of given adjacent bursts.
     * @return The number of lines in the overlapped area.
     */
    private static int computeBurstOverlapSize(final int overlapIndex, Sentinel1Utils.SubSwathInfo subSwath) {
        final double endTime = subSwath.burstLastLineTime[overlapIndex];
        final double startTime = subSwath.burstFirstLineTime[overlapIndex + 1];

        return (int)((endTime - startTime) / subSwath.azimuthTimeInterval);
    }
}
