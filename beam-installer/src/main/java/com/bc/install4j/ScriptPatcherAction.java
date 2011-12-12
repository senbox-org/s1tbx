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

package com.bc.install4j;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.beans.ReplacementMode;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.ProgressInterface;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;

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

    @Override
    public boolean install(InstallerContext context) throws UserCanceledException {


        File installationDirectory = context.getInstallationDirectory();
        File scriptDir = new File(installationDirectory, scriptDirPath);

        File[] files = scriptDir.listFiles(new FileFilter() {
            @Override
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

    private static void patchFile(File file) throws IOException {

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

    @Override
    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return true;
    }
}
