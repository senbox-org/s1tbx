package org.esa.beam.framework.ui.io;

import junit.framework.TestCase;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

public class BeamFileFilterTest extends TestCase {

    public void testDefaultConstructor() {
        final BeamFileFilter f = new BeamFileFilter();
        assertNull(f.getFormatName());
        assertNull(f.getDescription());
        assertNull(f.getDefaultExtension());
        assertNull(f.getExtensions());
        assertFalse(f.hasExtensions());
    }

    public void testSingleExtConstructor() {
        final BeamFileFilter f = new BeamFileFilter("RALLA", ".ral", "RALLA Files");
        assertEquals("RALLA", f.getFormatName());
        assertEquals("RALLA Files (*.ral)", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".ral", f.getDefaultExtension());
        assertEquals(1, f.getExtensions().length);
        assertEquals(".ral", f.getExtensions()[0]);
    }

    public void testMultipleExtConstructor() {
        final BeamFileFilter f = new BeamFileFilter("Holla", new String[]{".hol", ".ho", ".holla"}, "Holla Files");
        assertEquals("Holla", f.getFormatName());
        assertEquals("Holla Files (*.hol,*.ho,*.holla)", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".hol", f.getDefaultExtension());
        assertEquals(3, f.getExtensions().length);
        assertEquals(".hol", f.getExtensions()[0]);
        assertEquals(".ho", f.getExtensions()[1]);
        assertEquals(".holla", f.getExtensions()[2]);
    }

    public void testConstructorsBehaveEqualWithEmptyExtension() {
        BeamFileFilter fileFilter = new BeamFileFilter("All", "", "No Extension");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(null, fileFilter.getDefaultExtension());
        assertEquals("No Extension", fileFilter.getDescription());
        assertEquals(0, fileFilter.getExtensions().length);

        fileFilter = new BeamFileFilter("All", new String[]{""}, "No Extension");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(null, fileFilter.getDefaultExtension());
        assertEquals("No Extension", fileFilter.getDescription());
        assertEquals(0, fileFilter.getExtensions().length);

        fileFilter = new BeamFileFilter("All", ".42, ,uni", "One Empty");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(".42", fileFilter.getDefaultExtension());
        assertEquals("One Empty (*.42,*.uni)", fileFilter.getDescription());
        assertEquals(2, fileFilter.getExtensions().length);

        fileFilter = new BeamFileFilter("All", new String[]{".42", "", "uni"}, "One Empty");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(".42", fileFilter.getDefaultExtension());
        assertEquals("One Empty (*.42,*.uni)", fileFilter.getDescription());
        assertEquals(2, fileFilter.getExtensions().length);
    }

    public void testSetters() {
        final BeamFileFilter f = new BeamFileFilter();
        f.setFormatName("Zappo");
        f.setDescription("Zappo File Format");
        f.setExtensions(new String[]{".zap", ".ZAPPO"});

        assertEquals("Zappo", f.getFormatName());
        assertEquals("Zappo File Format", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".zap", f.getDefaultExtension());
        assertEquals(2, f.getExtensions().length);
        assertEquals(".zap", f.getExtensions()[0]);
        assertEquals(".ZAPPO", f.getExtensions()[1]);
    }

    public void testSingleExtIgnoreCase() {
        final BeamFileFilter f = new BeamFileFilter("RALLA", ".ral", "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
    }

    public void testMultipleExtIgnoreCase() {
        final BeamFileFilter f = new BeamFileFilter("RALLA", new String[]{".ral", ".lar"}, "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
        assertTrue(f.accept(new File("my_ralla.lar")));
        assertTrue(f.accept(new File("my_ralla.LAR")));
        assertTrue(f.accept(new File("my_ralla.Lar")));
    }

    public void testThatExtensionsIgnoreCase() {
        final BeamFileFilter f = new BeamFileFilter("RALLA", new String[]{".ral", ".ral.zip"}, "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
        assertTrue(f.accept(new File("my_ralla.ral.zip")));
        assertTrue(f.accept(new File("my_ralla.ral.ZIP")));
        assertTrue(f.accept(new File("my_ralla.RAL.Zip")));
    }

}
