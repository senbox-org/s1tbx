/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.DataFormat;
import junit.framework.TestCase;

import java.net.URL;
import java.nio.ByteOrder;

/**
 * Tests for class {@link SmosFormats}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class SmosFormatsTest extends TestCase {

    public void testGetFormat() {
        final SmosFormats formats = SmosFormats.getInstance();

        final DataFormat format = formats.getFormat("DBL_SM_XXXX_MIR_BWLD1C_0200.binXschema.xml");
        assertNotNull(format);
        assertEquals(ByteOrder.LITTLE_ENDIAN, format.getByteOrder());
    }

    public void testGetFormatForSchemaWhichDoesNotExist() {
        assertNull(SmosFormats.getInstance().getFormat("SCHEMA_DOES_NOT_EXIST.binXschema.xml"));
    }

    public void testGetSchemaResource() {
        final URL url = SmosFormats.getSchemaResource("DBL_SM_XXXX_MIR_BWLD1C_0200.binXschema.xml");

        assertNotNull(url);
        assertTrue(url.getPath().endsWith("DBL_SM_XXXX_MIR_BWLD1C_0200.binXschema.xml"));
        assertEquals(url, SmosFormats.getSchemaResource("DBL_SM_XXXX_MIR_BWLD1C_0200"));
    }
}
