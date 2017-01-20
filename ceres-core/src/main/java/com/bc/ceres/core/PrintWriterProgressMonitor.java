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
package com.bc.ceres.core;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;

/**
 * A progress monitor which prints progress using a {@link PrintWriter}.
 */
public class PrintWriterProgressMonitor implements ProgressMonitor {

    private PrintWriter printWriter;
    private boolean canceled;
    private String taskName;
    private String subTaskName;
    private double totalWork;
    private double currentWork;
    private int printMinorStepPercentage;
    private int printStepPercentage;
    private int percentageWorked;
    private int lastMinorPercentagePrinted;
    private int lastPercentagePrinted;

    public PrintWriterProgressMonitor(OutputStream output) {
        this(new PrintWriter(output, true));
    }

    public PrintWriterProgressMonitor(PrintWriter output) {
        Assert.notNull(output, "output");
        printWriter = output;
        printMinorStepPercentage = 2; // = 2%
        printStepPercentage = 10; // =10%
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getSubTaskName() {
        return subTaskName;
    }

    public void setSubTaskName(String subTaskName) {
        this.subTaskName = subTaskName;
    }

    public int getPrintMinorStepPercentage() {
        return printMinorStepPercentage;
    }

    public int getPrintStepPercentage() {
        return printStepPercentage;
    }

    public void setPrintMinorStepPercentage(int printMinorStepPercentage) {
        this.printMinorStepPercentage = printMinorStepPercentage;
    }

    public void setPrintStepPercentage(int printStepPercentage) {
        this.printStepPercentage = printStepPercentage;
    }

    public int getPercentageWorked() {
        return percentageWorked;
    }

    public void beginTask(String name, int totalWork) {
        this.taskName = name;
        this.totalWork = totalWork;
        currentWork = 0.0;
        percentageWorked = 0;
        lastMinorPercentagePrinted = 0;
        lastPercentagePrinted = 0;
        canceled = false;
        printStartMessage(printWriter);
    }

    public void done() {
        printDoneMessage(printWriter);
    }

    public void worked(int work) {
        internalWorked(work);
    }

    public void internalWorked(double work) {
        currentWork += work;
        percentageWorked = (int)(100.0 * currentWork / totalWork);
        if (percentageWorked - lastMinorPercentagePrinted >= getPrintMinorStepPercentage()) {
            lastMinorPercentagePrinted = percentageWorked;
            if (percentageWorked - lastPercentagePrinted >= getPrintStepPercentage()) {
                printWorkedMessage(printWriter);
                lastPercentagePrinted = percentageWorked;
            } else {
                printMinorWorkedMessage(printWriter);
            }
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
        if (isCanceled()) {
            printCanceledMessage(printWriter);
        }
    }

    protected void printStartMessage(PrintWriter pw) {
        pw.printf(MessageFormat.format("{0}, started\n", getMessage()));
    }

    protected void printWorkedMessage(PrintWriter pw) {
        pw.println(MessageFormat.format("{0}, {1}% worked", getMessage(), getPercentageWorked()));
    }

    protected void printMinorWorkedMessage(PrintWriter pw) {
    }

    protected void printDoneMessage(PrintWriter pw) {
        pw.println(MessageFormat.format("{0}, done", getMessage()));
    }

    protected void printCanceledMessage(PrintWriter pw) {
        pw.println(MessageFormat.format("{0}, cancelation requested", getMessage()));
    }

    protected String getMessage() {
        boolean validTaskName = taskName != null && taskName.length() > 0 ;
        boolean validSubTaskName = subTaskName != null && subTaskName.length() > 0;
        String message = "";
        if (validTaskName && validSubTaskName) {
            message = taskName + " - " + subTaskName;
        } else if (validTaskName) {
            message = taskName;
        } else if (validSubTaskName) {
            message = subTaskName;
        }
        return message;
    }
}
