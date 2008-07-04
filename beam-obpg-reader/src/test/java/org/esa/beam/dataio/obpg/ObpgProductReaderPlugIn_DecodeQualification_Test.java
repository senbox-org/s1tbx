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

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.HdfAttribute;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.dataio.obpg.hdf.lib.HDFTestCase;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;

public class ObpgProductReaderPlugIn_DecodeQualification_Test extends HDFTestCase {

    private ObpgProductReaderPlugIn plugIn;
    private File file;
    private static final String OBPG_TESTS = "ObpgTests";
    private ObpgUtils obpgUtilsMock;

    protected void setUp() throws Exception {
        file = new File(OBPG_TESTS + "/File.hdf");
        file.getParentFile().mkdirs();
        file.createNewFile();

        plugIn = new ObpgProductReaderPlugIn();
        obpgUtilsMock = mock(ObpgUtils.class);
        plugIn.utils = obpgUtilsMock;
    }

    protected void tearDown() throws Exception {
        TestUtil.deleteFileTree(OBPG_TESTS);
    }

    public void test_Unable_BecauseHDFLibrarySaysItIsNotHDF() throws HDFException {
        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(false);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.UNABLE, qualification);

        verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_Unable_emptyAttributeList() throws HDFException {
        final ArrayList<HdfAttribute> emptyAttributeList = new ArrayList<HdfAttribute>();

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(emptyAttributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.UNABLE, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_Unable_noTitleAttribute() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("anyName", 5, "gsch", 4));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.UNABLE, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_Unable_WithTitleAttributeButUnexpectedValue() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("Title", HDFConstants.DFNT_CHAR8, "gsch", 4));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.UNABLE, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_INTENDED___MODIS_A_Level_2_Data() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("Title", HDFConstants.DFNT_CHAR8, "MODISA Level-2 Data", 19));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.INTENDED, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_INTENDED___MODIS_T_Level_2_Data() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("Title", HDFConstants.DFNT_CHAR8, "MODIST Level-2 Data", 19));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.INTENDED, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_INTENDED___CZCS_Level_2_Data() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("Title", HDFConstants.DFNT_CHAR8, "CZCS Level-2 Data", 17));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.INTENDED, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }

    public void test_INTENDED___OCTS_Level_2_Data() throws HDFException {
        final ArrayList<HdfAttribute> attributeList = new ArrayList<HdfAttribute>();
        attributeList.add(new HdfAttribute("Title", HDFConstants.DFNT_CHAR8, "OCTS Level-2 Data", 17));
        attributeList.add(new HdfAttribute("anyName2", 6, "gsche", 5));

        stub(obpgUtilsMock.isHdfFile(anyString())).toReturn(true);
        stub(obpgUtilsMock.openHdfFileReadOnly(anyString())).toReturn(222);
        stub(obpgUtilsMock.openSdInterfaceReadOnly(anyString())).toReturn(333);
        stub(obpgUtilsMock.readGlobalAttributes(anyInt())).toReturn(attributeList);
        stub(obpgUtilsMock.closeHdfFile(anyInt())).toReturn(true);

        final DecodeQualification qualification = plugIn.getDecodeQualification(file);

        assertEquals(DecodeQualification.INTENDED, qualification);

        final InOrder order = inOrder(obpgUtilsMock);
        order.verify(obpgUtilsMock, times(1)).isHdfFile(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openHdfFileReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).openSdInterfaceReadOnly(file.getPath());
        order.verify(obpgUtilsMock, times(1)).readGlobalAttributes(333);
        order.verify(obpgUtilsMock, times(1)).closeHdfFile(222);
        verifyNoMoreInteractions(obpgUtilsMock);
    }
}
