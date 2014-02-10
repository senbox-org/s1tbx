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

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.jfree.ui.L1R3ButtonPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that presents the user with a sequence of steps for completing a task.  The panel
 * contains "Next" and "Previous" buttons, allowing the user to navigate through the task.
 * <p/>
 * When the user backs up by one or more steps, the panel keeps the completed steps so that
 * they can be reused if the user doesn't change anything - this handles the cases where the user
 * backs up a few steps just to review what has been completed.
 * <p/>
 * But if the user changes some options in an earlier step, then the panel may have to discard
 * the later steps and have them repeated.
 * <p/>
 */
public class TaskSequencePanel extends JPanel implements ActionListener {

    /**
     * The current step in the process (starting at step zero).
     */
    private int step;

    /**
     * A reference to the current panel.
     */
    private TaskPanel currentPanel;

    /**
     * A list of references to the panels the user has already seen - used for navigating through
     * the steps that have already been completed.
     */
    private List<TaskPanel> panels;

    /**
     * A handy reference to the "previous" button.
     */
    private JButton previousButton;

    /**
     * A handy reference to the "next" button.
     */
    private JButton nextButton;

    /**
     * A handy reference to the "finish" button.
     */
    private JButton finishButton;

    /**
     * A handy reference to the "help" button.
     */
    private JButton helpButton;

    private final AbstractToolView view;

    /**
     * Standard constructor - builds and returns a new TaskSequencePanel.
     *
     * @param firstPanel the first panel.
     */
    public TaskSequencePanel(final AbstractToolView view, final TaskPanel firstPanel) {
        super(new BorderLayout());

        this.view = view;
        this.currentPanel = firstPanel;
        this.currentPanel.setOwner(this);
        this.step = 0;
        this.panels = new ArrayList<TaskPanel>(4);
        this.panels.add(firstPanel);

        createContent();
    }

    /**
     * the user interface for the panel.
     */
    public void createContent() {

        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.add(this.panels.get(0));
        final L1R3ButtonPanel buttons = new L1R3ButtonPanel("Help", "Back", "Next", "Finish");

        this.helpButton = buttons.getLeftButton();
        this.helpButton.setEnabled(false);

        this.previousButton = buttons.getRightButton1();
        this.previousButton.setActionCommand("previousButton");
        this.previousButton.addActionListener(this);
        this.previousButton.setEnabled(false);

        this.nextButton = buttons.getRightButton2();
        this.nextButton.setActionCommand("nextButton");
        this.nextButton.addActionListener(this);
        this.nextButton.setEnabled(true);

        this.finishButton = buttons.getRightButton3();
        this.finishButton.setActionCommand("finishButton");
        this.finishButton.addActionListener(this);
        this.finishButton.setEnabled(false);

        buttons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        this.add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Returns the total number of steps in the sequence, if this number is known.  Otherwise
     * this method returns zero.  Subclasses should override this method unless the number of steps
     * is not known.
     *
     * @return the number of steps.
     */
    public int getStepCount() {
        return 0;
    }

    /**
     * Returns true if it is possible to back up to the previous panel, and false otherwise.
     *
     * @return boolean.
     */
    public boolean canDoPreviousPanel() {
        return (this.step > 0);
    }

    /**
     * Returns true if there is a 'next' panel, and false otherwise.
     *
     * @return boolean.
     */
    public boolean canDoNextPanel() {
        return this.currentPanel.hasNextPanel();
    }

    /**
     * Returns true if it is possible to finish the sequence at this point (possibly with defaults
     * for the remaining entries).
     *
     * @return boolean.
     */
    public boolean canFinish() {
        return this.currentPanel.canFinish();
    }

    /**
     * Returns the panel for the specified step (steps are numbered from zero).
     *
     * @param step the current step.
     * @return the panel.
     */
    public TaskPanel getTaskPanel(final int step) {
        if (step < this.panels.size()) {
            return this.panels.get(step);
        } else {
            return null;
        }
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("nextButton")) {
                next();
            } else if (command.equals("previousButton")) {
                previous();
            } else if (command.equals("finishButton")) {
                finish();
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    /**
     * Handles a click on the "previous" button, by displaying the previous panel in the sequence.
     */
    public void previous() {
        if (this.step > 0) {
            final TaskPanel previousPanel = getTaskPanel(this.step - 1);
            // tell the panel that we are returning
            previousPanel.returnFromLaterStep();
            this.remove(this.currentPanel);
            this.add(previousPanel);
            this.step = this.step - 1;
            this.currentPanel = previousPanel;
            updateState();
        }
    }

    /**
     * Displays the next step in the sequence.
     */
    public void next() {
        if (!this.currentPanel.validateInput()) {
            return;
        }
        TaskPanel nextPanel = getTaskPanel(this.step + 1);
        if (nextPanel != null) {
            if (!this.currentPanel.canRedisplayNextPanel()) {
                nextPanel = this.currentPanel.getNextPanel();
            }
        } else {
            nextPanel = this.currentPanel.getNextPanel();
        }

        this.step = this.step + 1;
        if (this.step < this.panels.size()) {
            this.panels.set(this.step, nextPanel);
        } else {
            this.panels.add(nextPanel);
        }

        this.remove(this.currentPanel);
        this.add(nextPanel);

        this.currentPanel = nextPanel;
        this.currentPanel.setOwner(this);
        updateState();
    }

    /**
     * Finishes the sequence.
     */
    public void finish() {
        this.currentPanel.finish();
    }

    /**
     * Enables/disables the buttons according to the current step.  A good idea would be to ask the
     * panels to return the status...
     */
    private void enableButtons() {
        this.previousButton.setEnabled(this.step > 0);
        this.nextButton.setEnabled(canDoNextPanel());
        this.finishButton.setEnabled(canFinish());
        // this.helpButton.setEnabled(helpId != null);
    }

    public void updateState() {
        enableButtons();
        repaint();

        final Window win = view.getPaneWindow();
        win.setSize(new Dimension(win.getWidth() + 1, win.getHeight()));
        win.setSize(new Dimension(win.getWidth() - 1, win.getHeight()));
    }
}
