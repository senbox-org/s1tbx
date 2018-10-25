/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.gpf.operators.tooladapter;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class wrapping the execution of an external process.
 *
 * @author  Cosmin Cara
 * @since   5.0.4
 */
public class ProcessExecutor {
    private volatile boolean isStopped;
    private List<String>[] arguments;
    private File workingDirectory;
    private ProcessOutputConsumer consumer;
    private Logger logger = Logger.getLogger(ProcessExecutor.class.getName());

    public void setConsumer(ProcessOutputConsumer consumer) {
        this.consumer = consumer;
        if (consumer != null) {
            this.consumer.setLogger(this.logger);
        }
    }

    public void setWorkingDirectory(File directory) {
        this.workingDirectory = directory;
    }

    public int execute(List<String> arguments) throws IOException {
        return execute(arguments, System.getenv());
    }

    public int execute(List<String> arguments, Map<String, String> envVars) throws IOException {
        return execute(arguments, envVars, null);
    }

    public int execute(List<String> arguments, Map<String, String> envVars, File workingDirectory) throws IOException {
        Process process = null;
        BufferedReader outReader = null;
        int ret = -1;
        try {
            ProcessBuilder pb = new ProcessBuilder(arguments);
            //redirect the error of the tool to the standard output
            pb.redirectErrorStream(true);
            //set the working directory
            if (workingDirectory != null) {
                pb.directory(workingDirectory);
            }
            if (envVars != null) {
                pb.environment().putAll(envVars);
            }
            //start the process
            process = pb.start();
            //get the process output
            outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (!isStopped()) {
                while (!isStopped && outReader.ready()) {
                    //read the process output line by line
                    String line = outReader.readLine();
                    //consume the line if possible
                    if (line != null && !"".equals(line.trim())) {
                        this.consumer.consumeOutput(line);
                    }
                }
                // check if the project finished execution
                if (!process.isAlive()) {
                    //isStopped the loop
                    stop();
                } else {
                    //yield the control to other threads
                    Thread.yield();
                }
            }
            while (process.isAlive()) {
                Thread.yield();
            }
            ret = process.exitValue();
        } finally {
            if (process != null) {
                // if the process is still running, force it to isStopped
                if (process.isAlive()) {
                    //destroy the process
                    process.destroyForcibly();
                }
                try {
                    //wait for the project to end.
                    ret = process.waitFor();
                } catch (InterruptedException ignored) {
                }
                //close the reader
                closeStream(outReader);
                //close all streams
                closeStream(process.getErrorStream());
                closeStream(process.getInputStream());
                closeStream(process.getOutputStream());
            }
        }

        return ret;
    }

    /**
     * Command to isStopped the tool.
     */
    public void stop() {
        this.isStopped = true;
    }

    /**
     * Close any stream without triggering exceptions.
     *
     * @param stream input or output stream.
     */
    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }
    /**
     * Check if a isStopped command was issued.
     * <p>
     * This method is synchronized.
     * </p>
     *
     * @return true if the execution of the tool must be stopped.
     */
    private boolean isStopped() {
        return this.isStopped;
    }

}
