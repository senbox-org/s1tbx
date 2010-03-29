package org.esa.beam.processor.binning.database;

import junit.framework.TestCase;

public class FloatArrayBinTest  extends TestCase {
     private FloatArrayBin bin12;

    @Override
    public void setUp() {
        bin12 = new FloatArrayBin(new int[]{1, 2});
    }

    public void testCreate(){
        assertFalse("contains no data after creation", bin12.containsData());
        assertEquals("contains a zero", 0, bin12.read(0), 0.00001);
    }

    public void testReadTooManyDataItems() {
        Bin bin1 = new FloatArrayBin(new int[]{1});
        try{
            bin1.setBandIndex(0);
            bin1.read(1);
            fail("Should raise an IndexOutOfBoundsException. Contains only one dataItem.");
        }catch(IndexOutOfBoundsException ignored) {
        }

        try{
            bin12.setBandIndex(1);
            bin12.read(3);
            fail("Should raise an IndexOutOfBoundsException. Contains only two dataItem.");
        }catch(IndexOutOfBoundsException ignored) {
        }
    }

    public void testReadTooManyBands() {
        Bin bin1 = new FloatArrayBin(new int[]{1});
        try{
            bin1.setBandIndex(1);
            fail("Should raise anIndexOutOfBoundsException. Contains only one band.");
        }catch(IndexOutOfBoundsException ignored) {
        }

        Bin bin11 = new FloatArrayBin(new int[]{1, 1});
        try{
            bin11.setBandIndex(2);
            fail("Should raise an IndexOutOfBoundsException. Contains only two bands.");
        }catch(IndexOutOfBoundsException ignored) {
        }
    }

    public void testReadWrtite() {
        assertFalse("contains no data after creation", bin12.containsData());
        bin12.setBandIndex(0);
        bin12.write(0, 42.42f);
        assertTrue("contains data", bin12.containsData());
        assertEquals("read 42.42f", 42.42f, bin12.read(0), 0.000001f);

        bin12.setBandIndex(1);
        bin12.write(0, 1f);
        bin12.write(1, 2f);
        assertEquals("read 1f", 1f, bin12.read(0), 0.000001f);
        assertEquals("read 2f", 2f, bin12.read(1), 0.000001f);
        bin12.setBandIndex(0);
        assertEquals("read 42.42f", 42.42f, bin12.read(0), 0.000001f);
    }

    public void testSave() {
        bin12.setBandIndex(0);
        bin12.write(0, 42f);
        bin12.setBandIndex(1);
        bin12.write(0, 1f);
        bin12.write(1, 2f);

        final float[] savedBin = bin12.save(null);
        assertEquals("contains 3 members", 3, savedBin.length);
        assertEquals("member value", 42f, savedBin[0], 0.000001f);
        assertEquals("member value", 1f, savedBin[1], 0.000001f);
        assertEquals("member value", 2f, savedBin[2], 0.000001f);

        float[] recycledData = new float[3];
        bin12.save(recycledData);
        assertEquals("contains 3 members", 3, recycledData.length);
        assertEquals("member value", 42f, recycledData[0], 0.000001f);
        assertEquals("member value", 1f, recycledData[1], 0.000001f);
        assertEquals("member value", 2f, recycledData[2], 0.000001f);
    }

    public void testLoad() {
        final float[] loadData = new float[]{42f, 1f, 2f};
        assertFalse("contains no data after creation", bin12.containsData());
        bin12.load(loadData);
        assertTrue("contains data", bin12.containsData());
        bin12.setBandIndex(0);
        assertEquals("member value", 42f, bin12.read(0), 0.000001f);
        bin12.setBandIndex(1);
        assertEquals("member value", 1f, bin12.read(0), 0.000001f);
        assertEquals("member value", 2f, bin12.read(1), 0.000001f);
    }

    public void testClear() {
        assertFalse("contains no data after creation", bin12.containsData());
        bin12.setBandIndex(0);
        bin12.write(0, 55f);
        assertTrue("contains data", bin12.containsData());
        bin12.clear();
        assertFalse("contains no data after clear", bin12.containsData());
    }
}
