/*
 * $Id: ItemInfo.java,v 1.1 2006/09/18 06:34:32 marcop Exp $
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
package org.esa.beam.dataio.envisat;

import org.esa.beam.util.Guardian;


/**
 * The <code>FieldInfo</code> class contains the information about the structure of a particular record field.
 * <p/>
 * <p> A <code>RecordInfo</code> instance contains a list of <code>FieldInfo</code> instances describing each field of
 * the record.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.Field
 * @see org.esa.beam.dataio.envisat.Record
 * @see org.esa.beam.dataio.envisat.RecordInfo
 */
public abstract class ItemInfo {

    /**
     * This items's name.
     */
    private String _name;

    /**
     * This items's description (optional, can be null).
     */
    private final String _description;

    /**
     * Constructs a new item-info from the supplied parameters.
     *
     * @param name        the items name, must not be null or empty
     * @param description this items's description (optional, can be null)
     *
     * @see org.esa.beam.framework.datamodel.ProductData
     */
    protected ItemInfo(String name, String description) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        _name = name;

        if (description != null) {
            // we may have lots of the same description strings around - let them be shared
            // from the String class pool
            _description = description.intern();
        } else {
            _description = null;
        }
    }

    /**
     * Gets this item's name.
     */
    public final String getName() {
        return _name;
    }

    /**
     * Gets the item's description string, which can be null.
     */
    public final String getDescription() {
        return _description;
    }

    /**
     * Sets a name prefix in the form of new_name = prefix.old_name for this field.
     *
     * @param prefix the prefix to be added
     */
    public void setNamePrefix(String prefix) {
        Guardian.assertNotNull("prefix", prefix);
        _name = prefix + "." + _name;
    }

    /**
     * Tests if the given name equals the name of this item. The method performs a case insensitive comparison.
     *
     * @param name the name to test
     *
     * @return <code>true</code> if the names are equal, <code>false</code> otherwise
     */
    public boolean isNameEqualTo(String name) {
        return getName().equalsIgnoreCase(name);
    }

    /**
     * Returns a string representation of this field-info which can be used for debugging purposes.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ItemInfo[");
        sb.append("'");
        sb.append(getName());
        sb.append("','");
        sb.append(getDescription());
        sb.append("']");
        return sb.toString();
    }
}
