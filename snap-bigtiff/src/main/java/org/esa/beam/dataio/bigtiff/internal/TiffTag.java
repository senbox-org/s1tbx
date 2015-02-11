/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.bigtiff.internal;

import org.esa.beam.dataio.bigtiff.Constants;

/**
 * TIFF tags for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class TiffTag {

    public static final short NewSubfileType = 254;
    public static final short SubfileType = 255;
    public static final TiffShort IMAGE_WIDTH = new TiffShort(256);
    public static final TiffShort IMAGE_LENGTH = new TiffShort(257);
    public static final TiffShort BITS_PER_SAMPLE = new TiffShort(258);
    public static final TiffShort COMPRESSION = new TiffShort(259);
    public static final TiffShort PHOTOMETRIC_INTERPRETATION = new TiffShort(262);
    public static final short Threshholding = 263;
    public static final short CellWidth = 264;
    public static final short CellLength = 265;
    public static final short FillOrder = 266;
    public static final short DocumentName = 269;
    public static final TiffShort IMAGE_DESCRIPTION = new TiffShort(270);
    public static final short Make = 271;
    public static final short Model = 272;
    public static final TiffShort STRIP_OFFSETS = new TiffShort(273);
    public static final short Orientation = 274;
    public static final TiffShort SAMPLES_PER_PIXEL = new TiffShort(277);
    public static final TiffShort ROWS_PER_STRIP = new TiffShort(278);
    public static final TiffShort STRIP_BYTE_COUNTS = new TiffShort(279);
    public static final short MinSampleValue = 280;
    public static final short MaxSampleValue = 281;
    public static final TiffShort X_RESOLUTION = new TiffShort(282);
    public static final TiffShort Y_RESOLUTION = new TiffShort(283);
    public static final TiffShort PLANAR_CONFIGURATION = new TiffShort(284);
    public static final short PageName = 285;
    public static final short XPosition = 286;
    public static final short YPosition = 287;
    public static final short FreeOffsets = 288;
    public static final short FreeByteCounts = 289;
    public static final short GrayResponseUnit = 290;
    public static final short GrayResponseCurve = 291;
    public static final short T4Options = 292;
    public static final short T6Options = 293;
    public static final TiffShort RESOLUTION_UNIT = new TiffShort(296);
    public static final short PageNumber = 297;
    public static final short TransferFunction = 301;
    public static final short Software = 305;
    public static final short DateTime = 306;
    public static final short Artist = 315;
    public static final short HostComputer = 316;
    public static final short Predictor = 317;
    public static final short WhitePoint = 318;
    public static final short PrimaryChromaticities = 319;
    public static final TiffShort COLOR_MAP = new TiffShort(320);
    public static final short HalftoneHints = 321;
    public static final TiffShort TILE_WIDTH = new TiffShort(322);
    public static final TiffShort TILE_LENGTH = new TiffShort(323);
    public static final short TileOffsets = 324;
    public static final short TileByteCounts = 325;
    public static final short InkSet = 332;
    public static final short InkNames = 333;
    public static final short NumberOfInks = 334;
    public static final short DotRange = 336;
    public static final short TargetPrinter = 337;
    public static final TiffShort EXTRA_SAMPLES = new TiffShort(338);
    public static final TiffShort SAMPLE_FORMAT = new TiffShort(339);
    public static final short SMinSampleValue = 340;
    public static final short SMaxSampleValue = 341;
    public static final short TransferRange = 342;
    public static final short JPEGProc = 512;
    public static final short JPEGInterchangeFormat = 513;
    public static final short JPEGInterchangeFormatLngth = 514;
    public static final short JPEGRestartInterval = 515;
    public static final short JPEGLosslessPredictors = 517;
    public static final short JPEGPointTransforms = 518;
    public static final short JPEGQTables = 519;
    public static final short JPEGDCTables = 520;
    public static final short JPEGACTables = 521;
    public static final short YCbCrCoefficients = 529;
    public static final short YCbCrSubSampling = 530;
    public static final short YCbCrPositioning = 531;
    public static final short ReferenceBlackWhite = 532;
    public static final int Copyright = 33432;
    public static final TiffShort ModelTiepointTag = new TiffShort(33922);
    public static final TiffShort ModelPixelScaleTag = new TiffShort(33550);
    public static final TiffShort ModelTransformationTag = new TiffShort(34264);
    public static final TiffShort GeoKeyDirectoryTag = new TiffShort(34735);
    public static final TiffShort GeoDoubleParamsTag = new TiffShort(34736);
    public static final TiffShort GeoAsciiParamsTag = new TiffShort(34737);
    public static final TiffShort BEAM_METADATA = new TiffShort(Constants.PRIVATE_BEAM_TIFF_TAG_NUMBER);
}