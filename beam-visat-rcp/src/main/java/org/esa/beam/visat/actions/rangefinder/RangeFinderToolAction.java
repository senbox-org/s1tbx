/*
 * $Id: RangeFinderToolAction.java,v 1.1 2007/04/19 10:16:12 marcop Exp $
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
package org.esa.beam.visat.actions.rangefinder;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ToolAction;

/**
 * This action can measure the distance covered by a path of points.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class RangeFinderToolAction extends ToolAction {

    @Override
    public void updateState(final CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        GeoCoding geoCoding = null;
        if (view != null && view.getProduct() != null) {
            geoCoding = view.getProduct().getGeoCoding();
        }
        setEnabled(geoCoding != null && geoCoding.canGetPixelPos());
    }


}
