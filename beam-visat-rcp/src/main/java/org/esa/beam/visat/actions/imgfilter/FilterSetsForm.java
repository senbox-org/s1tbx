package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.FilterSetStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * @author Norman
 */
public class FilterSetsForm extends JPanel implements FilterSetForm.Listener {

    private final String sourceBandName;
    private final JTabbedPane tabbedPane;
    private final JTextField targetBandNameField;
    private final JComboBox<Integer> iterationCountComboBox;
    private Filter selectedFilter;

    public FilterSetsForm(String sourceBandName, final FilterSetForm.Listener selectionListener, FilterSetStore filterSetStore, FilterEditor filterEditor, FilterSet... filterSets) {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));

        this.sourceBandName = sourceBandName;

        tabbedPane = new JTabbedPane();
        for (FilterSet filterSet : filterSets) {
            FilterSetForm filterSetForm = new FilterSetForm(filterSet, filterSetStore, filterEditor);
            filterSetForm.addListener(selectionListener);
            filterSetForm.addListener(this);
            tabbedPane.addTab(filterSet.getName(), filterSetForm);
        }
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                FilterSetForm filterSetForm = (FilterSetForm) tabbedPane.getSelectedComponent();
                Filter selectedFilter = filterSetForm.getSelectedFilterModel();
                filterSetForm.notifyFilterModelSelected(selectedFilter);
            }
        });

        targetBandNameField = new JTextField(10);
        iterationCountComboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});

        JPanel namePanel = new JPanel(new BorderLayout(2, 2));
        namePanel.add(new JLabel("Band name:"), BorderLayout.WEST);
        namePanel.add(targetBandNameField, BorderLayout.CENTER);

        JPanel iterPanel = new JPanel(new BorderLayout(2, 2));
        iterPanel.add(new JLabel("Number of iterations:"), BorderLayout.WEST);
        iterPanel.add(iterationCountComboBox, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(2, 2));
        inputPanel.add(namePanel, BorderLayout.NORTH);
        inputPanel.add(iterPanel, BorderLayout.SOUTH);

        add(tabbedPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        updateBandNameField();
    }

    public Filter getSelectedFilter() {
        return selectedFilter;
    }

    public String getTargetBandName() {
        return targetBandNameField.getText();
    }

    public int getIterationCount() {
        return (Integer) iterationCountComboBox.getSelectedItem();
    }

    @Override
    public void filterModelSelected(FilterSet filterSet, Filter filter) {
        if (this.selectedFilter != filter) {
            this.selectedFilter = filter;
            updateBandNameField();
        }
    }

    @Override
    public void filterModelChanged(FilterSet filterSet, Filter filter) {
        if (this.selectedFilter == filter) {
            updateBandNameField();
        }
    }

    @Override
    public void filterModelAdded(FilterSet filterSet, Filter filter) {

    }

    @Override
    public void filterModelRemoved(FilterSet filterSet, Filter filter) {

    }

    private void updateBandNameField() {
        if (selectedFilter != null) {
            targetBandNameField.setText(sourceBandName + "_" + selectedFilter.getShorthand());
            targetBandNameField.setEditable(true);
        } else {
            targetBandNameField.setText(sourceBandName + "_?");
            targetBandNameField.setEditable(false);
        }
    }
}
