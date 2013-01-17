package org.esa.nest;

import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.StopWatch;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.util.MemUtils;

import javax.media.jai.JAI;
import java.io.File;

/**
 * Benchmark code
 */
public abstract class Benchmark extends TestCase {
    protected OperatorSpi spi;
    private final File outputFile = new File("e:\\out\\output.dim");

    public Benchmark() {
        try {
            initTestEnvironment();
            DataSets.instance();

            spi = CreateOperatorSpi();
            GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private static void initTestEnvironment() throws RuntimeConfigException {
        final RuntimeConfig runtimeConfig = new DefaultRuntimeConfig();

        JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());

        //disable JAI media library
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    @Override
    protected void setUp() throws Exception {

    }

    @Override
    protected void tearDown() throws Exception {

    }

    protected abstract OperatorSpi CreateOperatorSpi();

    protected final void process(final OperatorSpi spi, final Product product) throws Throwable {
        if(!BenchConstants.runBenchmarks) return;
        if(product == null) return;

        try {
            if(BenchConstants.numIterations > 1) {
                //warm up
                run(spi, product);
            }

            final StopWatch watch = new StopWatch();
            for(int i=0; i< BenchConstants.numIterations; ++i) {
                MemUtils.freeAllMemory();
                run(spi, product);
            }
            watch.stop();

            final String mission = AbstractMetadata.getAbstractedMetadata(product).getAttributeString(AbstractMetadata.MISSION);
            final int w = product.getSceneRasterWidth();
            final int h = product.getSceneRasterHeight();
            final long milliseconds = watch.getTimeDiff() / BenchConstants.numIterations;
            final int seconds = (int)((milliseconds / 1000.0) + 0.5);

            System.out.println(spi.getOperatorAlias() +' '+ mission +' '+ product.getProductType() +' '+w +'x'+ h
                    +" avg time: "+ StopWatch.getTimeString(milliseconds) +" ("+ seconds +" s)");
        } catch(Throwable t) {
            System.out.println("Test failed " + spi.getOperatorAlias() +' '+ product.getProductType());
            throw t;
        }
    }

    private void run(final OperatorSpi spi, final Product srcProduct) throws Throwable {
        final Operator op = spi.createOperator();
        op.setSourceProduct(srcProduct);
        setOperatorParameters(op);
        writeProduct(op);
        op.dispose();
    }

    public void writeProduct(final Operator op) {

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();

        final WriteOp writeOp = new WriteOp(targetProduct, outputFile, "BEAM-DIMAP");
        writeOp.setDeleteOutputOnFailure(true);
        writeOp.setWriteEntireTileRows(true);
        writeOp.setClearCacheAfterRowWrite(false);

        final OperatorExecutor executor = OperatorExecutor.create(writeOp);
        executor.execute(new NullProgressMonitor());
    }

    protected void setOperatorParameters(final Operator op) {
    }
}
