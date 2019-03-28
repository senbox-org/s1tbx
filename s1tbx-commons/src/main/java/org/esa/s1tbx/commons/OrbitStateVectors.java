package org.esa.s1tbx.commons;

import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OrbitStateVectors {

    public OrbitStateVector[] orbitStateVectors = null;
    public PosVector[] sensorPosition = null; // sensor position for all range lines
    public PosVector[] sensorVelocity = null; // sensor velocity for all range lines
    private double dt = 0.0;
    private final Map<Double, PositionVelocity> timeMap = new HashMap<>();

    private static final int nv = 8;

    public OrbitStateVectors(final OrbitStateVector[] orbitStateVectors,
                             final double firstLineUTC, final double lineTimeInterval, final int sourceImageHeight) {

        this.orbitStateVectors = removeRedundantVectors(orbitStateVectors);

        this.dt = (this.orbitStateVectors[this.orbitStateVectors.length - 1].time_mjd -
                this.orbitStateVectors[0].time_mjd) / (this.orbitStateVectors.length - 1);

        this.sensorPosition = new PosVector[sourceImageHeight];
        this.sensorVelocity = new PosVector[sourceImageHeight];
        for (int i = 0; i < sourceImageHeight; i++) {
            final double time = firstLineUTC + i * lineTimeInterval;
            PositionVelocity pv = getPositionVelocity(time);
            sensorPosition[i] = pv.position;
            sensorVelocity[i] = pv.velocity;
        }
    }

    public OrbitStateVectors(final OrbitStateVector[] orbitStateVectors) {

        this.orbitStateVectors = removeRedundantVectors(orbitStateVectors);

        this.dt = (this.orbitStateVectors[orbitStateVectors.length - 1].time_mjd -
                this.orbitStateVectors[0].time_mjd) / (this.orbitStateVectors.length - 1);
    }

    private static OrbitStateVector[] removeRedundantVectors(OrbitStateVector[] orbitStateVectors) {

        final List<OrbitStateVector> vectorList = new ArrayList<>();
        double currentTime = 0.0;
        for (int i = 0; i < orbitStateVectors.length; i++) {
            if (i == 0) {
                currentTime = orbitStateVectors[i].time_mjd;
                vectorList.add(orbitStateVectors[i]);
            } else if (orbitStateVectors[i].time_mjd > currentTime) {
                currentTime = orbitStateVectors[i].time_mjd;
                vectorList.add(orbitStateVectors[i]);
            }
        }

        return vectorList.toArray(new OrbitStateVector[0]);
    }

    public PositionVelocity getPositionVelocity(final Double time) {

        PositionVelocity cachedPosVel = timeMap.get(time);
        if(cachedPosVel != null) {
            return cachedPosVel;
        }

        int i0, iN;
        if (orbitStateVectors.length <= nv) {
            i0 = 0;
            iN = orbitStateVectors.length - 1;
        } else {
            i0 = Math.max((int) ((time - orbitStateVectors[0].time_mjd) / dt) - nv / 2 + 1, 0);
            iN = Math.min(i0 + nv - 1, orbitStateVectors.length - 1);
            i0 = (iN < orbitStateVectors.length - 1 ? i0 : iN - nv + 1);
        }

        //lagrangeInterpolatingPolynomial
        final PositionVelocity pv = new PositionVelocity();

        for (int i = i0; i <= iN; ++i) {
            final OrbitStateVector orbI = orbitStateVectors[i];

            double weight = 1;
            for (int j = i0; j <= iN; ++j) {
                if (j != i) {
                    final double time2 = orbitStateVectors[j].time_mjd;
                    weight *= (time - time2) / (orbI.time_mjd - time2);
                }
            }

            pv.position.x += weight * orbI.x_pos;
            pv.position.y += weight * orbI.y_pos;
            pv.position.z += weight * orbI.z_pos;

            pv.velocity.x += weight * orbI.x_vel;
            pv.velocity.y += weight * orbI.y_vel;
            pv.velocity.z += weight * orbI.z_vel;
        }

        timeMap.put(time, pv);
        return pv;
    }

    PosVector getPosition(final double time, final PosVector position) {

        int i0, iN;
        if (orbitStateVectors.length <= nv) {
            i0 = 0;
            iN = orbitStateVectors.length - 1;
        } else {
            i0 = Math.max((int) ((time - orbitStateVectors[0].time_mjd) / dt) - nv / 2 + 1, 0);
            iN = Math.min(i0 + nv - 1, orbitStateVectors.length - 1);
            i0 = (iN < orbitStateVectors.length - 1 ? i0 : iN - nv + 1);
        }

        //lagrangeInterpolatingPolynomial
        position.x = 0;
        position.y = 0;
        position.z = 0;

        for (int i = i0; i <= iN; ++i) {
            final OrbitStateVector orbI = orbitStateVectors[i];

            double weight = 1;
            for (int j = i0; j <= iN; ++j) {
                if (j != i) {
                    final double time2 = orbitStateVectors[j].time_mjd;
                    weight *= (time - time2) / (orbI.time_mjd - time2);
                }
            }
            position.x += weight * orbI.x_pos;
            position.y += weight * orbI.y_pos;
            position.z += weight * orbI.z_pos;
        }
        return position;
    }

    PosVector getVelocity(final double time) {

        int i0, iN;
        if (orbitStateVectors.length <= nv) {
            i0 = 0;
            iN = orbitStateVectors.length - 1;
        } else {
            i0 = Math.max((int) ((time - orbitStateVectors[0].time_mjd) / dt) - nv / 2 + 1, 0);
            iN = Math.min(i0 + nv - 1, orbitStateVectors.length - 1);
            i0 = (iN < orbitStateVectors.length - 1 ? i0 : iN - nv + 1);
        }

        //lagrangeInterpolatingPolynomial
        final PosVector velocity = new PosVector();

        for (int i = i0; i <= iN; ++i) {
            final OrbitStateVector orbI = orbitStateVectors[i];

            double weight = 1;
            for (int j = i0; j <= iN; ++j) {
                if (j != i) {
                    final double time2 = orbitStateVectors[j].time_mjd;
                    weight *= (time - time2) / (orbI.time_mjd - time2);
                }
            }
            velocity.x += weight * orbI.x_vel;
            velocity.y += weight * orbI.y_vel;
            velocity.z += weight * orbI.z_vel;
        }
        return velocity;
    }

    private int[] findAdjacentVectors(final double time) {

        int[] vectorIndices;
        final int nv = 8;
        final int totalVectors = orbitStateVectors.length;
        if (totalVectors <= nv) {
            vectorIndices = new int[totalVectors];
            for (int i = 0; i < totalVectors; i++) {
                vectorIndices[i] = i;
            }
            return vectorIndices;
        }

        int idx = 0;
        for (int i = 0; i < totalVectors; i++) {
            if (time < orbitStateVectors[i].time_mjd) {
                idx = i - 1;
                break;
            }
        }

        if (idx == -1) {
            vectorIndices = new int[nv];
            for (int i = 0; i < nv; i++) {
                vectorIndices[i] = i;
            }
        } else if (idx == 0) {
            vectorIndices = new int[nv];
            for (int i = 0; i < nv; i++) {
                vectorIndices[i] = totalVectors - nv + i;
            }
        } else {
            final int stIdx = Math.max(idx - nv / 2 + 1, 0);
            final int edIdx = Math.min(idx + nv / 2, totalVectors - 1);
            vectorIndices = new int[edIdx - stIdx + 1];
            for (int i = 0; i < vectorIndices.length; i++) {
                vectorIndices[i] = stIdx + i;
            }
        }

        return vectorIndices;
    }

    public static class PositionVelocity {
        public final PosVector position = new PosVector();
        public final PosVector velocity = new PosVector();
    }
}
