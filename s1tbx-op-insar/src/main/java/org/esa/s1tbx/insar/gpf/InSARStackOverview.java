/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.Debug;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.jlinda.core.Baseline;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: pmar@ppolabs.com
 * Date: 1/20/12
 * Time: 3:39 PM
 */
public class InSARStackOverview {

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

    public InSARStackOverview() {
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
     *
     * @param srcProducts input products
     * @return the optimal master product
     */
    public static Product findOptimalMasterProduct(final Product[] srcProducts) throws Exception {
        final int size = srcProducts.length;
        final List<SLCImage> imgList = new ArrayList<>(size);
        final List<Orbit> orbList = new ArrayList<>(size);

        for (Product product : srcProducts) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            imgList.add(new SLCImage(absRoot, product));
            orbList.add(new Orbit(absRoot, 3));
        }

        try {
            final InSARStackOverview dataStack = new InSARStackOverview();
            dataStack.setInput(imgList.toArray(new SLCImage[size]), orbList.toArray(new Orbit[size]));
            int index = dataStack.estimateOptimalMaster(ProgressMonitor.NULL);
            return srcProducts[index];

        } catch (Throwable t) {
            Debug.trace(t);
        }
        return srcProducts[0];
    }

    public static InSARStackOverview.IfgStack[] calculateInSAROverview(final Product coregProduct) throws Exception {

        MetadataElement slaveElem = coregProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (slaveElem == null) {
            slaveElem = coregProduct.getMetadataRoot().getElement("Slave Metadata");
        }

        final List<MetadataElement> absMetaList = new ArrayList<>();
        absMetaList.add(AbstractMetadata.getAbstractedMetadata(coregProduct));
        absMetaList.addAll(Arrays.asList(slaveElem.getElements()));

        return InSARStackOverview.calculateInSAROverview(absMetaList.toArray(new MetadataElement[absMetaList.size()]));
    }

    public static InSARStackOverview.IfgStack[] calculateInSAROverview(final MetadataElement[] absRoots) throws Exception {
        final List<SLCImage> imgList = new ArrayList<>(absRoots.length);
        final List<Orbit> orbList = new ArrayList<>(absRoots.length);

        for (MetadataElement absRoot : absRoots) {
            final AbstractMetadata.DopplerCentroidCoefficientList[] dopplersArray = AbstractMetadata.getDopplerCentroidCoefficients(absRoot);
            if(dopplersArray.length == 0) {
                return null;
            }

            imgList.add(new SLCImage(absRoot, null));
            orbList.add(new Orbit(absRoot, 3));
        }

        final InSARStackOverview dataStack = new InSARStackOverview();
        dataStack.setInput(imgList.toArray(new SLCImage[imgList.size()]), orbList.toArray(new Orbit[orbList.size()]));
        return dataStack.getCoherenceScores(ProgressMonitor.NULL);
    }

    public static InSARStackOverview.IfgStack[] calculateInSAROverview(final Product[] products) throws Exception {
        final List<SLCImage> imgList = new ArrayList<>(products.length);
        final List<Orbit> orbList = new ArrayList<>(products.length);

        for (Product product : products) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            imgList.add(new SLCImage(absRoot, product));
            orbList.add(new Orbit(absRoot, 3));
        }

        final InSARStackOverview dataStack = new InSARStackOverview();
        dataStack.setInput(imgList.toArray(new SLCImage[imgList.size()]), orbList.toArray(new Orbit[orbList.size()]));
        return dataStack.getCoherenceScores(ProgressMonitor.NULL);
    }

    // inner classes
    private static class CplxContainer {

        public final long orbitNumber;
        public final double dateMjd;
        public final SLCImage metaData;
        public final Orbit orbit;

        public CplxContainer(long orbitNumber, double dateMjd, SLCImage metaData, Orbit orbit) {
            this.orbitNumber = orbitNumber;
            this.dateMjd = dateMjd;
            this.metaData = metaData;
            this.orbit = orbit;
        }
    }

    public static class IfgStack {

        private final CplxContainer master;
        private final IfgPair[] master_slave;
        private float meanCoherence;

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

        private final int refLine, refPixel;
        private final double refHeight;

        private final CplxContainer master, slave;
        private Baseline baseline = null;

        private float bPerp;            // perpendicular baseline
        private final float bTemp;            // temporal baseline
        private final float deltaDoppler;     // doppler centroid frequency difference
        private final float coherence;        // modeled coherence
        private float heightAmb;        // modeled coherence

        public IfgPair(CplxContainer master, CplxContainer slave) {

            this.master = master;
            this.slave = slave;

            final Point refPoint = master.metaData.getApproxRadarCentreOriginal();
            this.refPixel = (int) refPoint.x;
            this.refLine = (int) refPoint.y;
            this.refHeight = 0;

            try {
                baseline = new Baseline();
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

        public float getPerpendicularBaseline(final double line, final double pixel, final double height) throws Exception {
            return (float) baseline.getBperp(line, pixel, height);
        }

        public float getParallelBaseline(final double line, final double pixel, final double height) throws Exception {
            return (float) baseline.getBpar(line, pixel, height);
        }

        public float getAlpha(final double line, final double pixel, final double height) throws Exception {
            return (float) baseline.getAlpha(line, pixel, height);
        }

        public float getVerticalBaseline(final double line, final double pixel, final double height) throws Exception {
            return (float) baseline.getBvert(line, pixel, height);
        }

        public float getHorizontalBaseline(final double line, final double pixel, final double height) throws Exception {
            return (float) baseline.getBhor(line, pixel, height);
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
