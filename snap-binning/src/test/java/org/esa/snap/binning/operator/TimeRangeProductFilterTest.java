/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TimeRangeProductFilterTest {

    private Product product;
    private BinningProductFilter timeRangeProductFilter;
    private BinningProductFilter parent;

    @Before
    public void setUp() throws Exception {
        parent = mock(BinningProductFilter.class);
        product = mock(Product.class);
        when(parent.accept(product)).thenReturn(true);
        timeRangeProductFilter = new TimeRangeProductFilter(parent,
                                                            ProductData.UTC.parse("02-MAY-2013 00:00:00"),
                                                            ProductData.UTC.parse("02-MAY-2013 23:59:59"));
    }

    @Test
    public void testAcceptProduct_ProductsStartAndEndTimesAreBothWithinRange() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 15:10:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 15:40:00"));

        assertThat(timeRangeProductFilter.accept(product), is(true));
        assertThat(timeRangeProductFilter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_ParentProductFilterReject() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 15:10:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 15:40:00"));
        when(parent.accept(product)).thenReturn(false);
        when(parent.getReason()).thenReturn("parent reason");

        assertThat(timeRangeProductFilter.accept(product), is(false));
        assertThat(timeRangeProductFilter.getReason(), is("parent reason"));
    }

    @Test
    public void testRejectProduct_ProductsStartAndEndTimesAreBothBeforeRange() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("01-MAY-2013 15:10:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("01-MAY-2013 15:40:00"));

        assertThat(timeRangeProductFilter.accept(product), is(false));
        assertThat(timeRangeProductFilter.getReason(), is("Does not match the time range."));
    }

    @Test
    public void testRejectProduct_ProductsStartAndEndTimesAreBothAfterRange() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("03-MAY-2013 15:10:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("03-MAY-2013 15:40:00"));

        assertThat(timeRangeProductFilter.accept(product), is(false));
        assertThat(timeRangeProductFilter.getReason(), is("Does not match the time range."));
    }

    @Test
    public void testRejectProduct_ProductsStartTimeIsInsideRange() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 23:30:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("03-MAY-2013 00:30:00"));

        assertThat(timeRangeProductFilter.accept(product), is(true));
        assertThat(timeRangeProductFilter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_ProductsEndTimeIsInsideRange() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("01-MAY-2013 23:30:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 00:30:00"));

        assertThat(timeRangeProductFilter.accept(product), is(true));
        assertThat(timeRangeProductFilter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_ProductsStartTimeEqualsRangeStartTime() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 00:00:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 01:00:00"));

        assertThat(timeRangeProductFilter.accept(product), is(true));
        assertThat(timeRangeProductFilter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_ProductsEndTimeEqualsRangeEndTime() throws Exception {
        when(product.getStartTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 23:00:00"));
        when(product.getEndTime()).thenReturn(ProductData.UTC.parse("02-MAY-2013 23:59:59"));

        assertThat(timeRangeProductFilter.accept(product), is(true));
        assertThat(timeRangeProductFilter.getReason(), is(nullValue()));
    }
}
