/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.datamodel.quicklooks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DummyProductBuilder;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertEquals;

public class TestQuicklook {

    @Test
    public void testProduct() {
        DummyProductBuilder builder = new DummyProductBuilder();
        final Product product = builder.create();

        // initially has no quicklooks
        assertEquals(0, product.getQuicklookGroup().getNodeCount());

        final Quicklook defaultQL = product.getDefaultQuicklook();
        assertNotNull(defaultQL);
        assertEquals(1, product.getQuicklookGroup().getNodeCount());
    }

    @Test
    public void testDefaultQuicklook() {
        DummyProductBuilder builder = new DummyProductBuilder();
        final Product product = builder.create();

        final Quicklook defaultQL = product.getDefaultQuicklook();
        assertNotNull(defaultQL);

        assertEquals(Quicklook.DEFAULT_QUICKLOOK_NAME, defaultQL.getName());

        assertEquals(false, defaultQL.hasImage());
        assertEquals(false, defaultQL.hasCachedImage());

        final BufferedImage image = defaultQL.getImage(ProgressMonitor.NULL);
        assertNotNull(image);
    }
}
