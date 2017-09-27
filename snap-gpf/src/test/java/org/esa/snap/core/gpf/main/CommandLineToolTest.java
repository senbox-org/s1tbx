package org.esa.snap.core.gpf.main;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class CommandLineToolTest {
    @Test
    public void testFromBytes() throws Exception {

        Locale.setDefault(Locale.ENGLISH);
        
        assertEquals("43.0 GB", CommandLineTool.fromBytes(43L*(1024*1024*1024L)));
        assertEquals("3.2 GB", CommandLineTool.fromBytes(3421000000L));
        assertEquals("1.2 GB", CommandLineTool.fromBytes(1297000000L));
        assertEquals("7.5 MB", CommandLineTool.fromBytes(7845000));
        assertEquals("7.7 KB", CommandLineTool.fromBytes(7845));
        assertEquals("845 B", CommandLineTool.fromBytes(845));
    }

}