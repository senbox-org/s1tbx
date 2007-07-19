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

    public ScriptPatcherAction() {
    }

    public String getScriptDirPath() {
        return replaceVariables(scriptDirPath);
    }

    public void setScriptDirPath(String scriptDirPath) {
        this.scriptDirPath = scriptDirPath;
    }

    public boolean install(InstallerContext context) throws UserCanceledException {


        File installationDirectory = context.getInstallationDirectory();
        File scriptDir = new File(installationDirectory, scriptDirPath);

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
                    patchFile(file);
                } catch (IOException e) {
                    System.err.println(file + " patching failed: " + e.getMessage());
                }
                progressInterface.setPercentCompleted(((i + 1) * 100) / files.length);
            }
            progressInterface.setStatusMessage("");
        }
        return true;
    }

    private void patchFile(File file) throws IOException {

        long l = file.length();
        if (l >= Integer.MAX_VALUE) {
            System.err.println(file + " too big");
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

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return true;
    }
}
