package org.jlinda.core.stacks;

import org.jlinda.core.SLCImage;
import org.jlinda.core.Orbit;
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
