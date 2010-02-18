/*
 * $Id$
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
package org.esa.beam.dataio.obpg.hdf;

import junit.framework.TestCase;
import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.framework.datamodel.ProductData;

public class ObpgUtils_decodeHdfDataType_Test extends TestCase {

    public void test() {
        final ObpgUtils utils = new ObpgUtils();
        assertEquals(ProductData.TYPE_UINT8, utils.decodeHdfDataType(HDFConstants.DFNT_UCHAR8));
        assertEquals(ProductData.TYPE_UINT8, utils.decodeHdfDataType(HDFConstants.DFNT_UINT8));

        assertEquals(ProductData.TYPE_INT8, utils.decodeHdfDataType(HDFConstants.DFNT_CHAR8));
        assertEquals(ProductData.TYPE_INT8, utils.decodeHdfDataType(HDFConstants.DFNT_INT8));

        assertEquals(ProductData.TYPE_INT16, utils.decodeHdfDataType(HDFConstants.DFNT_INT16));

        assertEquals(ProductData.TYPE_UINT16, utils.decodeHdfDataType(HDFConstants.DFNT_UINT16));

        assertEquals(ProductData.TYPE_INT32, utils.decodeHdfDataType(HDFConstants.DFNT_INT32));

        assertEquals(ProductData.TYPE_UINT32, utils.decodeHdfDataType(HDFConstants.DFNT_UINT32));

        assertEquals(ProductData.TYPE_FLOAT32, utils.decodeHdfDataType(HDFConstants.DFNT_FLOAT32));

        assertEquals(ProductData.TYPE_FLOAT64, utils.decodeHdfDataType(HDFConstants.DFNT_FLOAT64));

        assertEquals(ProductData.TYPE_UNDEFINED, utils.decodeHdfDataType(HDFConstants.DFNT_FLOAT128));
    }

}