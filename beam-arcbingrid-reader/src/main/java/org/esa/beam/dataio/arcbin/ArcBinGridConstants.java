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
package org.esa.beam.dataio.arcbin;


class ArcBinGridConstants {

    static final int CCITT = 0xff;
    static final int RLE_4BIT = 0xfc;
    static final int RLE_8BIT = 0xf8;
    static final int RLE_16BIT = 0xf0;
    static final int RLE_32BIT = 0xe0;
    static final int RUN_MIN = 0xdf;
    static final int RUN_8BIT = 0xd7;
    static final int RUN_16BIT = 0xcf;
    static final int RAW_32BIT = 0x20;
    static final int RAW_16BIT = 0x10;
    static final int RAW_8BIT = 0x08;
    static final int RAW_4BIT = 0x04;
    static final int RAW_1BIT = 0x01;
    static final int CONST_BLOCK = 0x00;

    static final int CELL_TYPE_INT = 1;
    static final int CELL_TYPE_FLOAT = 2;

    static final float NODATA_VALUE_FLOAT = -340282346638528859811704183484516925440.0f;

    private ArcBinGridConstants() {
    }
}
