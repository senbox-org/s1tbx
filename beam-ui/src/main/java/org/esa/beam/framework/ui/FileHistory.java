/*
 * $Id: FileHistory.java,v 1.2 2007/03/28 14:27:50 norman Exp $
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
package org.esa.beam.framework.ui;

import java.io.File;

/**
 * <code>FileHistory</code> is a fixed-size array for the pathes of files opened/saved by a user. If a new file is added
 * and the file history is full, the list of registered files is shifted so that the oldest file path is beeing
 * skipped..
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class FileHistory extends UserInputHistory {

    public FileHistory(int maxNumEntries, String propertyKey) {
        super(maxNumEntries, propertyKey);
    }

    @Override
    protected boolean isValidItem(String item) {
        if (item != null && item.length() > 0) {
            return new File(item).exists();
        }
        return false;
    }
}
