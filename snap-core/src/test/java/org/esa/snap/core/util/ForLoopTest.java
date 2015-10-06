/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ForLoopTest {

    @Test
    public void test0D() throws Exception {
        final TestBody body = new TestBody();
        ForLoop.execute(new int[]{}, body);
        assertEquals(0, body.indexesList.size());
    }

    @Test
    public void test1D() throws Exception {
        final TestBody body = new TestBody();
        ForLoop.execute(new int[]{3}, body);
        assertEquals(3, body.indexesList.size());
        assertArrayEquals(new int[]{0}, body.indexesList.get(0));
        assertArrayEquals(new int[]{1}, body.indexesList.get(1));
        assertArrayEquals(new int[]{2}, body.indexesList.get(2));
    }

    @Test
    public void test2D() throws Exception {
        final TestBody body = new TestBody();
        ForLoop.execute(new int[]{2, 3}, body);
        assertEquals(2 * 3, body.indexesList.size());
        assertArrayEquals(new int[]{0, 0}, body.indexesList.get(0));
        assertArrayEquals(new int[]{0, 1}, body.indexesList.get(1));
        assertArrayEquals(new int[]{0, 2}, body.indexesList.get(2));
        assertArrayEquals(new int[]{1, 0}, body.indexesList.get(3));
        assertArrayEquals(new int[]{1, 1}, body.indexesList.get(4));
        assertArrayEquals(new int[]{1, 2}, body.indexesList.get(5));
    }

    @Test
    public void test3D() throws Exception {
        final TestBody body = new TestBody();
        ForLoop.execute(new int[]{2, 2, 3}, body);
        assertEquals(2 * 2 * 3, body.indexesList.size());
        assertArrayEquals(new int[]{0, 0, 0}, body.indexesList.get(0));
        assertArrayEquals(new int[]{0, 0, 1}, body.indexesList.get(1));
        assertArrayEquals(new int[]{0, 0, 2}, body.indexesList.get(2));
        assertArrayEquals(new int[]{0, 1, 0}, body.indexesList.get(3));
        assertArrayEquals(new int[]{0, 1, 1}, body.indexesList.get(4));
        assertArrayEquals(new int[]{0, 1, 2}, body.indexesList.get(5));
        assertArrayEquals(new int[]{1, 0, 0}, body.indexesList.get(6));
        assertArrayEquals(new int[]{1, 0, 1}, body.indexesList.get(7));
        assertArrayEquals(new int[]{1, 0, 2}, body.indexesList.get(8));
        assertArrayEquals(new int[]{1, 1, 0}, body.indexesList.get(9));
        assertArrayEquals(new int[]{1, 1, 1}, body.indexesList.get(10));
        assertArrayEquals(new int[]{1, 1, 2}, body.indexesList.get(11));
    }

    @Test
    public void test6D() throws Exception {
        final TestBody body = new TestBody();
        ForLoop.execute(new int[]{3, 2, 4, 2, 2, 3}, body);
        assertEquals(3 * 2 * 4 * 2 * 2 * 3, body.indexesList.size());
    }

    private static class TestBody implements ForLoop.Body {
        private ArrayList<int[]> indexesList = new ArrayList<int[]>();

        @Override
        public void execute(int[] indexes, int[] sizes) {
            indexesList.add(indexes.clone());
        }
    }
}
