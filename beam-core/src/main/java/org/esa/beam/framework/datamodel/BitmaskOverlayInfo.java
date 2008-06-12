/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Guardian;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>BitmaskOverlayInfo</code> class manages a list of <code>{@link BitmaskDef}</code>s.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class BitmaskOverlayInfo implements Cloneable {

    private List<BitmaskDef> _bitmaskDefs;

    public BitmaskOverlayInfo() {
    }

    /**
     * Adds a bitmask definition reference to this bitmask overlay info.
     */
    public void addBitmaskDef(BitmaskDef bitmaskDef) {
        if (bitmaskDef != null) {
            if (_bitmaskDefs == null) {
                _bitmaskDefs = new ArrayList<BitmaskDef>(3);
            }
            _bitmaskDefs.add(bitmaskDef);
        }
    }

    /**
     * Removes a bitmask definiton reference from this bitmask overlay info.
     */
    public void removeBitmaskDef(BitmaskDef bitmaskDef) {
        Guardian.assertNotNull("bitmaskDef", bitmaskDef);
        if (_bitmaskDefs != null) {
            _bitmaskDefs.remove(bitmaskDef);
        }
    }

    /**
     * Gets an array of bitmask definition references, never <code>null</code>.
     */
    public BitmaskDef[] getBitmaskDefs() {
        if (_bitmaskDefs == null) {
            return new BitmaskDef[0];
        }
        return _bitmaskDefs.toArray(new BitmaskDef[_bitmaskDefs.size()]);
    }

    /**
     * Checks whether or not the given bitmask definition is contained in this bitmask overlay info.
     *
     * @param bitmaskDef the bitmask definition
     *
     * @return <code>true</code> if the given bitmask def is contained in this bitmask overlay info, otherwise
     *         <code>false</code>
     */
    public boolean containsBitmaskDef(BitmaskDef bitmaskDef) {
        return _bitmaskDefs != null && _bitmaskDefs.contains(bitmaskDef);
    }

    /**
     * Checks whether or not the bitmask with the given name  is contained in this bitmask overlay info.
     *
     * @param bitmaskName the bitmask name
     *
     * @return <code>true</code> if a bitmask with the given name  is contained in this bitmask overlay info, otherwise
     *         <code>false</code>
     */
    public boolean containsBitmaskDef(String bitmaskName) {
        if (_bitmaskDefs != null) {
            BitmaskDef[] bitmaskDefs = getBitmaskDefs();
            for (BitmaskDef bitmaskDef : bitmaskDefs) {
                if (bitmaskDef.getName().equalsIgnoreCase(bitmaskName)) {
                    return true;
                }
            }
        }
        return false;
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
        if (_bitmaskDefs != null) {
            _bitmaskDefs = null;
        }
    }
}
