/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.operators.tooladapter;

import com.bc.ceres.core.ProgressMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation for process output processing.
 * The class would expect specific patterns (i.e. RegEx) for errors and progress messages.
 * Any other message (which does not respect one of these patterns) will be considered informational.
 * If no pattern is given in ctor, all messages are treated as informational.
 *
 * @author Cosmin Cara
 */
class DefaultOutputConsumer implements ProcessOutputConsumer {

    private Pattern error;
    private Pattern progress;
    private Logger logger;
    private ProgressMonitor progressMonitor;
    private List<String> processOutput;
    private static final int MAX_UNITS = 100;

    public DefaultOutputConsumer() {
        this(null, null, null);
    }

    public DefaultOutputConsumer(String progressPattern, ProgressMonitor pm) {
        this(progressPattern, null, pm);
    }

    public DefaultOutputConsumer(String progressPattern, String errorPattern, ProgressMonitor pm) {
        progressMonitor = pm;
        if (errorPattern != null && errorPattern.trim().length() > 0) {
            error = Pattern.compile(errorPattern, Pattern.CASE_INSENSITIVE);
        }
        if (progressPattern != null && progressPattern.trim().length() > 0) {
            progress = Pattern.compile(progressPattern, Pattern.CASE_INSENSITIVE);
            initializeProgressMonitor();
        }
        processOutput = new ArrayList<>();
    }

    public void setProgressMonitor(ProgressMonitor monitor) {
        this.progressMonitor = monitor;
        initializeProgressMonitor();
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(DefaultOutputConsumer.class.getName());
        }
        return logger;
    }

    @Override
    public void consumeOutput(String line) {
        Matcher matcher = null;
        try {
            if (progress != null && (matcher = progress.matcher(line)).matches()) {
                int worked;
                try {
                    worked = Integer.parseInt(matcher.group(1));
                } catch (Exception e) {
                    worked = (int) Float.parseFloat(matcher.group(1));
                }
                progressMonitor.worked(Math.min(worked, MAX_UNITS));
                progressMonitor.setSubTaskName(line);
                getLogger().info(line);
            } else if (error != null && (matcher = error.matcher(line)).matches()) {
                getLogger().severe(matcher.group(1));
            } else {
                getLogger().info(line);
                progressMonitor.setSubTaskName(line);
            }
        } catch (Exception ignored) {
        }
        processOutput.add(line);
    }

    @Override
    public List<String> getProcessOutput() {
        return processOutput;
    }

    private void initializeProgressMonitor() {
        if (progressMonitor == null) {
            progressMonitor = ProgressMonitor.NULL;
            progressMonitor.beginTask("Starting", MAX_UNITS);
        }
    }

    public void close() {
        if (progressMonitor != null) {
            progressMonitor.done();
        }
    }
}
