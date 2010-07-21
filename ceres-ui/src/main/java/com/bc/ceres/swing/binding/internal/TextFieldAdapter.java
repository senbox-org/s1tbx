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

package com.bc.ceres.swing.binding.internal;

import javax.swing.JTextField;
import java.awt.event.ActionListener;

/**
 * A binding for a {@link javax.swing.JTextField} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.6
 * @deprecated since Ceres 0.10, use {@link TextComponentAdapter} directly
 */
@Deprecated
public class TextFieldAdapter extends TextComponentAdapter implements ActionListener {

    public TextFieldAdapter(JTextField textField) {
        super(textField);
    }
}
