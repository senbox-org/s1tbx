/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.ui;

import com.bc.ceres.core.ProgressBarProgressMonitor;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.util.GregorianCalendar;

/**
 * @author Thomas Storm
 */
public class DownloadProgressBarPM extends ProgressBarProgressMonitor implements LabelledProgressBarPM {

    private final JProgressBar progressBar;
    private final JLabel preMessageLabel;
    private JLabel postMessageLabel;
    private int totalWork;
    private int currentWork;
    private long startTime;
    private JButton cancelButton;

    public DownloadProgressBarPM(JProgressBar progressBar, JLabel preMessageLabel, JLabel postMessageLabel, JButton cancelButton) {
        super(progressBar, preMessageLabel);
        this.progressBar = progressBar;
        this.preMessageLabel = preMessageLabel;
        this.postMessageLabel = postMessageLabel;
        this.cancelButton = cancelButton;
    }

    @Override
    public void setPreMessage(String preMessageText) {
        setTaskName(preMessageText);
    }

    @Override
    public void setPostMessage(String postMessageText) {
        postMessageLabel.setText(postMessageText);
    }

    @Override
    public void setTooltip(String tooltip) {
        preMessageLabel.setToolTipText(tooltip);
        postMessageLabel.setToolTipText(tooltip);
        progressBar.setToolTipText(tooltip);
    }

    @Override
    public void beginTask(String name, int totalWork) {
        super.beginTask(name, totalWork);
        this.totalWork = totalWork;
        this.currentWork = 0;
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
    }

    @Override
    public void worked(int work) {
        super.worked(work);
        currentWork += work;
    }

    @Override
    protected void setDescription(String description) {
    }

    @Override
    protected void setVisibility(boolean visible) {
        // once the progress bar has been made visible, it shall always be visible
        progressBar.setVisible(true);
    }

    @Override
    protected void setRunning() {
    }

    @Override
    protected void finish() {
        progressBar.setIndeterminate(false);
        cancelButton.setEnabled(false);
    }

    @Override
    public int getTotalWork() {
        return totalWork;
    }

    @Override
    public int getCurrentWork() {
        return currentWork;
    }

    public void updateTask(int additionalWork) {
        totalWork += additionalWork;
        progressBar.setMaximum(totalWork);
        progressBar.updateUI();
    }

    public void resetStartTime() {
        GregorianCalendar gc = new GregorianCalendar();
        startTime = gc.getTimeInMillis();
    }

    public long getStartTime() {
        return startTime;
    }
}
