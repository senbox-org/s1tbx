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

package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DummyImageInputStream;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.TreeNode;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class AbstractProductReaderTest extends TestCase {

    private AbstractProductReader reader;

    public void testGetProductComponents_inputFile() throws IOException {
        URL location = AbstractProductReaderTest.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(location.getFile());

        reader.readProductNodes(file, null);

        TreeNode<File> productComponents = reader.getProductComponents();
        assertNotNull(productComponents);

        File parent = file.getParentFile();
        assertEquals(parent.getName(), productComponents.getId());
        assertEquals(parent, productComponents.getContent());

        TreeNode<File>[] treeNodes = productComponents.getChildren();
        assertEquals(1, treeNodes.length);
        assertEquals(file.getName(), treeNodes[0].getId());
        assertEquals(file, treeNodes[0].getContent());
    }

    public void testGetProductComponents_inputString() throws IOException {
        URL location = AbstractProductReaderTest.class.getProtectionDomain().getCodeSource().getLocation();

        reader.readProductNodes(location.getFile(), null);

        File file = new File(location.getFile());
        TreeNode<File> productComponents = reader.getProductComponents();
        assertNotNull(productComponents);

        File parent = file.getParentFile();
        assertEquals(parent.getName(), productComponents.getId());
        assertEquals(parent, productComponents.getContent());

        TreeNode<File>[] treeNodes = productComponents.getChildren();
        assertEquals(1, treeNodes.length);
        assertEquals(file.getName(), treeNodes[0].getId());
        assertEquals(file, treeNodes[0].getContent());
    }

    public void testGetProductComponents_unsupportedInputObject() throws IOException {
        reader.readProductNodes(new DummyImageInputStream(), null);

        TreeNode<File> productComponents = reader.getProductComponents();
        assertNull(productComponents);
    }

    @Override
    protected void setUp() throws Exception {
        reader = new TestProductReader();
    }

    private class TestProductReader extends AbstractProductReader {

        private TestProductReader() {
            super(null);
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            return new Product("test", "what", 3, 5);
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {            
        }
    }
}
