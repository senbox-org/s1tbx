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

package org.esa.beam.examples.selection;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JOptionPane;

public class SelectionAwareAction extends AbstractVisatAction implements SelectionChangeListener {

    private SelectionManager selectionManager;

    @Override
    public void actionPerformed(CommandEvent event) {
        // if the action is invoked the number of selected placemarks is shown.
        final Object[] selectedValues = getSelectionManager().getSelection().getSelectedValues();
        int numPlacemarks = selectedValues.length;
        JOptionPane.showMessageDialog(null, String.format("Number of selected placemarks is %d", numPlacemarks));
    }

    @Override
    public void updateState(CommandEvent event) {
        // Based on the current selection the enable state is set
        final SelectionContext selectionContext = getSelectionManager().getSelectionContext();
        if (selectionContext == null) {
            setEnabled(false);
            return;
        }
        final Selection selection = selectionContext.getSelection();
        if (selection.isEmpty()) {
            setEnabled(false);
            return;
        }
        final Object selectedValue = selection.getSelectedValue();
        if (!(selectedValue instanceof SimpleFeatureFigure)) {
            setEnabled(false);
            return;
        }

        // Action is only enabled if a placemark is selected
        SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) selectedValue;
        setEnabled(featureFigure.getSimpleFeature().getType() == Placemark.getFeatureType());
    }

    private SelectionManager getSelectionManager() {
        // on first access the selection manager is initialised
        // this can not be done in the constructor, because at the time it is called the AppContext is not
        // fully initialised
        if (selectionManager == null) {
            selectionManager = getAppContext().getApplicationPage().getSelectionManager();
            // todo - this is needed to consistently update the enable state of the action
            // todo - probably a call to updateChange() is missing in the framework at the time the menu gets visible
            selectionManager.addSelectionChangeListener(this);
        }
        return selectionManager;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Implementation of the SelectionChangeListener interface

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        updateState();
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        updateState();
    }

    //
    /////////////////////////////////////////////////////////////////////////////////

}
