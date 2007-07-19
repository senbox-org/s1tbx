package com.bc.install4j;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.ProgressInterface;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.api.beans.ReplacementMode;

import java.io.*;

/**
 * A sample action that can be also used for the uninstaller.
 * <p/>
 * The class com.bc.install4j.ScriptPatcherAction in the same package defines how the bean
 * is handled in install4j's GUI.
 */
public class ScriptPatcherAction extends AbstractInstallOrUninstallAction {

    private String scriptDirPath = "bin";
    private boolean writingLog = false;
    private String logFilename = "install.log";
    private File scriptDir;

    public ScriptPatcherAction() {
    }

    public String getScriptDirPath() {
        return replaceVariables(scriptDirPath);
    }

    public void setScriptDirPath(String scriptDirPath) {
        this.scriptDirPath = scriptDirPath;
    }

    public String getLogFilename() {
        return replaceVariables(logFilename);
    }

    public void setLogFilename(String logFilename) {
        this.logFilename = logFilename;
    }

    public boolean isWritingLog() {
        return writingLog;
    }

    public void setWritingLog(boolean writingLog) {
        this.writingLog = writingLog;
    }

    public boolean install(InstallerContext context) throws UserCanceledException {


        File installationDirectory = context.getInstallationDirectory();
        scriptDir = new File(installationDirectory, scriptDirPath);

        PrintWriter logWriter = getLogWriter(context);

        File[] files = scriptDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && (
                        file.getName().endsWith(".bat")
                        || file.getName().endsWith(".cmd")
                        || file.getName().endsWith(".sh")
                        || file.getName().endsWith(".command"));
            }
        });
        if (files != null) {
            ProgressInterface progressInterface = context.getProgressInterface();
            progressInterface.setStatusMessage("Patching command line scripts");
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                try {
                    patchFile(context, file);
                    logWriter.println(file + " patched");
                } catch (IOException e) {
                    logWriter.println(file + " patching failed: " + e.getMessage());
                }
                progressInterface.setPercentCompleted(((i + 1) * 100) / files.length);
            }
            progressInterface.setStatusMessage("");
        }
        logWriter.close();
        return true;
    }

    private void patchFile(InstallerContext context, File file) throws IOException {

        long l = file.length();
        if (l >= Integer.MAX_VALUE) {
            return;
        }

        int length = (int) l;
        byte[] buffer = new byte[length];

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        try {
            randomAccessFile.readFully(buffer);
            String patchedContent = replaceVariables(new String(buffer), ReplacementMode.PLAIN);
            randomAccessFile.setLength(0);
            randomAccessFile.writeBytes(patchedContent);
        } finally {
            randomAccessFile.close();
        }
    }


    private PrintWriter getLogWriter(InstallerContext context) throws UserCanceledException {
        PrintWriter logWriter = null;
        if (writingLog && logFilename.length() > 0) {
            try {
                File file = new File(scriptDir, logFilename);
                logWriter = new PrintWriter(new FileWriter(file));
                context.registerUninstallFile(file);
            } catch (IOException e) {
                // ok
            }
        }
        if (logWriter == null) {
            logWriter = new PrintWriter(new OutputStreamWriter(System.out));
        }
        return logWriter;
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return true;
    }

    @Override
    public void rollback(InstallerContext installerContext) {
        super.rollback(installerContext);
    }
}
