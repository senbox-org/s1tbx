/*
 * $Id: UIDefaults.java,v 1.1 2006/10/10 14:47:39 norman Exp $
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

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;

/**
 * A utility class which creates GUI componants with default settings.
 *
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public class UIDefaults {

    public static final Color HEADER_COLOR = new Color(82, 109, 165);
    public static final int INSET_SIZE = 6;
    public static final EmptyBorder DIALOG_BORDER = new EmptyBorder(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE);
    public static final Border SLIDER_BOX_BORDER = BorderFactory.createEtchedBorder(new Color(166, 202, 240),
                                                                                    new Color(91, 135, 206));

    private static final Font _verySmallSansSerifFont = new Font("SansSerif", Font.PLAIN, 9);
    private static final Font _smallSansSerifFont = new Font("SansSerif", Font.PLAIN, 11);


    public static EmptyBorder getDialogBorder() {
        return DIALOG_BORDER;
    }

    public static Font getVerySmallSansSerifFont() {
        return _verySmallSansSerifFont;
    }

    public static Font getSmallSansSerifFont() {
        return _smallSansSerifFont;
    }

}
