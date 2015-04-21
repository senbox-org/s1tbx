package org.esa.snap.binning.support;

/**
 * Implementation of a regular gaussian grid.
 * <p>
 * A gaussian grid is a latitude/longitude grid. The spacing of the latitudes is not regular.
 * However, the spacing of the lines of latitude is symmetrical about the Equator. Note that
 * there is no latitude at either Pole or at the Equator. A grid is usually referred to by its
 * 'number' N, which is the number of lines of latitude between a Pole and the Equator.
 * <p>
 * The longitudes of the grid points are defined by giving the number of points along each
 * line of latitude. The first point is at longitude 0 and the points are equally spaced along
 * the line of latitude. In a regular gaussian grid, the number of longitude points along a
 * latitude is 4N. In a reduced gaussian grid, the number of longitude points along a latitude
 * is specified. Latitudes may have differing numbers of points but the grid is symmetrical
 * about the Equator.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @see ReducedGaussianGrid
 */
public final class RegularGaussianGrid extends AbstractGaussianGrid {

    /**
     * Creates a new regular gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public RegularGaussianGrid(int numRows) {
        super(numRows);
    }

    @Override
    public int getRowIndex(long binIndex) {
        return (int) (binIndex / getConfig().getRegularColumnCount());
    }

    @Override
    public long getNumBins() {
        return getNumRows() * getConfig().getRegularColumnCount();
    }

    @Override
    protected long getFirstBinIndexUnchecked(int rowIndex) {
        return rowIndex * getNumCols(rowIndex);
    }

    @Override
    protected int getNumColsUnchecked(int rowIndex) {
        return getConfig().getRegularColumnCount();
    }

    @Override
    protected double getCenterLon(int rowIndex, int colIndex) {
        return getConfig().getRegularLongitudePoints()[colIndex];
    }

}
