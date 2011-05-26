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

package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.figure.*;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.ui.layer.AbstractLayerConfigurationEditor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.util.ObjectUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class VectorDataLayerEditor extends AbstractLayerConfigurationEditor {

    private static final String FILL_COLOR_NAME = DefaultFigureStyle.FILL_COLOR.getName();
    private static final String FILL_OPACITY_NAME = DefaultFigureStyle.FILL_OPACITY.getName();
    private static final String STROKE_COLOR_NAME = DefaultFigureStyle.STROKE_COLOR.getName();
    private static final String STROKE_WIDTH_NAME = DefaultFigureStyle.STROKE_WIDTH.getName();
    private static final String STROKE_OPACITY_NAME = DefaultFigureStyle.STROKE_OPACITY.getName();

    private final SelectionChangeHandler selectionChangeHandler;
    private final StyleUpdater styleListener;
    private final AtomicBoolean isAdjusting;

    public VectorDataLayerEditor() {
        selectionChangeHandler = new SelectionChangeHandler();
        styleListener = new StyleUpdater();
        isAdjusting = new AtomicBoolean(false);
    }

    @Override
    protected void addEditablePropertyDescriptors() {
        final AbstractShapeFigure[] shapeFigures = getFigures();

        final PropertyDescriptor fillColor = new PropertyDescriptor(DefaultFigureStyle.FILL_COLOR);
        fillColor.setDefaultValue(getStyleProperty(FILL_COLOR_NAME, shapeFigures));
        final PropertyDescriptor fillOpacity = new PropertyDescriptor(DefaultFigureStyle.FILL_OPACITY);
        fillOpacity.setDefaultValue(getStyleProperty(FILL_OPACITY_NAME, shapeFigures));

        final PropertyDescriptor strokeColor = new PropertyDescriptor(DefaultFigureStyle.STROKE_COLOR);
        strokeColor.setDefaultValue(getStyleProperty(STROKE_COLOR_NAME, shapeFigures));
        final PropertyDescriptor strokeWidth = new PropertyDescriptor(DefaultFigureStyle.STROKE_WIDTH);
        strokeWidth.setDefaultValue(getStyleProperty(STROKE_WIDTH_NAME, shapeFigures));
        final PropertyDescriptor strokeOpacity = new PropertyDescriptor(DefaultFigureStyle.STROKE_OPACITY);
        strokeOpacity.setDefaultValue(getStyleProperty(STROKE_OPACITY_NAME, shapeFigures));

        addPropertyDescriptor(fillColor);
        addPropertyDescriptor(fillOpacity);
        addPropertyDescriptor(strokeColor);
        addPropertyDescriptor(strokeWidth);
        addPropertyDescriptor(strokeOpacity);
    }

    @Override
    public void handleLayerContentChanged() {
        final BindingContext bindingContext = getBindingContext();

        if (isAdjusting.compareAndSet(false, true)) {
            try {
                final AbstractShapeFigure[] selectedFigures = getFigures();
                updateBinding(selectedFigures, bindingContext);
            } finally {
                isAdjusting.set(false);
            }
        }

        super.handleLayerContentChanged();
    }

    @Override
    public void handleEditorAttached() {
        FigureEditor figureEditor = getAppContext().getSelectedProductSceneView().getFigureEditor();
        if (figureEditor != null) {
            figureEditor.addSelectionChangeListener(selectionChangeHandler);
        }
        getBindingContext().addPropertyChangeListener(styleListener);
    }

    @Override
    public void handleEditorDetached() {
        FigureEditor figureEditor = getAppContext().getSelectedProductSceneView().getFigureEditor();
        if (figureEditor != null) {
            figureEditor.removeSelectionChangeListener(selectionChangeHandler);
        }
        getBindingContext().removePropertyChangeListener(styleListener);
    }

    private void updateBinding(AbstractShapeFigure[] selectedFigures, BindingContext bindingContext) {
        final PropertySet propertySet = bindingContext.getPropertySet();
        setPropertyValue(FILL_COLOR_NAME, propertySet, getStyleProperty(FILL_COLOR_NAME, selectedFigures));
        setPropertyValue(FILL_OPACITY_NAME, propertySet, getStyleProperty(FILL_OPACITY_NAME, selectedFigures));
        setPropertyValue(STROKE_COLOR_NAME, propertySet, getStyleProperty(STROKE_COLOR_NAME, selectedFigures));
        setPropertyValue(STROKE_WIDTH_NAME, propertySet, getStyleProperty(STROKE_WIDTH_NAME, selectedFigures));
        setPropertyValue(STROKE_OPACITY_NAME, propertySet, getStyleProperty(STROKE_OPACITY_NAME, selectedFigures));
    }

    private void setPropertyValue(String propertyName, PropertySet propertySet, Object value) {
        final Object oldValue = propertySet.getValue(propertyName);
        if (!ObjectUtils.equalObjects(oldValue, value)) {
            propertySet.setValue(propertyName, value);
        }
    }

    private Object getStyleProperty(String propertyName, AbstractShapeFigure[] figures) {
        Object lastProperty = null;
        for (AbstractShapeFigure figure : figures) {
            final Object currentProperty = figure.getNormalStyle().getValue(propertyName);
            if (lastProperty == null) {
                lastProperty = currentProperty;
            } else {
                if (!lastProperty.equals(currentProperty)) {
                    return null;
                }
            }
        }
        return lastProperty;
    }

    private AbstractShapeFigure[] getFigures() {
        AbstractShapeFigure[] figures = new AbstractShapeFigure[0];
        if (getAppContext() != null) {
            final ProductSceneView sceneView = getAppContext().getSelectedProductSceneView();
            SimpleFeatureFigure[] featureFigures = sceneView.getSelectedFeatureFigures();
            if (featureFigures.length == 0) {
                featureFigures = getAllFigures((VectorDataLayer) getCurrentLayer());
            }
            List<AbstractShapeFigure> selFigureList = new ArrayList<AbstractShapeFigure>(7);
            for (SimpleFeatureFigure featureFigure : featureFigures) {
                if (featureFigure instanceof AbstractShapeFigure) {
                    selFigureList.add((AbstractShapeFigure) featureFigure);
                }
            }
            figures = selFigureList.toArray(new AbstractShapeFigure[selFigureList.size()]);
        }
        return figures;
    }

    private SimpleFeatureFigure[] getAllFigures(VectorDataLayer vectorDataLayer) {
        final FigureCollection figureCollection = vectorDataLayer.getFigureCollection();
        ArrayList<SimpleFeatureFigure> selectedFigures = new ArrayList<SimpleFeatureFigure>(
                figureCollection.getFigureCount());
        for (Figure figure : figureCollection.getFigures()) {
            if (figure instanceof SimpleFeatureFigure) {
                selectedFigures.add((SimpleFeatureFigure) figure);
            }
        }
        return selectedFigures.toArray(new SimpleFeatureFigure[selectedFigures.size()]);
    }


    private void transferPropertyValueToStyle(PropertySet propertySet, String propertyName, FigureStyle style) {
        final Object value = propertySet.getValue(propertyName);
        if (value != null) {
            style.setValue(propertyName, value);
        }
    }

    private class SelectionChangeHandler extends AbstractSelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            handleLayerContentChanged();
        }
    }

    private class StyleUpdater implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() == null) {
                return;
            }
            final AbstractShapeFigure[] selectedFigures = getFigures();
            final BindingContext bindContext = getBindingContext();
            if (isAdjusting.compareAndSet(false, true)) {
                try {
                    for (AbstractShapeFigure selectedFigure : selectedFigures) {
                        final Object oldFigureValue = selectedFigure.getNormalStyle().getValue(evt.getPropertyName());
                        final Object newValue = evt.getNewValue();
                        if (!newValue.equals(oldFigureValue)) {
                            final FigureStyle origStyle = selectedFigure.getNormalStyle();
                            final DefaultFigureStyle style = new DefaultFigureStyle();
                            style.fromCssString(origStyle.toCssString());
                            transferPropertyValueToStyle(bindContext.getPropertySet(), evt.getPropertyName(), style);
                            selectedFigure.setNormalStyle(style);
                        }
                    }
                } finally {
                    isAdjusting.set(false);
                }
            }
        }
    }
}
