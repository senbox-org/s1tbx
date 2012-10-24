package org.esa.beam.opendap.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.ui.RegionBoundsInputUI;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import thredds.catalog.ThreddsMetadata;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class RegionFilter implements FilterComponent {

    private final JButton applyButton;
    private final List<FilterChangeListener> filterChangeListeners;

    private RegionBoundsInputUI regionBoundsInputUI;

    Property eastBoundProperty;
    Property westBoundProperty;
    Property northBoundProperty;
    Property southBoundProperty;
    private final JCheckBox useRegionFilter;

    public RegionFilter(final JCheckBox useRegionFilter) {
        this.useRegionFilter = useRegionFilter;
        filterChangeListeners = new ArrayList<FilterChangeListener>();
        useRegionFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireFilterChanged();
                updateUI(useRegionFilter.isSelected(), useRegionFilter.isSelected());
            }
        });
        applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireFilterChanged();
                updateUI(false, true);
            }
        });
    }

    @Override
    public JComponent getUI() {
        final JPanel regionPanel = new JPanel(new BorderLayout());
        regionBoundsInputUI = new RegionBoundsInputUI();
        regionBoundsInputUI.getUI().setBorder(new EmptyBorder(0, 0, 8, 0));
        regionPanel.add(regionBoundsInputUI.getUI(), BorderLayout.NORTH);
        regionPanel.add(applyButton, BorderLayout.EAST);

        PropertySet propertySet = regionBoundsInputUI.getBindingContext().getPropertySet();
        eastBoundProperty = propertySet.getProperty(RegionBoundsInputUI.PROPERTY_EAST_BOUND);
        westBoundProperty = propertySet.getProperty(RegionBoundsInputUI.PROPERTY_WEST_BOUND);
        northBoundProperty = propertySet.getProperty(RegionBoundsInputUI.PROPERTY_NORTH_BOUND);
        southBoundProperty = propertySet.getProperty(RegionBoundsInputUI.PROPERTY_SOUTH_BOUND);

        BoundsChangeListener boundsChangeListener = new BoundsChangeListener();
        eastBoundProperty.addPropertyChangeListener(boundsChangeListener);
        westBoundProperty.addPropertyChangeListener(boundsChangeListener);
        northBoundProperty.addPropertyChangeListener(boundsChangeListener);
        southBoundProperty.addPropertyChangeListener(boundsChangeListener);

        updateUI(false, false);

        return regionPanel;
    }

    @Override
    public boolean accept(OpendapLeaf leaf) {
        ThreddsMetadata.GeospatialCoverage geospatialCoverage = leaf.getDataset().getGeospatialCoverage();
        if (geospatialCoverage == null) {
            return true;
        }

        Double eastBound = eastBoundProperty.getValue();
        Double westBound = westBoundProperty.getValue();
        Double northBound = northBoundProperty.getValue();
        Double southBound = southBoundProperty.getValue();

        LatLonPointImpl northWest = new LatLonPointImpl(northBound, westBound);
        LatLonPointImpl southEast = new LatLonPointImpl(southBound, eastBound);

        LatLonRect latLonRect = new LatLonRect(northWest, southEast);
        return latLonRect.intersect(geospatialCoverage.getBoundingBox()) != null;
    }

    @Override
    public void addFilterChangeListener(FilterChangeListener listener) {
        filterChangeListeners.add(listener);
    }

    private void fireFilterChanged() {
        for (FilterChangeListener filterChangeListener : filterChangeListeners) {
            filterChangeListener.filterChanged();
        }
    }

    private class BoundsChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateUI(true, true);
        }
    }

    private void updateUI(boolean enableApplyButton, boolean enableRegionBoundsInputUI) {
        if (useRegionFilter.isSelected()) {
            applyButton.setEnabled(enableApplyButton);
            regionBoundsInputUI.setEnabled(enableRegionBoundsInputUI);
        } else {
            applyButton.setEnabled(false);
            regionBoundsInputUI.setEnabled(false);
        }
    }
}