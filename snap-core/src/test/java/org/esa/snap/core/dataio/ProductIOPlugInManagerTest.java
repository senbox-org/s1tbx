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

package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.IOException;
import java.util.Locale;

public class ProductIOPlugInManagerTest extends TestCase {

    private static final String _prodType = "TestProduct";
    private final ProductIOPlugInManager _m = ProductIOPlugInManager.getInstance();
    ProductReaderPlugIn _xr = new XProductReaderPi();
    ProductReaderPlugIn _yr = new YProductReaderPi();
    ProductWriterPlugIn _xw = new XProductWriterPi();
    ProductWriterPlugIn _yw = new YProductWriterPi();
    private static final int _sceneWidth = 400;
    private static final int _sceneHeight = 300;

    public ProductIOPlugInManagerTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductIOPlugInManagerTest.class);
    }

    @Override
    protected void setUp() {
        _m.addReaderPlugIn(_xr);
        _m.addReaderPlugIn(_yr);
        _m.addWriterPlugIn(_xw);
        _m.addWriterPlugIn(_yw);
    }

    @Override
    protected void tearDown() {
        _m.removeReaderPlugIn(_xr);
        _m.removeReaderPlugIn(_yr);
        _m.removeWriterPlugIn(_xw);
        _m.removeWriterPlugIn(_yw);
    }

    public void testThatGetInstanceAlwaysReturnsTheSameObject() {
        assertSame(_m, ProductIOPlugInManager.getInstance());
    }

    static class XProductReaderPi implements ProductReaderPlugIn {

        public Class[] getInputTypes() {
            return new Class[]{String.class};
        }

        public String[] getFormatNames() {
            return new String[]{"X"};
        }

        public String[] getDefaultFileExtensions() {
            return new String[]{".X"};
        }

        public String getDescription(Locale locale) {
            return "X  product reader";
        }

        public ProductReader createReaderInstance() {
            return new XProductReader(this);
        }

        public SnapFileFilter getProductFileFilter() {
            return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));

        }

        public DecodeQualification getDecodeQualification(Object input) {
            if(input instanceof String) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;
        }
    }

    static class YProductReaderPi implements ProductReaderPlugIn {

        public Class[] getInputTypes() {
            return new Class[]{String.class};
        }

        public String[] getFormatNames() {
            return new String[]{"Y"};
        }

        public String[] getDefaultFileExtensions() {
            return new String[]{".Y"};
        }

        public String getDescription(Locale locale) {
            return "Y product reader";
        }

        public ProductReader createReaderInstance() {
            return new YProductReader(this);
        }

        public SnapFileFilter getProductFileFilter() {
            return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
        }

        public DecodeQualification getDecodeQualification(Object input) {
             if(input instanceof String) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;
        }
    }

    static class XProductWriterPi implements ProductWriterPlugIn {

        @Override
        public EncodeQualification getEncodeQualification(Product product) {
            return EncodeQualification.FULL;
        }

        public Class[] getOutputTypes() {
            return new Class[]{String.class};
        }

        public String[] getFormatNames() {
            return new String[]{"X"};
        }

        public String[] getDefaultFileExtensions() {
            return new String[]{".X"};
        }

        public String getDescription(Locale locale) {
            return "X product writer";
        }

        public ProductWriter createWriterInstance() {
            return new XProductWriter(this);
        }

        public SnapFileFilter getProductFileFilter() {
            return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
        }

    }

    static class YProductWriterPi implements ProductWriterPlugIn {

        @Override
        public EncodeQualification getEncodeQualification(Product product) {
            return EncodeQualification.FULL;
        }

        public Class[] getOutputTypes() {
            return new Class[]{String.class};
        }

        public String[] getFormatNames() {
            return new String[]{"Y"};
        }

        public String[] getDefaultFileExtensions() {
            return new String[]{".Y"};
        }

        public String getDescription(Locale locale) {
            return "Y product writer";
        }

        public ProductWriter createWriterInstance() {
            return new YProductWriter(this);
        }

        public SnapFileFilter getProductFileFilter() {
            return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
        }

    }

    static class XProductReader extends AbstractProductReader {

        public XProductReader(XProductReaderPi pi) {
            super(pi);
        }

        public String[] getBandNames() {
            return new String[0];
        }

        @Override
        public Product readProductNodesImpl() throws IOException {
            return new Product("X product", _prodType, _sceneWidth, _sceneHeight);
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                              int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                              int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                              ProgressMonitor pm) throws IOException {
        }

        @Override
        public void close() {
        }
    }

    static class YProductReader extends AbstractProductReader {

        public YProductReader(YProductReaderPi pi) {
            super(pi);
        }

        public String[] getBandNames() {
            return new String[0];
        }

        @Override
        public Product readProductNodesImpl() throws IOException {
            return new Product("Y product", _prodType, _sceneWidth, _sceneHeight);
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                              int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                              int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                              ProgressMonitor pm) throws IOException {
        }

        @Override
        public void close() {
        }
    }

    static class XProductWriter extends AbstractProductWriter {

        public XProductWriter(XProductWriterPi pi) {
            super(pi);
        }

        @Override
        protected void writeProductNodesImpl() throws IOException {
        }

        public void writeBandRasterData(Band sourceBand,
                                        int sourceOffsetX, int sourceOffsetY,
                                        int sourceWidth, int sourceHeight,
                                        ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        }

        public void flush() {
        }

        public void close() {
        }

        public void deleteOutput() {
        }
    }

    static class YProductWriter extends AbstractProductWriter {

        public YProductWriter(YProductWriterPi pi) {
            super(pi);
        }

        @Override
        protected void writeProductNodesImpl() throws IOException {
        }

        public void writeBandRasterData(Band sourceBand,
                                        int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                        ProductData sourceBuffer,
                                        ProgressMonitor pm) throws IOException {
        }

        public void flush() {
        }

        public void close() {
        }

        public void deleteOutput() {
        }
    }
}
