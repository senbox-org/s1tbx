package org.esa.beam.jai;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LevelImageSupportTest {

    private LevelImageSupport levelImageSupport;

    @Test
    public void testGetSourceX() {
        final int w = levelImageSupport.getSourceWidth();

        assertEquals(760, w);

        assertEquals(0, levelImageSupport.getSourceX(0));
        assertEquals(1, levelImageSupport.getSourceX(1));

        assertEquals(w - 1, levelImageSupport.getSourceX(w - 1));
        assertEquals(w - 1, levelImageSupport.getSourceX(w));
        assertEquals(w - 1, levelImageSupport.getSourceX(w + 1));
    }

    @Test
    public void testGetSourceY() {
        final int h = levelImageSupport.getSourceHeight();

        assertEquals(1120, h);

        assertEquals(0, levelImageSupport.getSourceY(0));
        assertEquals(1, levelImageSupport.getSourceY(1));

        assertEquals(h - 1, levelImageSupport.getSourceY(h - 1));
        assertEquals(h - 1, levelImageSupport.getSourceY(h));
        assertEquals(h - 1, levelImageSupport.getSourceY(h + 1));
    }

    @Test
    public void testGetSourceWidth() {
        final int w = levelImageSupport.getSourceWidth();

        assertEquals(760, w);

        assertEquals(1, levelImageSupport.getSourceWidth(0));
        assertEquals(1, levelImageSupport.getSourceWidth(1));

        assertEquals(w - 1, levelImageSupport.getSourceWidth(w - 1));
        assertEquals(w, levelImageSupport.getSourceWidth(w));
        assertEquals(w, levelImageSupport.getSourceWidth(w + 1));
    }

    @Test
    public void testGetSourceHeight() {
        final int h = levelImageSupport.getSourceHeight();

        assertEquals(1120, h);

        assertEquals(1, levelImageSupport.getSourceHeight(0));
        assertEquals(1, levelImageSupport.getSourceHeight(1));

        assertEquals(h - 1, levelImageSupport.getSourceHeight(h - 1));
        assertEquals(h, levelImageSupport.getSourceHeight(h));
        assertEquals(h, levelImageSupport.getSourceHeight(h + 1));
    }

    @Before
    public void init() {
        levelImageSupport = new LevelImageSupport(760, 1120, ResolutionLevel.MAXRES);
    }

}
