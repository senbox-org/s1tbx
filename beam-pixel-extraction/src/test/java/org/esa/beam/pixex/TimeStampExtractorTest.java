package org.esa.beam.pixex;

import org.junit.*;

import static org.junit.Assert.*;

public class TimeStampExtractorTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSomething() {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "*_MER_${date}*");
        assertNotNull(extractor);
    }
}
