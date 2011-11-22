package org.esa.beam.util;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        ArrayList<int[]> indexesList = new ArrayList<int[]>();

        @Override
        public void execute(int[] indexes, int[] sizes) {
            indexesList.add(indexes.clone());
        }
    }
}
