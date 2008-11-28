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
package com.bc.ceres.binio.binx;

import junit.framework.TestCase;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.io.IOException;
import java.util.Map;

import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class DBL_SM_XXXX_MIR_SCSD1C_0200_Test extends TestCase {

    public void testBinXIO() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("DBL_SM_XXXX_MIR_SCSD1C_0200.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();
        DataFormat dataFormat = binx.readDataFormat(uri);
        assertNotNull(dataFormat);
        assertNotNull(dataFormat.getType());
        assertEquals("Data_Block", dataFormat.getType().getName());
    }
}
