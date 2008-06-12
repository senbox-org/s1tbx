package org.esa.beam.framework.dataop.resamp;



/**
 * This class implements the Nearest Neighbour resampling method.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
final class NearestNeighbourResampling implements Resampling {

    public String getName() {
        return "NEAREST_NEIGHBOUR";
    }

    public final Index createIndex() {
        return new Index(0, 0);
    }

    public final void computeIndex(final float x,
                                   final float y,
                                   int width, int height, final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        index.i0 = Index.crop((int) Math.floor(x), width - 1);
        index.j0 = Index.crop((int) Math.floor(y), height - 1);
    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {
        return raster.getSample(index.i0, index.j0);
    }

    @Override
    public String toString() {
        return "Nearest neighbour resampling";
    }
}
