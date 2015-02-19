/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.dialogs;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Check List Dialog
 */
public class CheckListDialog extends ModalDialog {
    private final List<JToggleButton> toggleList = new ArrayList<>(3);
    protected final Map<String, Boolean> items;
    private final boolean singleSelection;
    private boolean ok = false;

    public CheckListDialog(final String title) {
        this(title, new HashMap<>(3), false);
    }

    public CheckListDialog(final String title, final Map<String, Boolean> items, final boolean singleSelection) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK, null);
        this.items = items;
        this.singleSelection = singleSelection;

        initContent();
    }

    protected void initContent() {
        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        final ButtonGroup group = new ButtonGroup();

        for (String name : items.keySet()) {
            final JToggleButton btn;
            if(singleSelection) {
                btn = new JRadioButton(name);
                group.add(btn);
            } else {
                btn = new JCheckBox(name);
            }
            toggleList.add(btn);
            content.add(btn);

            btn.setSelected(items.get(name));
        }

        getJDialog().setMinimumSize(new Dimension(200, 100));

        setContent(content);
    }

    protected void onOK() {
        for (JToggleButton btn : toggleList) {
            items.put(btn.getText(), btn.isSelected());
        }

        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

}