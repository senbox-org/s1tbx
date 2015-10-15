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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.AbstractParamEditor;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.param.validators.StringArrayValidator;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventListener;

/**
 * An editor which uses a {@link JList}.
 */
public class ListEditor extends AbstractParamEditor {

    private JList _list;
    private JScrollPane _listScrollPane;

    public ListEditor(Parameter parameter) {
        super(parameter, false);
    }

    public JList getList() {
        return _list;
    }

    public JScrollPane getListScrollPane() {
        return _listScrollPane;
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getListScrollPane();
    }

    @Override
    protected void initUI() {

        setDefaultLabelComponent(true);

        _list = new JList();
        nameEditorComponent(_list);
        _listScrollPane = new JScrollPane(_list);
        nameComponent(_listScrollPane, "ScrollPane");

        // Configure scroll pane
        //
        _listScrollPane.setAutoscrolls(true);
        _listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        _listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Configure list box
        //
        if (getParameter().getProperties().getDescription() != null) {
            _list.setToolTipText(getParameter().getProperties().getDescription());
        }
        _list.setListData(getParameter().getProperties().getValueSet());
        _list.setEnabled(!getParameter().getProperties().isReadOnly());
        _list.setVisibleRowCount(6); /* really 6? */
        _list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setSelectedIndices(getParameter());
        _list.addListSelectionListener(createListSelectionListener());
        _list.addFocusListener(createListFocusListener());
    }


    @Override
    public void updateUI() {
        super.updateUI();

        // @todo 1 nf/** - Who wrote the following? What does it???
        // was written by Sabine speak with her
        EventListener[] li = _list.getListeners(ListSelectionListener.class);
        removeListSelectionListener(li);
        setSelectedIndices(getParameter());
        addListSelectionListener(li);

        if (_list.isEnabled() != isEnabled()) {
            _list.setEnabled(isEnabled());
        }
    }

    @Override
    public void reconfigureUI() {
        EventListener[] li = _list.getListeners(ListSelectionListener.class);
        removeListSelectionListener(li);
        _list.setListData(getParameter().getProperties().getValueSet());
        setSelectedIndices(getParameter());
        addListSelectionListener(li);
    }

    private FocusAdapter createListFocusListener() {
        return new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                JList list = (JList) e.getSource();
                Object[] values = list.getSelectedValues();
                if (values.length == 0) {
                    getParameter().setValue(null,
                                            getExceptionHandler());
                } else {
                    getParameter().setValue(StringUtils.toStringArray(values),
                                            getExceptionHandler());
                }
            }
        };
    }

    private ListSelectionListener createListSelectionListener() {
        return new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                JList list = (JList) e.getSource();
                Object[] values = list.getSelectedValues();
                if (values.length == 0) {
                    getParameter().setValue(null,
                                            getExceptionHandler());
                } else {
                    getParameter().setValue(StringUtils.toStringArray(values),
                                            getExceptionHandler());
                }
            }
        };
    }

    private void addListSelectionListener(EventListener[] li) {
        for (int i = 0; i < li.length; i++) {
            _list.addListSelectionListener((ListSelectionListener) li[i]);
        }
    }

    private void removeListSelectionListener(EventListener[] li) {
        for (int i = 0; i < li.length; i++) {
            _list.removeListSelectionListener((ListSelectionListener) li[i]);
        }
    }

    private void setSelectedIndices(Parameter parameter) {
        if (parameter.getValue() != null) {
            Debug.assertTrue(parameter.getValue() instanceof String[]);
            Debug.assertTrue(parameter.getValidator() instanceof StringArrayValidator);
            int[] indexes = ((StringArrayValidator) parameter.getValidator()).getValueSetIndices(parameter);
            _list.setSelectedIndices(indexes);
        }
    }
}
