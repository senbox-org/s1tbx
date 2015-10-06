package org.esa.snap.core.util;

/**
 * A programmatic, functional for-loop. A {@link Body} is executed for each index combination of a given array of dimensions.
 * For example, given the dimensions {2, 3} (a 2D array) the body shall be called for the following sequence of indexes:
 * <pre>
 *     {0, 0}
 *     {0, 1}
 *     {0, 2}
 *     {1, 0}
 *     {1, 1}
 *     {1, 2}
 * </pre>
 * So higher indexes vary faster and correspond to inner loops.
 * It is possible to execute multiple different bodies using the same for-loop instance.
 *
 * @author Norman Fomferra
 */
public class ForLoop {

    public interface Body {
        void execute(int[] indexes, int[] sizes);
    }

    private final int[] sizes;
    private final int[] indexes;
    private final int dim;

    public ForLoop(int[] sizes) {
        this.sizes = sizes.clone();
        this.indexes = new int[sizes.length];
        this.dim = sizes.length - 1;
    }

    public static ForLoop execute(int[] sizes, Body body) {
        final ForLoop forLoop = new ForLoop(sizes);
        forLoop.execute(body);
        return forLoop;
    }

    public ForLoop execute(Body body) {
        if (dim >= 0) {
            loopN(body, dim);
        }
        return this;
    }

    private void loopN(Body body, int off) {
        for (int k = 0; k < sizes[dim - off]; k++) {
            indexes[dim - off] = k;
            if (off == 0) {
                body.execute(indexes, sizes);
            } else {
                loopN(body, off - 1);
            }
        }
    }
}
