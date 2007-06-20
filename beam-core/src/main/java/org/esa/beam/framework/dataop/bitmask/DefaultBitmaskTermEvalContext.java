/*
 * $Id: DefaultBitmaskTermEvalContext.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import org.esa.beam.util.Guardian;

/**
 * Defines a context in which bit-mask terms can be evaluated. This class serves as default implementation. Further specialized classes may be derived from this one.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */

public class DefaultBitmaskTermEvalContext implements BitmaskTermEvalContext {

    private DefaultFlagDataset[] _flagDatasets;

    public DefaultBitmaskTermEvalContext(DefaultFlagDataset flagDataset) {
        this(new DefaultFlagDataset[]{flagDataset});
    }

    public DefaultBitmaskTermEvalContext(DefaultFlagDataset[] flagDatasets) {
        Guardian.assertNotNull("flagDatasets", flagDatasets);
        _flagDatasets = flagDatasets;
    }

    /**
     * Returns the dataset for the given dataset name. The method performs a case-insensitive search on the given name.
     *
     * @param datasetName the name of the flag dataset to be resolved
     * @return the flag dataset associated with the given name
     */
    public FlagDataset getFlagDataset(String datasetName) {
        for (int i = 0; i < _flagDatasets.length; i++) {
            DefaultFlagDataset flagDataset = _flagDatasets[i];
            if (flagDataset.getDatasetName().equalsIgnoreCase(datasetName)) {
                return flagDataset;
            }
        }
        return null;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        if (_flagDatasets != null) {
            for (int i = 0; i < _flagDatasets.length; i++) {
                _flagDatasets[i] = null;
            }
            _flagDatasets = null;
        }
    }
}
