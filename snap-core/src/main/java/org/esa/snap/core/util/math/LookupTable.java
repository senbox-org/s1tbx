/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.util.math;

import java.text.MessageFormat;

/**
 * The class {@code LookupTable} performs the function of multilinear
 * interpolation for lookup tables with an arbitrary number of dimensions.
 * <p>
 * todo - method for degrading a table (see C++ code below)
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class LookupTable {

    /**
     * The lookup values.
     */
    private final Array values;
    /**
     * The dimensions associated with the lookup table.
     */
    private final IntervalPartition[] dimensions;
    /**
     * The strides defining the layout of the lookup value array.
     */
    private final int[] strides;
    /**
     * The relative array offsets of the lookup values for the vertices of a coordinate grid cell.
     */
    private final int[] o;

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is not equal to
     *                                  the number of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final IntervalPartition... dimensions) {
        this(new Array.Double(values), dimensions);
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is not equal to
     *                                  the number of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final float[] values, final IntervalPartition... dimensions) {
        this(new Array.Float(values), dimensions);
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is is not equal to
     *                                  the number of coordinate grid vertices or any dimension is
     *                                  not an interval partion.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final double[]... dimensions) {
        this(values, IntervalPartition.createArray(dimensions));
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is is not equal to
     *                                  the number of coordinate grid vertices or any dimension is
     *                                  not an interval partion.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final float[] values, final float[]... dimensions) {
        this(values, IntervalPartition.createArray(dimensions));
    }

    private LookupTable(final Array values, final IntervalPartition... dimensions) {
        ensureLegalArray(dimensions);
        ensureLegalArray(values, getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;

        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = 1; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        computeVertexOffsets(strides, o);
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    public final IntervalPartition[] getDimensions() {
        return dimensions;
    }

    protected int[] getStrides() {
        return strides;
    }

    protected int[] getOffsets() {
        return o;
    }

    protected Array getValues() {
        return values;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     *
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(final int i) {
        return dimensions[i];
    }

    /**
     * Returns an interpolated value for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     *
     * @return the interpolated value.
     *
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double getValue(final double... coordinates) throws IllegalArgumentException, NullPointerException {
        return getValue(coordinates, FracIndex.createArray(coordinates.length), new double[1 << coordinates.length]);
    }

    /**
     * Returns an interpolated value for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @param fracIndexes workspace array of (at least) the same length as {@code coordinates}.
     * @param v           workspace array of (at least) length {@code 1 << coordinates.length}.
     *
     * @return the interpolated value.
     *
     * @throws ArrayIndexOutOfBoundsException if the {@code fracIndexes} and {@code v} arrays
     *                                        do not have proper length.
     * @throws IllegalArgumentException       if the length of the {@code coordinates} array is
     *                                        not equal to the number of dimensions associated
     *                                        with the lookup table.
     * @throws NullPointerException           if any parameter is {@code null} or exhibits any
     *                                        element, which is {@code null}.
     */
    public final double getValue(final double[] coordinates, final FracIndex[] fracIndexes, final double[] v)
            throws IllegalArgumentException, IndexOutOfBoundsException, NullPointerException {
        ensureLegalArray(coordinates, dimensions.length);

        for (int i = 0; i < dimensions.length; ++i) {
            computeFracIndex(dimensions[i], coordinates[i], fracIndexes[i]);
        }

        return getValue(fracIndexes, v);
    }

    /**
     * Returns an interpolated value for the given fractional indices.
     *
     * @param fracIndexes workspace array of (at least) the same length as {@code coordinates}.
     * @param v           workspace array of (at least) length {@code 1 << coordinates.length}.
     *
     * @return the interpolated value.
     *
     * @throws ArrayIndexOutOfBoundsException if the {@code fracIndexes} and {@code v} arrays
     *                                        do not have proper length.
     * @throws IllegalArgumentException       if the length of the {@code coordinates} array is
     *                                        not equal to the number of dimensions associated
     *                                        with the lookup table.
     * @throws NullPointerException           if any parameter is {@code null} or exhibits any
     *                                        element, which is {@code null}.
     */
    public final double getValue(final FracIndex[] fracIndexes, final double[] v) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        for (int i = 0; i < v.length; ++i) {
            v[i] = values.getValue(origin + o[i]);
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                v[j] += f * (v[m + j] - v[j]);
            }
        }

        return v[0];
    }

    /**
     * Returns an array of interpolated values for the given fractional indices.
     *
     * @param fracIndexes workspace array of length {@code coordinates - 1}.
     *
     * @return the interpolated values.
     *
     * @throws ArrayIndexOutOfBoundsException if the {@code fracIndexes} and {@code v} arrays
     *                                        do not have proper length.
     * @throws IllegalArgumentException       if the length of the {@code coordinates} array is
     *                                        not exactly one less than the number of dimensions associated
     *                                        with the lookup table.
     * @throws NullPointerException           if {@code fracIndexes} is {@code null} or exhibits any
     *                                        element, which is {@code null}.
     */
    public final double[] getValues(final FracIndex[] fracIndexes) {

        if (fracIndexes.length != strides.length - 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "fracIndexes.length = {0} does not correspond to the expected length {1}",
                    fracIndexes.length, strides.length - 1));
        }

        final int resultLength = strides[fracIndexes.length - 1];
        double[][] v = new double[1 << fracIndexes.length][resultLength];

        int origin = 0;
        for (int i = 0; i < fracIndexes.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        for (int i = 0; i < v.length; ++i) {
            values.copyTo(origin + o[i], v[i], 0, resultLength);
        }
        for (int i = fracIndexes.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                for (int k = 0; k < resultLength; k++) {
                    v[j][k] += f * (v[m + j][k] - v[j][k]);
                }
            }
        }
        return v[0];
    }

    /**
     * Computes the {@link FracIndex} of a coordinate value with respect to a given
     * interval partition. The integral component of the returned {@link FracIndex}
     * corresponds to the index of the maximum partition member which is less than
     * or equal to the coordinate value. The [0, 1) fractional component describes
     * the position of the coordinate value within its bracketing subinterval.
     * <p>
     * Exception: If the given coordinate value is equal to the partition maximum,
     * the fractional component of the returned {@link FracIndex} is equal to 1.0,
     * and the integral component is set to the index of the next to last partition
     * member.
     *
     * @param partition  the interval partition.
     * @param coordinate the coordinate value. If the coordinate value is less (greater)
     *                   than the minimum (maximum) of the given interval partition,
     *                   the returned {@link FracIndex} is the same as if the coordinate.
     *                   value was equal to the partition minimum (maximum).
     * @param fracIndex  the {@link FracIndex}.
     */
    public final static void computeFracIndex(final IntervalPartition partition, final double coordinate,
                                 final FracIndex fracIndex) {
        int lo = 0;
        int hi = partition.getCardinal() - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < partition.get(m)) {
                hi = m;
            } else {
                lo = m;
            }
        }

        fracIndex.i = lo;
        fracIndex.f = (coordinate - partition.get(lo)) / (partition.get(hi) - partition.get(lo));
        fracIndex.truncate();
    }

    /**
     * Computes the relative array offsets of the lookup values for the vertices
     * of a coordinate grid cell.
     *
     * @param strides the strides defining the layout of the lookup value array.
     * @param offsets the offsets.
     */
    static void computeVertexOffsets(final int[] strides, final int[] offsets) {
        for (int i = 0; i < strides.length; ++i) {
            final int k = 1 << i;

            for (int j = 0; j < k; ++j) {
                offsets[k + j] = offsets[j] + strides[i];
            }
        }
    }

    /**
     * Returns the number of vertices in the coordinate grid defined by the given dimensions.
     *
     * @param dimensions the dimensions defining the coordinate grid.
     *
     * @return the number of vertices.
     */
    static int getVertexCount(final IntervalPartition[] dimensions) {
        int count = 1;

        for (final IntervalPartition dimension : dimensions) {
            count *= dimension.getCardinal();
        }

        return count;
    }

    static <T> void ensureLegalArray(final T[] array) throws IllegalArgumentException, NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("array.length == 0");
        }
        for (final T element : array) {
            if (element == null) {
                throw new NullPointerException("element == null");
            }
        }
    }

    static void ensureLegalArray(final float[] array, final int length) throws
                                                                        IllegalArgumentException,
                                                                        NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
    }

    static void ensureLegalArray(final double[] array, final int length) throws
                                                                         IllegalArgumentException,
                                                                         NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
    }

    static void ensureLegalArray(Array array, final int length) throws
                                                                IllegalArgumentException,
                                                                NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.getLength() != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.getLength(), length));
        }
    }
}

/*
#ifndef BASIC_LOOKUP_TABLE_INC
#define BASIC_LOOKUP_TABLE_INC

#include <algorithm>
#include <cassert>
#include <iostream>
#include <valarray>

using namespace std;

template<class Wp>
class AerLookupTable {
public:
	typedef valarray<Wp> Axis;

	AerLookupTable();
	AerLookupTable(const Axis axes[], size_t numAxes);
	AerLookupTable(const Axis axes[], size_t numAxes, const Wp values[]);
	AerLookupTable(const Axis* axes[], size_t numAxes, const Wp values[]);
	~AerLookupTable();

	Wp operator()(const Wp coordinates[]) const;

	Wp maxCoordinate(size_t axisIndex) const;
	Wp minCoordinate(size_t axisIndex) const;
	bool isValidCoordinate(size_t axisIndex, Wp coordinate) const;

	void degrade(size_t axisIndex, Wp coordinate);

	void reset(const Axis axes[], size_t numAxes);
	void reset(const Axis axes[], size_t numAxes, const Wp values[]);
	void reset(const Axis* axes[], size_t numAxes, const Wp values[]);

    istream& read(istream& is);
    ostream& write(ostream& os) const;
	istream& readValues(istream& is);
	ostream& writeValues(ostream& os) const;

private:
    void updateSizesAndStrides();
	void getVertexes(const Wp coordinates[], size_t vertexes[]) const;
    void interpolate(Wp values[], const Wp values2[], size_t numValues,
    	Wp interpolationFactor) const;

	Wp interpolationFactor(size_t axisIndex, Wp coordinate, size_t vertex) const;

	size_t valueIndex(const size_t vertexes[]) const;
    size_t vertex(size_t axisIndex, Wp coordinate) const;

	static size_t numValues(const size_t sizes[], size_t numAxes);
    static size_t numValues(const Axis axes[], size_t numAxes);
    static size_t numValues(const Axis* axes[], size_t numAxes);
	static bool isBigEndian();

	template<class T>
	static istream& read(istream& is, T* first, size_t n);

	template<class T>
	static ostream& write(ostream& is, const T* first, size_t n);

	valarray<Axis> _x;
		// coordinate axes = vertex coordinates
	valarray<Wp> _y;
		// tabulated values

	valarray<size_t> _sizes;
	valarray<size_t> _strides;
	valarray<size_t> _indexes;

    size_t _n;
    	// table dimension = number of coordinate axes
};

template<class Wp>
AerLookupTable<Wp>::AerLookupTable()
	:	_x(), _y(), _sizes(), _strides(), _indexes(), _n(0)
{
}

template<class Wp>
AerLookupTable<Wp>::AerLookupTable(const Axis axes[], size_t numAxes)
	:	_x(), _y(), _sizes(), _strides(), _indexes(), _n(0)
{
	reset(axes, numAxes);
}

template<class Wp>
AerLookupTable<Wp>::AerLookupTable(const Axis axes[], size_t numAxes, const Wp values[])
	:	_x(), _y(), _sizes(), _strides(), _indexes(), _n(0)
{
	reset(axes, numAxes, values);
}

template<class Wp>
AerLookupTable<Wp>::AerLookupTable(const Axis* axes[], size_t numAxes, const Wp values[])
	:	_x(), _y(), _sizes(), _strides(), _indexes(), _n(0)
{
	reset(axes, numAxes, values);
}

template<class Wp>
AerLookupTable<Wp>::~AerLookupTable()
{
}

template<class Wp>
Wp
AerLookupTable<Wp>::operator()(const Wp coordinates[]) const
{
	using std::valarray;

	valarray<size_t> v(_n);
	getVertexes(coordinates, &v[0]);

	valarray<Wp> values = _y[_indexes + valueIndex(&v[0])];
		// extract the y-values at the vertexes of the smallest n-cube
        // containing the interpolation point
	for (size_t i = 0, j = values.size(); j >>= 1 != 0; ++i)
		interpolate(&values[0], &values[j], j,
			interpolationFactor(i, coordinates[i], v[i]));

	return values[0];
}

template<class Wp>
inline
Wp
AerLookupTable<Wp>::maxCoordinate(size_t axisIndex) const
{
	assert (axisIndex < _n and _sizes[axisIndex] > 0);
	return _x[axisIndex][_sizes[axisIndex] - 1];
}


template<class Wp>
inline
Wp
AerLookupTable<Wp>::minCoordinate(size_t axisIndex) const
{
	assert (axisIndex < _n and _sizes[axisIndex] > 0);
	return _x[axisIndex][0];
}

template<class Wp>
bool
AerLookupTable<Wp>::isValidCoordinate(size_t axisIndex, Wp coordinate) const
{
	return coordinate >= minCoordinate(axisIndex) and
		coordinate <= maxCoordinate(axisIndex);
}

template<class Wp>
void
AerLookupTable<Wp>::degrade(size_t axisIndex, Wp coordinate)
{
	using std::gslice;
	using std::valarray;

    if (_n > 1) {
        const size_t v = vertex(axisIndex, coordinate);
    	const size_t start = v * _strides[axisIndex];

        _sizes[axisIndex] = 1;

        valarray<Wp> values = _y[gslice(start, _sizes, _strides)];
        const valarray<Wp> values2 = _y[gslice(start + _strides[axisIndex], _sizes, _strides)];
        valarray<Axis> axes(_n - 1);

        for (size_t i = 0; i < axisIndex; ++i) {
        	axes[i].resize(_sizes[i]);
        	axes[i] = _x[i];
        }
        for (size_t i = axisIndex; i < _n - 1; ++i) {
        	axes[i].resize(_sizes[i + 1]);
        	axes[i] = _x[i + 1];
        }
 		interpolate(&values[0], &values2[0], values.size(),
			interpolationFactor(axisIndex, coordinate, v));

		reset(&axes[0], _n - 1, &values[0]);
    }
}

template<class Wp>
std::istream&
AerLookupTable<Wp>::read(std::istream& is)
{
	using std::valarray;

	if (is) {
		size_t n;
		read(is, &n, 1);

		valarray<size_t> sizes(n);
		valarray<Axis> axes(n);

		for (size_t i = 0; i < n; ++i) {
			read(is, &sizes[i], 1);
			axes[i].resize(sizes[i]);
			read(is, &axes[i][0], sizes[i]);
		}

		valarray<Wp> values(numValues(&sizes[0], n));
		read(is, &values[0], values.size());

		if (is) {
			reset(&axes[0], n, &values[0]);
		}
	}

	return is;
}

template<class Wp>
ostream&
AerLookupTable<Wp>::write(ostream& os) const
{
	if (os) {
		write(os, &_n, 1);
		for (size_t i = 0; i < _n; ++i) {
			write(os, &_sizes[i], 1);
			write(os, &_x[i][0], _sizes[i]);
		}
		write(os, &_y[0], _y.size());
	}

	return os;
}

template<class Wp>
ostream&
AerLookupTable<Wp>::writeValues(ostream& os) const
{
	if (os) {
		write(os, &_y[0], _y.size());
	}

	return os;
}

template<class Wp>
inline
Wp
AerLookupTable<Wp>::interpolationFactor(size_t axisIndex,
	Wp coordinate, size_t vertex) const
{
	return (coordinate - _x[axisIndex][vertex]) / (_x[axisIndex][vertex + 1] -
		_x[axisIndex][vertex]);
}

template<class Wp>
size_t
AerLookupTable<Wp>::numValues(const size_t sizes[], size_t numAxes)
{
	size_t numValues = 1;

	for (size_t i = 0; i < numAxes; ++i) {
		assert(sizes[i] > 0);
		numValues *= sizes[i];
	}

	return numValues;
}

template<class Wp>
size_t
AerLookupTable<Wp>::numValues(const Axis axes[], size_t numAxes)
{
	size_t numValues = 1;

	for (size_t i = 0; i < numAxes; ++i) {
		assert(axes[i].size() > 0);
		numValues *= axes[i].size();
	}

	return numValues;
}

template<class Wp>
size_t
AerLookupTable<Wp>::numValues(const Axis* axes[], size_t numAxes)
{
	size_t numValues = 1;

	for (size_t i = 0; i < numAxes; ++i) {
		assert(axes[i]->size() > 0);
		numValues *= axes[i]->size();
	}

	return numValues;
}

template<class Wp>
size_t
AerLookupTable<Wp>::valueIndex(const size_t vertexes[]) const
{
	size_t index = 0;

	for (size_t i = 0; i < _n; ++i) {
		assert(vertexes[i] < _sizes[i]);

		index += vertexes[i] * _strides[i];
	}

	return index;
}

template<class Wp>
size_t
AerLookupTable<Wp>::vertex(size_t axisIndex, Wp coordinate) const
{
    assert(axisIndex < _n and _sizes[axisIndex] > 0);

    if (!isValidCoordinate(axisIndex, coordinate)) {
    	cout << "Table dimension  = " << _n << endl;
    	cout << "Axis index       = " << axisIndex << endl;
    	cout << "Coordinate value = " << coordinate << endl;
    	cout << "Minimum value    = " << minCoordinate(axisIndex) << endl;
    	cout << "Maximum value    = " << maxCoordinate(axisIndex) << endl;
    }
    assert(coordinate >= _x[axisIndex][0] and coordinate <= _x[axisIndex][_sizes[axisIndex] - 1]);

    size_t i = 0;
    size_t k = _sizes[axisIndex] - 1;

    while (k > i + 1) {
        const size_t j = (i + k) >> 1;

        if (coordinate > _x[axisIndex][j])
            i = j;
        else
            k = j;
    }

    return i;
}

template<class Wp>
void
AerLookupTable<Wp>::getVertexes(const Wp coordinates[], size_t vertexes[]) const
{
    for (size_t i = 0; i < _n; ++i)
        vertexes[i] = vertex(i, coordinates[i]);
}

template<class Wp>
void
AerLookupTable<Wp>::interpolate(Wp values[], const Wp values2[], size_t numValues,
    	Wp interpolationFactor) const
{
	for (size_t i = 0; i < numValues; ++i)
		values[i] = (Wp(1) - interpolationFactor) * values[i] +
			interpolationFactor * values2[i];
}

template<class Wp>
void
AerLookupTable<Wp>::updateSizesAndStrides()
{
	_sizes.resize(_x.size());
	_strides.resize(_x.size());

	for (size_t i = 0; i < _x.size(); ++i)
		_sizes[i] = _x[i].size();
    for (size_t i = _x.size(), stride = 1; i-- > 0; ) {
        _strides[i] = stride;
        stride *= _sizes[i];
    }

	_n = _x.size();
}

template<class Wp>
void
AerLookupTable<Wp>::reset(const Axis axes[], size_t numAxes)
{
	const size_t numValues = this->numValues(axes, numAxes);

	_x.resize(numAxes);
	_y.resize(numValues);

	for (size_t i = 0; i < numAxes; ++i) {
		const Axis& axis = axes[i];

		_x[i].resize(axis.size());
		copy(&axis[0], &axis[axis.size()], &_x[i][0]);
	}

    updateSizesAndStrides();

 	valarray<size_t> numbers(numValues);
	for (size_t i = 0; i < numValues; ++i)
		numbers[i] = i;
	_indexes.resize(1 << _n);
	_indexes = numbers[gslice(0, valarray<size_t>(2, _n), _strides)];
}

template<class Wp>
void
AerLookupTable<Wp>::reset(const Axis axes[], size_t numAxes, const Wp values[])
{
	const size_t numValues = this->numValues(axes, numAxes);

	_x.resize(numAxes);
	_y.resize(numValues);

	for (size_t i = 0; i < numAxes; ++i) {
		const Axis& axis = axes[i];

		_x[i].resize(axis.size());
		copy(&axis[0], &axis[axis.size()], &_x[i][0]);
	}
 	copy(&values[0], &values[numValues], &_y[0]);

    updateSizesAndStrides();

 	valarray<size_t> numbers(numValues);
	for (size_t i = 0; i < numValues; ++i)
		numbers[i] = i;
	_indexes.resize(1 << _n);
	_indexes = numbers[gslice(0, valarray<size_t>(2, _n), _strides)];
}

template<class Wp>
void
AerLookupTable<Wp>::reset(const Axis* axes[], size_t numAxes, const Wp values[])
{
	const size_t numValues = this->numValues(axes, numAxes);

	_x.resize(numAxes);
	_y.resize(numValues);

	for (size_t i = 0; i < numAxes; ++i) {
		const Axis& axis = *axes[i];

		_x[i].resize(axis.size());
		copy(&axis[0], &axis[axis.size()], &_x[i][0]);
	}
 	copy(&values[0], &values[numValues], &_y[0]);

    updateSizesAndStrides();

 	valarray<size_t> numbers(numValues);
	for (size_t i = 0; i < numValues; ++i)
		numbers[i] = i;
	_indexes.resize(1 << _n);
	_indexes = numbers[gslice(0, valarray<size_t>(2, _n), _strides)];
}

template<class Wp>
bool
AerLookupTable<Wp>::isBigEndian()
{
	const unsigned long test = 1;

	return *reinterpret_cast<const unsigned char*>(&test) == 0;
}

template<class Wp>
template<class T>
istream&
AerLookupTable<Wp>::read(istream& is, T* first, size_t n)
{
	if (isBigEndian()) {
		is.read(reinterpret_cast<char*>(first), sizeof(T) * n);
	} else {
		char bytes[sizeof(T)];

		for (size_t i = 0; i < n; ++i, ++first) {
			is.read(bytes, sizeof(T));
			reverse(bytes, bytes + sizeof(T));
			*first = *reinterpret_cast<T*>(bytes);
		}
	}

	return is;
}

template<class Wp>
template<class T>
ostream&
AerLookupTable<Wp>::write(ostream& os, const T* first, size_t n)
{
	if (isBigEndian()) {
		os.write(reinterpret_cast<const char*>(first), sizeof(T) * n);
	} else {
		char bytes[sizeof(T)];

		for (size_t i = 0; i < n; ++i, ++first) {
			reverse_copy(reinterpret_cast<const char*>(first),
				reinterpret_cast<const char*>(first) + sizeof(T), bytes);
			os.write(bytes, sizeof(T));
		}
	}

	return os;
}

#endif // BASIC_LOOKUP_TABLE_INC
*/
