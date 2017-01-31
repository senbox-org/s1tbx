/*
 * Copyright (C) 2017 Brockmann Consult GmbH (info@brockmann-consult.de)
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
public class PrintWriterConciseProgressMonitor extends PrintWriterProgressMonitor {

    public PrintWriterConciseProgressMonitor(OutputStream output) {
        super(new PrintWriter(output, true));
    }

    @Override
    protected void printStartMessage(PrintWriter pw) {
        pw.println(MessageFormat.format("{0}", getMessage()));
        pw.flush();
    }

    @Override
    protected void printWorkedMessage(PrintWriter pw) {
        pw.print(MessageFormat.format("{0}%", getPercentageWorked()));
        pw.flush();
    }

    @Override
    protected void printMinorWorkedMessage(PrintWriter pw) {
        pw.print(".");
        pw.flush();
    }

    @Override
    protected void printDoneMessage(PrintWriter pw) {
        pw.println(" done.");
        pw.flush();
    }
}
