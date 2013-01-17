package org.esa.nest;

/**
 * Benchmark code
 */
public abstract class SingleOperatorBenchmark extends Benchmark {

    protected boolean skipS1 = false;

    //RS-2
    public void testPerf_RS2_Quad() throws Throwable {
        process(spi, DataSets.instance().RS2_quad_product);
    }

    //ASAR
    public void testPerf_ASAR_IMS() throws Throwable {
        process(spi, DataSets.instance().ASAR_IMS_product);
    }

    public void testPerf_ASAR_IMP() throws Throwable {
        process(spi, DataSets.instance().ASAR_IMP_product);
    }

    public void testPerf_ASAR_APP() throws Throwable {
        process(spi, DataSets.instance().ASAR_APP_product);
    }

    public void testPerf_ASAR_APS() throws Throwable {
        process(spi, DataSets.instance().ASAR_APS_product);
    }

    public void testPerf_ASAR_WSM() throws Throwable {
        process(spi, DataSets.instance().ASAR_WSM_product);
    }

    //ERS-2 CEOS
    public void testPerf_ERS2_PRI() throws Throwable {
        process(spi, DataSets.instance().ERS2_PRI_product);
    }

    public void testPerf_ERS2_SLC() throws Throwable {
        process(spi, DataSets.instance().ERS2_SLC_product);
    }

    //ERS-2
    public void testPerf_ERS2_IMP() throws Throwable {
        process(spi, DataSets.instance().ERS2_IMP_product);
    }

    public void testPerf_ERS2_IMS() throws Throwable {
        process(spi, DataSets.instance().ERS2_IMS_product);
    }

    //ALOS
    public void testPerf_ALOS_L11() throws Throwable {
        process(spi, DataSets.instance().ALOS_L11_product);
    }

    //TerraSAR-X
    public void testPerf_TSX_SSC() throws Throwable {
        process(spi, DataSets.instance().TSX_SSC_product);
    }

    public void testPerf_TSX_SSC_Quad() throws Throwable {
        process(spi, DataSets.instance().TSX_SSC_Quad_product);
    }

    //S-1
    public void testPerf_S1_GRD() throws Throwable {
        if(skipS1) return;
        process(spi, DataSets.instance().S1_GRD_product);
    }

}
