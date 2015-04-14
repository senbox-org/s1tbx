package org.esa.snap.binning;

/**
 * A function that computes a weight factor given a number of observations.
 */
public abstract class WeightFn {

    /**
     * Factory method that creates the function {@code pow(numObs, c)}.
     *
     * @param c The exponent.
     * @return The weight function.
     */
    public static WeightFn createPow(double c) {
        if (c == 0.0) {
            return new One();
        } else if (c == 0.5) {
            return new Sqrt();
        } else if (c == 1.0) {
            return new Ident();
        } else {
            return new Pow(c);
        }
    }

    /**
     * Computes a weight factor from the given number of observations.
     *
     * @param numObs The number of observations.
     * @return The computed weight.
     */
    public abstract float eval(int numObs);

    /////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private final static class Ident extends WeightFn {
        @Override
        public float eval(int numObs) {
            return (float) numObs;
        }
    }

    private final static class One extends WeightFn {
        @Override
        public float eval(int numObs) {
            return 1.0f;
        }
    }

    private final static class Sqrt extends WeightFn {
        @Override
        public float eval(int numObs) {
            return (float) Math.sqrt(numObs);
        }
    }

    private final static class Pow extends WeightFn {
        private final double c;

        private Pow(double c) {
            this.c = c;
        }

        @Override
        public float eval(int numObs) {
            return (float) Math.pow(numObs, c);
        }
    }
}
