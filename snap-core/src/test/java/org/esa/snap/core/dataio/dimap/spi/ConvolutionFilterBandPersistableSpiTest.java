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

package org.esa.snap.core.dataio.dimap.spi;

import junit.framework.TestCase;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.ProductData;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;

public class ConvolutionFilterBandPersistableSpiTest extends TestCase {

       private ConvolutionFilterBandPersistableSpi _persistableSpi;

    @Override
    public void setUp() {
        _persistableSpi = new ConvolutionFilterBandPersistableSpi();
    }

    @Override
    protected void tearDown() throws Exception {
        _persistableSpi = null;
    }

    public void testCanDecode_GoodElement() {

        final Element bandInfo = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        final Element filterInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        final Attribute bandType = new Attribute(DimapProductConstants.ATTRIB_BAND_TYPE, "ConvolutionFilterBand");
        filterInfo.setAttribute(bandType);
        bandInfo.addContent(filterInfo);

        assertTrue(_persistableSpi.canDecode(bandInfo));
    }

    public void testCanDecode_NotSpectralBandInfo() {

        final Element element = new Element("SomeWhat");

        assertFalse(_persistableSpi.canDecode(element));
    }

    public void testCanDecode_NoBandType() {

        final Element bandInfo = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        final Element filterInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        bandInfo.addContent(filterInfo);
        assertFalse(_persistableSpi.canDecode(bandInfo));
    }

    public void testCanDecode_NotCorrectBandType() {
        final Element bandInfo = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        final Element filterInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        final Attribute bandType = new Attribute(DimapProductConstants.ATTRIB_BAND_TYPE, "VirtualBand");
        filterInfo.setAttribute(bandType);
        bandInfo.addContent(filterInfo);

        assertFalse(_persistableSpi.canDecode(bandInfo));
    }


    public void testCanPersist() {
        final Band source = new Band("b", ProductData.TYPE_INT8, 2, 2);
        final ConvolutionFilterBand cfb = new ConvolutionFilterBand("test", source,
                                                                    new Kernel(2, 2, new double[]{0, 1, 2, 3}), 1);

        assertTrue(_persistableSpi.canPersist(cfb));

        assertFalse(_persistableSpi.canPersist(new ArrayList()));
        assertFalse(_persistableSpi.canPersist(new Object()));
        assertFalse(_persistableSpi.canPersist(new Band("b", ProductData.TYPE_INT8, 2, 2)));
    }

    public void testCreatePersistable() {
        assertNotNull(_persistableSpi.createPersistable());
    }
}
