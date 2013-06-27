package org.jlinda.core.stacks;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.AbstractMetadata;
import org.jlinda.core.*;
import org.jlinda.core.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User: pmar@ppolabs.com
 * Date: 1/20/12
 * Time: 3:39 PM
 */
public class MasterSelection implements OptimalMaster {

    private final static int BTEMP_CRITICAL = 3 * 365;
    private final static int BPERP_CRITICAL = 1200;
    private final static int DFDC_CRITICAL = 1380;

    private SLCImage[] slcImages;
    private Orbit[] orbits;
    private int numOfImages;

    private long orbitNumber;
    private float modeledCoherence;

    // TODO: function to sort input array according to modeled coherence
    // TODO: critical values for other sensors then C.band ESA

    public MasterSelection() {
    }

    public void setInput(SLCImage[] slcImages, Orbit[] orbits) {
        this.slcImages = slcImages;
        this.orbits = orbits;
        this.numOfImages = slcImages.length;

        if (this.numOfImages != orbits.length) {
            throw new IllegalArgumentException("Number of elements in input arrays has to be the same!");
        }
    }

    // returns orbit number of an "optimal master"
    public long getOrbitNumber() {
        return orbitNumber;
    }

    public float getModeledCoherence() {
        return modeledCoherence;
    }

    // methods
    private static float modelCoherence(float bPerp, float bTemp, float fDc, float bPerpCritical,
                                 float bTempCritical, float fDcCritical) {
        return coherenceFnc(bPerp, bPerpCritical) * coherenceFnc(bTemp, bTempCritical) * coherenceFnc(fDc, fDcCritical);
    }

    private static float modelCoherence(float bPerp, float bTemp, float fDc) {
        return coherenceFnc(bPerp, BPERP_CRITICAL) * coherenceFnc(bTemp, BTEMP_CRITICAL) * coherenceFnc(fDc, DFDC_CRITICAL);
    }

    private static float coherenceFnc(float value, float value_CRITICAL) {
        if (Math.abs(value) > value_CRITICAL) {
            return (float) 0.01;
        } else {
            return (1 - Math.abs(value) / value_CRITICAL);
        }
    }

    // setup cplxcontainer
    private CplxContainer[] setupCplxContainers() {
        CplxContainer[] cplxContainers = new CplxContainer[numOfImages];

        for (int i = 0; i < numOfImages; i++) {
            SLCImage slcImage = slcImages[i];
            Orbit orbit = orbits[i];
            cplxContainers[i] = new CplxContainer(slcImage.getOrbitNumber(), slcImage.getMjd(), slcImage, orbit);
        }
        return cplxContainers;
    }

    private IfgStack[] setupIfgStack(CplxContainer[] cplxContainers, ProgressMonitor pm) {
        // construct pairs from data in containers
        IfgStack[] ifgStack = new IfgStack[numOfImages];
        IfgPair[][] ifgPair = new IfgPair[numOfImages][numOfImages];

        pm.beginTask("Computing...", numOfImages);
        for (int i = 0; i < numOfImages; i++) {

            CplxContainer master = cplxContainers[i];

            for (int j = 0; j < numOfImages; j++) {

                CplxContainer slave = cplxContainers[j];
                ifgPair[i][j] = new IfgPair(master, slave);

            }

            ifgStack[i] = new IfgStack(master, ifgPair[i]);
            ifgStack[i].meanCoherence();

            pm.worked(1);
        }
        pm.done();
        return ifgStack;
    }

    public int findOptimalMaster(IfgStack[] ifgStack) {

        orbitNumber = ifgStack[0].master.orbitNumber;
        modeledCoherence = ifgStack[0].meanCoherence;

        int index = 0;
        int i = 0;
        for (IfgStack anIfgStack : ifgStack) {

            long orbit = anIfgStack.master.orbitNumber;
            float coherence = anIfgStack.meanCoherence;

            if (coherence > modeledCoherence) {
                modeledCoherence = coherence;
                orbitNumber = orbit;
                index = i;
            }
            ++i;
        }
        return index;
    }

    public IfgStack[] getCoherenceScores(final ProgressMonitor pm) {
        CplxContainer[] cplxContainers = setupCplxContainers();
        return setupIfgStack(cplxContainers, pm);
    }

    public int estimateOptimalMaster(final ProgressMonitor pm) {
        return findOptimalMaster(getCoherenceScores(pm));
    }

    /**
     * Finds the optimal master product from a list of products
     * @param srcProducts input products
     * @return the optimal master product
     */
    public static Product findOptimalMasterProduct(final Product[] srcProducts) {
        final int size = srcProducts.length;
        final List<SLCImage> imgList = new ArrayList<SLCImage>(size);
        final List<Orbit> orbList = new ArrayList<Orbit>(size);

        for(Product product : srcProducts) {
            try {
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final SLCImage img = new SLCImage(absRoot);
                final Orbit orb = new Orbit(absRoot, 3);

                imgList.add(img);
                orbList.add(orb);
            } catch(Exception e) {
                VisatApp.getApp().showErrorDialog("Error: "+product.getName()+'\n'+e.getMessage());
            }
        }

        try {
            if(VisatApp.getApp() != null) {
                final Worker worker = new Worker(VisatApp.getApp().getMainFrame(), "Computing Optimal InSAR Master",
                        imgList.toArray(new SLCImage[size]), orbList.toArray(new Orbit[size]));
                worker.executeWithBlocking();

                Integer index = (Integer)worker.get();
                return srcProducts[index];
            }  else {

                final OptimalMaster dataStack = new MasterSelection();
                dataStack.setInput(imgList.toArray(new SLCImage[size]), orbList.toArray(new Orbit[size]));
                int index = dataStack.estimateOptimalMaster(ProgressMonitor.NULL);
                return srcProducts[index];
            }

        } catch(Throwable t) {
            Debug.trace(t);
        }
        return srcProducts[0];
    }


    private static class Worker extends ProgressMonitorSwingWorker {
        SLCImage[] imgList;
        Orbit[] orbList;
        Worker(final Component component, final String title,
               final SLCImage[] imgList, final Orbit[] orbList) {
            super(component, title);
            this.imgList = imgList;
            this.orbList = orbList;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            final OptimalMaster dataStack = new MasterSelection();
            dataStack.setInput(imgList, orbList);
            return dataStack.estimateOptimalMaster(pm);
        }
    }


    // inner classes
    private static class CplxContainer {

        public long orbitNumber;
        public double dateMjd;
        public SLCImage metaData;
        public Orbit orbit;

        public CplxContainer(double dateMjd, SLCImage metaData, Orbit orbit) {
            this.dateMjd = dateMjd;
            this.metaData = metaData;
            this.orbit = orbit;
        }

        public CplxContainer(long orbitNumber, double dateMjd, SLCImage metaData, Orbit orbit) {
            this.orbitNumber = orbitNumber;
            this.dateMjd = dateMjd;
            this.metaData = metaData;
            this.orbit = orbit;
        }

        public CplxContainer(long orbitNumber, SLCImage metaData) {
            this.orbitNumber = orbitNumber;
            this.metaData = metaData;
        }
    }


    public static class IfgStack {

        CplxContainer master;
        IfgPair[] master_slave;
        float meanCoherence;

        public IfgStack(CplxContainer master, IfgPair... master_slave) {
            this.master = master;
            this.master_slave = master_slave;
        }

        public void meanCoherence() {
            for (IfgPair aMaster_slave : master_slave) {
                meanCoherence += aMaster_slave.coherence;
            }
            meanCoherence /= master_slave.length;
        }

        public IfgPair[] getMasterSlave() {
            return master_slave;
        }
    }

    public static class IfgPair {

        int refLine;
        int refPixel;
        double refHeight;

        CplxContainer master;
        CplxContainer slave;

        float bPerp;   // perpendicular baseline
        float bTemp;   // temporal baseline
        float deltaDoppler;     // doppler centroid frequency difference
        float coherence;     // modeled coherence
        float heightAmb;     // modeled coherence

        public IfgPair(CplxContainer master, CplxContainer slave) {

            this.master = master;
            this.slave = slave;

            final Point refPoint = master.metaData.getApproxRadarCentreOriginal();
            this.refPixel = (int) refPoint.x;
            this.refLine = (int) refPoint.y;
            this.refHeight = 0;

            try {
                Baseline baseline = new Baseline();
                baseline.model(master.metaData, slave.metaData, master.orbit, slave.orbit);
                bPerp = (float) baseline.getBperp(refLine, refPixel);
                heightAmb = (float) baseline.getHamb(refLine, refPixel, refHeight);

            } catch (Exception e) {
                e.printStackTrace();
            }

            bTemp = (float) (master.dateMjd - slave.dateMjd);
            deltaDoppler = (float) (master.metaData.doppler.getF_DC_a0() - slave.metaData.doppler.getF_DC_a0());

            coherence = modelCoherence(bPerp, bTemp, deltaDoppler);

        }

        public float getPerpendicularBaseline() {
            return bPerp;
        }

        public float getTemporalBaseline() {
            return bTemp;
        }

        public float getDopplerDifference() {
            return deltaDoppler;
        }

        public float getCoherence() {
            return coherence;
        }

        public float getHeightAmb() {
            return heightAmb;
        }

        public SLCImage getMasterMetadata() {
            return master.metaData;   
        }

        public SLCImage getSlaveMetadata() {
            return slave.metaData;   
        }
    }

}
