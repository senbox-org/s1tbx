/*
 * $Id: FormatedFileHistory.java,v 1.1 2006/10/10 14:47:38 norman Exp $
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

import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.Guardian;

/**
 * <code>FileHistory</code> is a fixed-size array for the pathes of files opened/saved by a user. If a new file is added
 * and the file history is full, the list of registered files is shifted so that the oldest file path is beeing
 * skipped..
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class FormatedFileHistory extends FileHistory {

    private final ProductReaderPlugIn _productReaderPlugIn;

    public FormatedFileHistory(FileHistory history, ProductReaderPlugIn readerPlugIn) {
        this(history.getMaxNumEntries(), history.getPropertyKey(), readerPlugIn);
    }

    public FormatedFileHistory(int maxNumEntries, String propertyKey, ProductReaderPlugIn readerPlugIn) {
        super(maxNumEntries, propertyKey);
        Guardian.assertNotNull("readerPlugIn", readerPlugIn);
        _productReaderPlugIn = readerPlugIn;
    }

    @Override
    protected boolean isValidItem(String item) {
        if (super.isValidItem(item)) {
            final File file = new File(item);
            return _productReaderPlugIn.getDecodeQualification(file) != DecodeQualification.UNABLE;
        }
        return false;
    }
}
