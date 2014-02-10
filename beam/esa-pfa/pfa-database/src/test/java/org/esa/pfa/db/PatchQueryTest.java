package org.esa.pfa.db;

import org.junit.Test;

import java.io.File;

/**
 *
 */
public class PatchQueryTest {

    @Test
    public void testQueryAll() throws Exception {
        PatchQuery db = new PatchQuery(new File("c:\\temp"));

        db.query("product: ENVI*", 30);
    }


}
