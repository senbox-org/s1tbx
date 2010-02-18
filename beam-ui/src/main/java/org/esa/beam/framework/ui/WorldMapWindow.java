/*
 * $Id: WorldMapWindow.java,v 1.3 2007/04/18 13:01:13 norman Exp $
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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;

import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Frame;

/**
 * The window displaying the world map.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapWindow extends JDialog {

    private static final String DEFAUL_TTITLE = "World Map";

    private String helpId;
    private WorldMapPaneDataModel worldMapDataModel;

    /**
     * @param owner  the owner of this window
     * @param title  the window title
     * @param helpId the helpId
     */
    public WorldMapWindow(Frame owner, String title, String helpId) {
        super(owner, title != null && title.length() > 0 ? title : DEFAUL_TTITLE, false);
        this.helpId = helpId;
        createUI();
        if (this.helpId != null) {
            HelpSys.enableHelpKey(this.getContentPane(), this.helpId);
        }
    }

    /**
     * @param owner     the owner of this window
     * @param title     the window title
     * @param helpId    the helpId
     * @param accessory is ignored
     *
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    @SuppressWarnings({"UnusedDeclaration"})
    public WorldMapWindow(Frame owner, String title, String helpId, JComponent accessory) {
        this(owner, title, helpId);
    }

    public void setSelectedProduct(Product product) {
        worldMapDataModel.setSelectedProduct(product);
    }

    public Product getSelectedProduct() {
        return worldMapDataModel.getSelectedProduct();
    }

    public void setProducts(Product[] products) {
        worldMapDataModel.setProducts(products);
    }

    public void setPathesToDisplay(GeoPos[][] geoBoundaries) {
        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    private void createUI() {
        worldMapDataModel = new WorldMapPaneDataModel();

        final WorldMapPane mapPane = new WorldMapPane(worldMapDataModel);
        setContentPane(mapPane);

        if (helpId != null) {
            HelpSys.enableHelpKey(this, helpId);
        }
    }

    /**
     * @deprecated since BEAM 4.7, call {@link #pack()} instead directly
     */
    @Deprecated
    public void packIfNeeded() {
        pack();
    }
}
