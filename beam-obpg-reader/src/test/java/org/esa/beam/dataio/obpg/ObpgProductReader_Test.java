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
package org.esa.beam.dataio.obpg;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.HdfAttribute;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.dataio.obpg.hdf.SdsInfo;
import org.esa.beam.framework.datamodel.Product;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

public class ObpgProductReader_Test extends TestCase {

    private ObpgProductReaderPlugIn plugIn;
    private File file;
    private static final String OBPG_TESTS = "ObpgTests";
    private ObpgUtils obpgUtilsMock;
    private ObpgProductReader productReader;

    protected void setUp() throws Exception {
        file = new File(OBPG_TESTS + "/File.hdf");
        file.getParentFile().mkdirs();
        file.createNewFile();

        obpgUtilsMock = mock(ObpgUtils.class);

        plugIn = new ObpgProductReaderPlugIn();
        productReader = (ObpgProductReader) plugIn.createReaderInstance();
        productReader.obpgUtils = obpgUtilsMock;
    }

    protected void tearDown() throws Exception {
        TestUtil.deleteFileTree(OBPG_TESTS);
    }

    public void testOK() throws IOException, HDFException {
        final ArrayList<HdfAttribute> globalAttributes = new ArrayList<HdfAttribute>();
        final Product prodRet = new Product("name", "type", 22, 33);
        final int fileID = 222;
        final int sdStart = 333;
        final SdsInfo[] sdsInfos = new SdsInfo[3];

        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(fileID);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(sdStart);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(globalAttributes);
        stub(obpgUtilsMock.createProductBody(globalAttributes)).toReturn(prodRet);
        stub(obpgUtilsMock.extractSdsData(sdStart)).toReturn(sdsInfos);

        final Product product = productReader.readProductNodes(file, null);

        assertNotNull(product);
        assertSame(prodRet, product);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(sdStart);
        order.verify(obpgUtilsMock, times(1)).createProductBody(new ArrayList<HdfAttribute>());
        order.verify(obpgUtilsMock, times(1)).addGlobalMetadata(prodRet, new ArrayList<HdfAttribute>());
        order.verify(obpgUtilsMock, times(1)).extractSdsData(sdStart);
        order.verify(obpgUtilsMock, times(1)).addScientificMetadata(prodRet, sdsInfos);
        order.verify(obpgUtilsMock, times(1)).addBands(prodRet, sdsInfos);
        order.verify(obpgUtilsMock, times(1)).addGeocoding(prodRet, sdsInfos);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(fileID);
        verifyNoMoreInteractions(obpgUtilsMock);
    }
}
