package org.esa.snap.remote.execution.executors;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by jcoravu on 21/1/2019.
 */
public class ProcessExecutor {

    public static int executeWindowsCommand(String command, File workingDirectory, OutputConsole outputConsole) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
        if (workingDirectory != null) {
            pb.directory(workingDirectory);
        }
        // redirect the error of the tool to the standard output
        pb.redirectErrorStream(true);
        pb.environment().putAll(System.getenv());

        // start the process
        Process process = pb.start();
        try {
            return readStreamResults(process, null, outputConsole);
        } finally {
            resetProcess(process);
        }
    }

    public static int executeUnixCommand(String command, String superUserPassword, File workingDirectory, OutputConsole outputConsole) throws IOException {
        boolean asSuperUser = (superUserPassword != null);
        if (asSuperUser) {
            command = "sudo -S -p '' " + command;
        }
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        if (workingDirectory != null) {
            pb.directory(workingDirectory);
        }
        // redirect the error of the tool to the standard output
        pb.redirectErrorStream(true);
        pb.environment().putAll(System.getenv());

        // start the process
        Process process = pb.start();
        try {
            if (asSuperUser) {
                OutputStream outputStream = process.getOutputStream();
                outputStream.write((superUserPassword + "\n").getBytes());
                outputStream.flush();
            }
            return readStreamResults(process, superUserPassword, outputConsole);
        } finally {
            resetProcess(process);
        }
    }

    private static int readStreamResults(Process process, String superUserPassword, OutputConsole outputConsole) throws IOException {
        InputStream inputStream = process.getInputStream(); // get the process output
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            try {
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                try {
                    String line;
                    while (true) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!"".equals(line.trim())) {
                                if (superUserPassword == null) {
                                    outputConsole.appendNormalMessage(line);
                                } else {
                                    if (!superUserPassword.equals(line)) {
                                        outputConsole.appendNormalMessage(line);
                                    }
                                }
                            }
                        }
                        if (process.isAlive()) {
                            Thread.yield(); // yield the control to other threads
                        } else {
                            break;
                        }
                    }
                } finally {
                    closeStream(bufferedReader);
                }
            } finally {
                closeStream(inputStreamReader);
            }
        } finally {
            closeStream(inputStream);
        }
        return process.exitValue();
    }

    private static void resetProcess(Process process) {
        if (process != null) {
            // if the process is still running, force it to isStopped
            if (process.isAlive()) {
                //destroy the process
                process.destroyForcibly();
            }
            try {
                //wait for the project to end.
                process.waitFor();
            } catch (InterruptedException ignored) {
            }
            //close all streams
            closeStream(process.getErrorStream());
            closeStream(process.getInputStream());
            closeStream(process.getOutputStream());
        }
    }

    static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // nothing to do
            }
        }
    }
}
