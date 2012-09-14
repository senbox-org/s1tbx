package org.jdoris.core.stacks;

import org.jdoris.core.SLCImage;
import org.jdoris.core.Orbit;
import com.bc.ceres.core.ProgressMonitor;

/**
 * Interface to selecting an optimal master for insar
 */
public interface OptimalMaster {

    public void setInput(SLCImage[] slcImages, Orbit[] orbits);

    public MasterSelection.IfgStack[] getCoherenceScores(final ProgressMonitor pm);

    public int estimateOptimalMaster(final ProgressMonitor pm);

    public int findOptimalMaster(MasterSelection.IfgStack[] ifgStack);

    public float getModeledCoherence();

    public long getOrbitNumber();
}
