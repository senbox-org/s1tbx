/*
 * $Id: DefaultFlagDataset.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.framework.dataop.bitmask;

import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

/**
 * An implementation of the <code>FlagDataset</code> interface. This class is abstract, but it defines several nested
 * classes which provide full implementations for datasets of specific sample types.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class DefaultFlagDataset implements FlagDataset {

    /**
     * The flag coding.
     */
    private FlagCoding _flagCoding;
    /**
     * The flag samples.
     */
    private ProductData _flagSamples;

    /**
     * Creates a flag dataset for the given flag names, values and the actual dataset data stored in an array of
     * <code>byte</code> samples.
     *
     * @param datasetName the dataset name, must not be <code>null</code>
     * @param flagNames   the array of flag names, must not be <code>null</code> and must have the same size as the
     *                    <code>flagMasks</code> array
     * @param flagMasks   the array of flag mask values, must not be <code>null</code>
     * @param samples     the actual dataset data as an array of <code>byte</code> samples
     *
     * @return a new flag dataset
     */
    public static DefaultFlagDataset create(String datasetName, String[] flagNames, int[] flagMasks, byte[] samples) {
        ProductData flagSamples = ProductData.createInstance(ProductData.TYPE_INT8, samples.length);
        flagSamples.setElems(samples);
        return create(datasetName, flagNames, flagMasks, flagSamples);
    }

    /**
     * Creates a flag dataset for the given flag names, values and the actual dataset data stored in an array of
     * <code>short</code> samples.
     *
     * @param flagNames the array of flag names, must not be <code>null</code> and must have the same size as the
     *                  <code>flagMasks</code> array
     * @param flagMasks the array of flag mask values, must not be <code>null</code>
     * @param samples   the actual dataset data as an array of <code>short</code> samples
     *
     * @return a new flag dataset
     */
    public static DefaultFlagDataset create(String datasetName, String[] flagNames, int[] flagMasks, short[] samples) {
        ProductData flagSamples = ProductData.createInstance(ProductData.TYPE_INT16, samples.length);
        flagSamples.setElems(samples);
        return create(datasetName, flagNames, flagMasks, flagSamples);
    }

    /**
     * Creates a flag dataset for the given flag names, values and the actual dataset data stored in an array of
     * <code>int</code> samples.
     *
     * @param datasetName the dataset name, must not be <code>null</code>
     * @param flagNames   the array of flag names, must not be <code>null</code> and must have the same size as the
     *                    <code>flagMasks</code> array
     * @param flagMasks   the array of flag mask values, must not be <code>null</code>
     * @param samples     the actual dataset data as an array of <code>int</code> samples
     *
     * @return a new flag dataset
     */
    public static DefaultFlagDataset create(String datasetName, String[] flagNames, int[] flagMasks, int[] samples) {
        ProductData flagSamples = ProductData.createInstance(ProductData.TYPE_INT32, samples.length);
        flagSamples.setElems(samples);
        return create(datasetName, flagNames, flagMasks, flagSamples);
    }

    /**
     * Creates a flag dataset for the given flag names, values and the actual dataset data stored in a
     * <code>org.esa.beam.framework.datamodel.ProductData</code> instance.
     *
     * @param datasetName the dataset name, must not be <code>null</code>
     * @param flagNames   the array of flag names, must not be <code>null</code> and must have the same size as the
     *                    <code>flagMasks</code> array
     * @param flagMasks   the array of flag mask values, must not be <code>null</code>
     * @param flagSamples the actual flag data stored in a <code>org.esa.beam.framework.datamodel.ProductData</code>
     *                    instance, must not be <code>null</code> and must have an integer element type
     *
     * @return a new flag dataset
     *
     * @see org.esa.beam.framework.datamodel.ProductData
     */
    public static DefaultFlagDataset create(String datasetName, String[] flagNames, int[] flagMasks,
                                            ProductData flagSamples) {
        Guardian.assertNotNull("datasetName", datasetName);
        Guardian.assertNotNull("flagNames", flagNames);
        Guardian.assertNotNull("flagMasks", flagMasks);
        FlagCoding flagCoding = new FlagCoding(datasetName);
        for (int i = 0; i < flagNames.length; i++) {
            flagCoding.setAttributeInt(flagNames[i], flagMasks[i]);
        }
        return create(flagCoding, flagSamples);
    }

    /**
     * Creates a flag dataset for the given flag coding and the actual dataset data stored in a
     * <code>org.esa.beam.framework.datamodel.ProductData</code> instance.
     *
     * @param flagCoding  the flag coding
     * @param flagSamples the actual flag data stored in a <code>org.esa.beam.framework.datamodel.ProductData</code>
     *                    instance, must not be <code>null</code> and must have an integer element type
     *
     * @return a new flag dataset
     *
     * @see org.esa.beam.framework.datamodel.FlagCoding
     * @see org.esa.beam.framework.datamodel.ProductData
     */
    public static DefaultFlagDataset create(FlagCoding flagCoding, ProductData flagSamples) {
        Guardian.assertNotNull("flagCoding", flagCoding);
        Guardian.assertNotNull("flagSamples", flagSamples);
        if (!flagSamples.isInt()) {
            throw new IllegalArgumentException("'flagSamples' does not contain integer elements");
        }
        if (flagCoding.getNumAttributes() == 0) {
            throw new IllegalArgumentException("'flagCoding' does not contain any flags");
        }
        return new DefaultFlagDataset(flagCoding, flagSamples);
    }

    /**
     * Returns this dataset's name.
     */
    public String getDatasetName() {
        return _flagCoding.getName();
    }

    /**
     * Returns the names of all flags in this dataset.
     *
     * @return the array of flag names
     */
    public String[] getFlagNames() {
        return _flagCoding.getAttributeNames();
    }

    /**
     * Gets the flag mask value for the given flag name. The method performs an case-insensitive search on the given
     * name.
     *
     * @param flagName the flag's name
     *
     * @return the flag mask value
     */
    public int getFlagMask(String flagName) {
        return _flagCoding.getAttributeInt(flagName, 0);
    }

    /**
     * Gets the number of samples in this dataset.
     *
     * @return the number of samples in this dataset
     */
    public int getNumSamples() {
        return _flagSamples.getNumElems();
    }

    /**
     * Gets the sample at the specified zero-based index.
     *
     * @param index the zero-based sample index
     *
     * @return the sample at the specified zero-based index
     */
    public int getSampleAt(int index) {
        return _flagSamples.getElemIntAt(index);
    }

    /**
     * Constructs a flag dataset for the given flag coding and the actual dataset data stored in a
     * <code>org.esa.beam.framework.datamodel.ProductData</code> instance.
     *
     * @param flagCoding  the flag coding
     * @param flagSamples the actual flag data stored in a <code>org.esa.beam.framework.datamodel.ProductData</code>
     *                    instance, must not be <code>null</code> and must have an integer element type
     */
    protected DefaultFlagDataset(FlagCoding flagCoding, ProductData flagSamples) {
        Debug.assertNotNull(flagCoding);
        Debug.assertTrue(flagCoding.getNumAttributes() > 0);
        Debug.assertNotNull(flagSamples);
        Debug.assertTrue(flagSamples.getNumElems() > 0);
        _flagCoding = flagCoding;
        _flagSamples = flagSamples;
    }
}
