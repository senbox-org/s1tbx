/*
 * $Id: FlagDataset.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

/**
 * A <code>FlagDataset</code> is a dataset providing flag values as unsigned integer samples. A single flag dataset can
 * therefore have a maximum of 32 flag values per sample.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public interface FlagDataset {

    /**
     * Returns this dataset's name.
     */
    String getDatasetName();

    /**
     * Gets the flag-mask value for the given flag name. The method performs an case-insensitive search on the given
     * name.
     *
     * @param flagName the flag name
     *
     * @return the flag value
     */
    int getFlagMask(String flagName);

    /**
     * Gets the number of samples in this dataset.
     *
     * @return the number of samples in this dataset
     */
    int getNumSamples();

    /**
     * Gets the sample at the specified zero-based index.
     *
     * @param index the zero-based sample index
     *
     * @return the sample at the specified zero-based index
     */
    int getSampleAt(int index);
}
