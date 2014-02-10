/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir;

import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that provides the user interface for a single step in a TaskSequencePanel.
 *
 */
public abstract class TaskPanel extends JPanel {

    /** The owner. */
    private TaskSequencePanel owner;
    private final String panelTitle;
    private final static Font titleFont = new Font("Ariel", Font.BOLD, 14);

    protected boolean finishing = false;

    /**
     * Creates a new panel.
     *
     * @param name  the panel name.
     */
    protected TaskPanel(final String name) {
        super(new BorderLayout(3,3));
        this.panelTitle = name;

        this.setPreferredSize(new Dimension(500,500));
    }

    public String getPanelTitle() {
        return panelTitle;
    }

    /**
     * Returns a reference to the dialog that owns the panel.
     *
     * @return the owner.
     */
    public TaskSequencePanel getOwner() {
        return this.owner;
    }

    /**
     * Sets the reference to the dialog that owns the panel (this is called automatically by
     * the dialog when the panel is added to the dialog).
     *
     * @param owner  the owner.
     */
    public void setOwner(final TaskSequencePanel owner) {
        this.owner = owner;
    }

    public void finish() {
    }

    protected static void showErrorMsg(String msg) {
        VisatApp.getApp().showErrorDialog("Oops!", msg);
    }

    protected JLabel createTitleLabel() {
        final JLabel titleLabel = new JLabel(getPanelTitle());
        titleLabel.setFont(titleFont);
        return titleLabel;
    }

    protected static JPanel createTextPanel(final String title, final String text) {
        final JPanel textPanel = new JPanel(new BorderLayout(2,2));
        if(title != null)
            textPanel.setBorder(BorderFactory.createTitledBorder(title));
        final JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPanel.add(textPane, BorderLayout.CENTER);
        return textPanel;
    }

    protected JPanel createInstructionsPanel(final String title, final String text) {
        final JPanel instructPanel = new JPanel(new BorderLayout(2, 2));
        instructPanel.add(createTitleLabel(), BorderLayout.NORTH);
        instructPanel.add(createTextPanel(title, text), BorderLayout.CENTER);
        return instructPanel;
    }

    public abstract boolean validateInput();

    /**
     * This method is called when the dialog redisplays this panel as a result of the user clicking
     * the "Previous" button.  Inside this method, subclasses should make a note of their current
     * state, so that they can decide what to do when the user hits "Next".
     */
    public abstract void returnFromLaterStep();

    /**
     * Returns true if it is OK to redisplay the last version of the next panel, or false if a new
     * version is required.
     *
     * @return boolean.
     */
    public abstract boolean canRedisplayNextPanel();

    /**
     * Returns true if there is a next panel.
     *
     * @return boolean.
     */
    public abstract boolean hasNextPanel();

    /**
     * Returns true if it is possible to finish from this panel.
     *
     * @return boolean.
     */
    public abstract boolean canFinish();

    /**
     * Returns the next panel in the sequence, given the current user input.  Returns null if this
     * panel is the last one in the sequence.
     *
     * @return the next panel in the sequence.
     */
    public abstract TaskPanel getNextPanel();

}
