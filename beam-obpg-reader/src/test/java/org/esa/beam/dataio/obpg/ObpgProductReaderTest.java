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

import junit.framework.TestCase;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.HdfAttribute;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.dataio.obpg.hdf.SdsInfo;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.text.MessageFormat;

public class ObpgProductReaderTest extends TestCase {

    private File file;
    private ObpgUtils obpgUtilsMock;
    private ObpgProductReader productReader;

    @Override
    protected void setUp() throws Exception {
        if (!TestUtil.isHdfLibraryAvailable()) {
            return;
        }

        this.file = TestUtil.createFile();
        obpgUtilsMock = mock(ObpgUtils.class);

        final ProductReaderPlugIn plugIn = new ObpgProductReaderPlugIn();
        productReader = (ObpgProductReader) plugIn.createReaderInstance();
        productReader.obpgUtils = obpgUtilsMock;
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtil.deleteFileTree();
    }

    public void fixThisTestOK() throws IOException, HDFException {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        
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
        order.verify(obpgUtilsMock, times(1)).addBands(prodRet, sdsInfos, new HashMap<String, String>(),
                                                       new HashMap<String, String>());
        order.verify(obpgUtilsMock, times(1)).addGeocoding(prodRet, sdsInfos, false);
        order.verify(obpgUtilsMock, times(1)).addBitmaskDefinitions(prodRet, new BitmaskDef[0]);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(fileID);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void testFlipDataInt() {

        final int[] data = {
                11, 12, 13, 14, 15,
                16, 17, 18, 19, 20,
                21, 22, 23, 24, 25,
                26, 27, 28, 29, 30,
        };
        ObpgProductReader.reverse(ProductData.createInstance(data));

        final int[] expected = {
                30, 29, 28, 27, 26,
                25, 24, 23, 22, 21,
                20, 19, 18, 17, 16,
                15, 14, 13, 12, 11,
        };

        assertTrue(Arrays.equals(expected, data));
    }

    public void testFlipDataDouble() {

        final double[] data = {
                11.1, 12.1, 13.1, 14.1, 15.1,
                16.1, 17.1, 18.1, 19.1, 20.1,
                21.1, 22.1, 23.1, 24.1, 25.1,
                26.1, 27.1, 28.1, 29.1, 30.1,
        };
        ObpgProductReader.reverse(ProductData.createInstance(data));

        final double[] expected = {
                30.1, 29.1, 28.1, 27.1, 26.1,
                25.1, 24.1, 23.1, 22.1, 21.1,
                20.1, 19.1, 18.1, 17.1, 16.1,
                15.1, 14.1, 13.1, 12.1, 11.1,
        };

        assertTrue(Arrays.equals(expected, data));
    }
}
