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
package org.esa.beam.framework.ui;

import org.esa.beam.util.Guardian;

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * A utility class providing helper methods for <code>JPanel</code>s with a <code>GridBagLayout</code> layout manager.
 *
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public class GridBagUtils {

    public static JPanel createPanel() {
        return new JPanel(new GridBagLayout());
    }

    public static JPanel createDefaultEmptyBorderPanel() {
        JPanel panel = createPanel();
        panel.setBorder(UIDefaults.getDialogBorder());
        return panel;
    }

    /**
     * Creates a <code>GridBagConstraints</code> instance with the attributes given as a comma separated key-value pairs
     * in a text string.
     * <p/>
     * <p>According to the public <code>GridBagConstraints</code> attributes, the following key-value pairs are can
     * occur in the text string:
     * <p/>
     * <ld> <li><code>{@link GridBagConstraints#gridx gridx}=<b>RELATIVE</b>|<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#gridy gridy}=<b>RELATIVE</b>|<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#gridwidth gridwidth}=<b>REMAINDER</b>|<b>RELATIVE</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#gridheight gridheight}=<b>REMAINDER</b>|<b>RELATIVE</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#weightx weightx}=<i>double</i></code></li> <li><code>{@link
     * GridBagConstraints#weighty weighty}=<i>double</i></code></li> <li><code>{@link GridBagConstraints#anchor
     * anchor}=<b>CENTER</b>|<b>NORTH</b>|<b>NORTHEAST</b>|<b>EAST</b>|<b>SOUTHEAST</b>|<b>SOUTH</b>|<b>SOUTHWEST</b>|<b>WEST</b>|<b>NORTHWEST</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#fill fill}=<b>NONE</b>|<b>HORIZONTAL</b>|<b>VERTICAL</b>|<b>BOTH</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#insets insets.bottom}=<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#insets insets.left}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#insets
     * insets.right}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#insets
     * insets.top}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#ipadx
     * ipadx}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#ipady ipady}=<i>integer</i></code></li>
     * </ld>
     *
     * @param code a textual representation of the attributes to be set
     */
    public static GridBagConstraints createConstraints(String code) {
        GridBagConstraints gbc = new GridBagConstraints();
        setAttributes(gbc, code);
        return gbc;
    }

    /**
     * Creates a <code>GridBagConstraints</code> instance with the following attributes: <ld> <li><code>{@link
     * GridBagConstraints#anchor anchor}=<b>WEST</b></li> <li><code>{@link GridBagConstraints#insets
     * insets.top}=<b>0</b></code></li> <li><code>{@link GridBagConstraints#insets insets.left}=<b>3</b></code></li>
     * <li><code>{@link GridBagConstraints#insets insets.bottom}=<b>0</b></code></li> <li><code>{@link
     * GridBagConstraints#insets insets.right}=<b>3</b></code></li> </ld>
     */
    public static GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 3, 0, 3);
        return gbc;
    }

    /**
     * Adds a component to a panel with a grid bag layout.
     *
     * @param panel the panel to which to add the component
     * @param comp  the component to be added
     * @param gbc   the grid bag constraints to be used, can be <code>null</code> if <code>code</code> is not
     *              <code>null</code>
     */
    public static void addToPanel(JPanel panel, Component comp, GridBagConstraints gbc) {
        LayoutManager layoutManager = panel.getLayout();
        if (!(layoutManager instanceof GridBagLayout)) {
            throw new IllegalArgumentException("'panel' does not have a GridBagLayout manager");
        }
        GridBagLayout gbl = (GridBagLayout) layoutManager;
        gbl.setConstraints(comp, gbc);
        panel.add(comp);
    }

    /**
     * Adds a component to a panel with a grid bag layout. <p>For the <code>code</code> parameter, see also {@link
     * #setAttributes(GridBagConstraints, String)}.
     *
     * @param panel the panel to which to add the component
     * @param comp  the component to be added
     * @param gbc   the grid bag constraints to be used, can be <code>null</code> if <code>code</code> is not
     *              <code>null</code>
     * @param code  the code containing the constraints, can be <code>null</code> if <code>gbc</code> is not
     *              <code>null</code>
     * @see #setAttributes(GridBagConstraints, String)
     */
    public static void addToPanel(JPanel panel, Component comp, GridBagConstraints gbc, String code) {
        addToPanel(panel, comp, setAttributes(gbc, code));
    }


    public static void addHorizontalFiller(JPanel panel, GridBagConstraints gbc) {
        int fillOld = gbc.fill;
        int anchorOld = gbc.anchor;
        double weightxOld = gbc.weightx;
        if (gbc.gridx >= 0) {
            gbc.gridx++;
        }
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        addToPanel(panel, new JLabel(" "), gbc);
        gbc.weightx = weightxOld;
        gbc.anchor = anchorOld;
        gbc.fill = fillOld;
    }

    public static void addVerticalFiller(JPanel panel, GridBagConstraints gbc) {
        int fillOld = gbc.fill;
        int anchorOld = gbc.anchor;
        double weightyOld = gbc.weighty;
        if (gbc.gridy >= 0) {
            gbc.gridy++;
        }
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 200.0;
        addToPanel(panel, new JLabel(" "), gbc);
        gbc.weighty = weightyOld;
        gbc.anchor = anchorOld;
        gbc.fill = fillOld;
    }

    /**
     * Sets the attributes of a given  <code>GridBagConstraints</code> instance to the attribute values given as a comma
     * separated key-value pairs in a text string.
     * <p/>
     * <p>According to the public <code>GridBagConstraints</code> attributes, the following key-value pairs are can
     * occur in the text string:
     * <p/>
     * <ld> <li><code>{@link GridBagConstraints#gridx gridx}=<b>RELATIVE</b>|<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#gridy gridy}=<b>RELATIVE</b>|<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#gridwidth gridwidth}=<b>REMAINDER</b>|<b>RELATIVE</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#gridheight gridheight}=<b>REMAINDER</b>|<b>RELATIVE</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#weightx weightx}=<i>double</i></code></li> <li><code>{@link
     * GridBagConstraints#weighty weighty}=<i>double</i></code></li> <li><code>{@link GridBagConstraints#anchor
     * anchor}=<b>CENTER</b>|<b>NORTH</b>|<b>NORTHEAST</b>|<b>EAST</b>|<b>SOUTHEAST</b>|<b>SOUTH</b>|<b>SOUTHWEST</b>|<b>WEST</b>|<b>NORTHWEST</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#fill fill}=<b>NONE</b>|<b>HORIZONTAL</b>|<b>VERTICAL</b>|<b>BOTH</b>|<i>integer</i></code></li>
     * <li><code>{@link GridBagConstraints#insets insets.bottom}=<i>integer</i></code></li> <li><code>{@link
     * GridBagConstraints#insets insets.left}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#insets
     * insets.right}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#insets
     * insets.top}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#ipadx
     * ipadx}=<i>integer</i></code></li> <li><code>{@link GridBagConstraints#ipady ipady}=<i>integer</i></code></li>
     * </ld>
     *
     * @param gbc  the grid bag constraints whose attributes are to be set, must not be null
     * @param code a textual representation of the attributes to be set
     */
    public static GridBagConstraints setAttributes(GridBagConstraints gbc, String code) {
        Guardian.assertNotNull("gbc", gbc);
        if (code == null || code.trim().length() == 0) {
            return gbc;
        }
        StringTokenizer st = new StringTokenizer(code, ",", false);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int pos = token.indexOf('=');
            if (pos < 0) {
                throw new IllegalArgumentException("illegal token '" + token + "'");
            }
            String key = token.substring(0, pos).trim();
            String value = token.substring(pos + 1).trim();
            try {
                if (key.equals("gridx")) {
                    if (value.equals("RELATIVE")) {
                        gbc.gridx = GridBagConstraints.RELATIVE;
                    } else {
                        gbc.gridx = Integer.parseInt(value);
                    }
                } else if (key.equals("gridy")) {
                    if (value.equals("RELATIVE")) {
                        gbc.gridy = GridBagConstraints.RELATIVE;
                    } else {
                        gbc.gridy = Integer.parseInt(value);
                    }
                } else if (key.equals("gridwidth")) {
                    if (value.equals("RELATIVE")) {
                        gbc.gridwidth = GridBagConstraints.RELATIVE;
                    } else if (value.equals("REMAINDER")) {
                        gbc.gridwidth = GridBagConstraints.REMAINDER;
                    } else {
                        gbc.gridwidth = Integer.parseInt(value);
                    }
                } else if (key.equals("gridheight")) {
                    if (value.equals("RELATIVE")) {
                        gbc.gridheight = GridBagConstraints.RELATIVE;
                    } else if (value.equals("REMAINDER")) {
                        gbc.gridheight = GridBagConstraints.REMAINDER;
                    } else {
                        gbc.gridheight = Integer.parseInt(value);
                    }
                } else if (key.equals("weightx")) {
                    gbc.weightx = Double.parseDouble(value);
                } else if (key.equals("weighty")) {
                    gbc.weighty = Double.parseDouble(value);
                } else if (key.equals("anchor")) {
                    if (value.equals("CENTER")) {
                        gbc.anchor = GridBagConstraints.CENTER;
                    } else if (value.equals("NORTH")) {
                        gbc.anchor = GridBagConstraints.NORTH;
                    } else if (value.equals("NORTHEAST")) {
                        gbc.anchor = GridBagConstraints.NORTHEAST;
                    } else if (value.equals("EAST")) {
                        gbc.anchor = GridBagConstraints.EAST;
                    } else if (value.equals("SOUTHEAST")) {
                        gbc.anchor = GridBagConstraints.SOUTHEAST;
                    } else if (value.equals("SOUTH")) {
                        gbc.anchor = GridBagConstraints.SOUTH;
                    } else if (value.equals("SOUTHWEST")) {
                        gbc.anchor = GridBagConstraints.SOUTHWEST;
                    } else if (value.equals("WEST")) {
                        gbc.anchor = GridBagConstraints.WEST;
                    } else if (value.equals("NORTHWEST")) {
                        gbc.anchor = GridBagConstraints.NORTHWEST;
                    } else {
                        gbc.anchor = Integer.parseInt(value);
                    }
                } else if (key.equals("fill")) {
                    if (value.equals("NONE")) {
                        gbc.fill = GridBagConstraints.NONE;
                    } else if (value.equals("HORIZONTAL")) {
                        gbc.fill = GridBagConstraints.HORIZONTAL;
                    } else if (value.equals("VERTICAL")) {
                        gbc.fill = GridBagConstraints.VERTICAL;
                    } else if (value.equals("BOTH")) {
                        gbc.fill = GridBagConstraints.BOTH;
                    } else {
                        gbc.fill = Integer.parseInt(value);
                    }
                } else if (key.equals("insets.bottom")) {
                    gbc.insets.bottom = Integer.parseInt(value);
                } else if (key.equals("insets.left")) {
                    gbc.insets.left = Integer.parseInt(value);
                } else if (key.equals("insets.right")) {
                    gbc.insets.right = Integer.parseInt(value);
                } else if (key.equals("insets.top")) {
                    gbc.insets.top = Integer.parseInt(value);
                } else if (key.equals("insets")) {
                    gbc.insets.top = gbc.insets.left = gbc.insets.right = gbc.insets.bottom = Integer.parseInt(value);
                } else if (key.equals("ipadx")) {
                    gbc.ipadx = Integer.parseInt(value);
                } else if (key.equals("ipady")) {
                    gbc.ipady = Integer.parseInt(value);
                } else {
                    throw new IllegalArgumentException("unknown key '" + key + "'");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("illegal value '" + value + "' for key '" + key + "'");
            }
        }
        return gbc;
    }
}
