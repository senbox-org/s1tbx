package org.esa.snap.remote.execution.file.system;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.executors.ProcessExecutor;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class LinuxLocalMachineFileSystem extends UnixLocalMachineFileSystem {

    private static final Logger logger = Logger.getLogger(LinuxLocalMachineFileSystem.class.getName());

    public LinuxLocalMachineFileSystem() {
        super();
    }

    @Override
    public String findPhysicalSharedFolderPath(String shareNameToFind, String localPassword) throws IOException {
        String sambaUserSharesFolderPath = "/var/lib/samba/usershares";
        Map<String, List<String>> linesGroupBySectionName = readSambaConfigurationFile(localPassword);
        if (linesGroupBySectionName != null) {
            // the samba configuration file content has been successfully read
            String globalSectionName = "[global]";
            String[] reservedSectionNames = new String[] {globalSectionName, "[homes]", "[netlogon]", "[profiles]", "[printers]", "[print$]"};
            String shareNameToFindAsSection = "[" + shareNameToFind + "]";
            boolean isReservedSection = false;
            for (int k = 0; k < reservedSectionNames.length && !isReservedSection; k++) {
                if (reservedSectionNames[k].equals(shareNameToFindAsSection)) {
                    isReservedSection = true;
                }
            }
            if (!isReservedSection) {
                List<String> sectionLines = linesGroupBySectionName.get(shareNameToFindAsSection);
                if (sectionLines != null) {
                    // the share name exists in the sambaa configuration file 'smb.config'
                    String pathValue = extractPropertyValueFromSectionLines(sectionLines, "path");
                    if (pathValue != null) {
                        return pathValue;
                    }
                }
            }

            List<String> globalSsectionLines = linesGroupBySectionName.get(globalSectionName);
            if (globalSsectionLines != null) {
                // the share name exists in the sambaa configuration file 'smb.config'
                String userSharePathValue = extractPropertyValueFromSectionLines(globalSsectionLines, "usershare path");
                if (userSharePathValue != null) {
                    sambaUserSharesFolderPath = userSharePathValue;
                }
            }
        }

        String command = "ls -F " + sambaUserSharesFolderPath;
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);
        if (exitStatus == 0) {
            String[] filesAndFolders = outputConsole.getNormalStreamMessages().split(OutputConsole.MESSAGE_SEPARATOR);
            for (int i = 0; i < filesAndFolders.length; i++) {
                if (!filesAndFolders[i].endsWith("/")) {
                    String physicalSharedFolderPath = checkPhysicalSharedFolderPathInFile(shareNameToFind, localPassword, sambaUserSharesFolderPath, filesAndFolders[i]);
                    if (physicalSharedFolderPath != null) {
                        return physicalSharedFolderPath;
                    }
                }
            }
        } else {
            // failed to get the files and folders
            logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage("Failed to get the files and folders from the folder '"+sambaUserSharesFolderPath+"'.", command, outputConsole, exitStatus));
        }


        return null;
    }

    private static String extractPropertyValueFromSectionLines(List<String> sectionLines, String propertyKey) {
        for (int k=0; k<sectionLines.size(); k++) {
            String line = sectionLines.get(k);
            if (line.startsWith(propertyKey)) {
                return extractPropertyValueFromLine(line, propertyKey);
            }
        }
        return null;
    }


    private static Map<String, List<String>> readSambaConfigurationFile(String localPassword) throws IOException {
        String sambaConfigurationFilePath = "/etc/samba/smb.conf";

        Map<String, List<String>> linesGroupBySectionName = null;
        String command = "cat " + sambaConfigurationFilePath;
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);
        if (exitStatus == 0) {
            String[] fileLines = outputConsole.getNormalStreamMessages().split(OutputConsole.MESSAGE_SEPARATOR);
            linesGroupBySectionName = new HashMap<String, List<String>>();
            List<String> currentSectionLines = null;
            for (int i=0; i<fileLines.length; i++) {
                String line = fileLines[i].trim();
                if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
                    // ignore the line
                } else if (line.startsWith("[") && line.endsWith("]")) {
                    currentSectionLines = new ArrayList<String>();
                    linesGroupBySectionName.put(line, currentSectionLines);
                } else {
                    currentSectionLines.add(line);
                }
            }
        } else {
            // failed to get the file content of the samba configuration file
            logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage("Failed to get the content of the samba configuration file '"+sambaConfigurationFilePath+"'.", command, outputConsole, exitStatus));
        }
        return linesGroupBySectionName;
    }

    private static String checkPhysicalSharedFolderPathInFile(String shareNameToFind, String localPassword,
                                                              String sambaUserSharesFolderPath, String fileNameToCheck)
                                                              throws IOException {

        String filePath = sambaUserSharesFolderPath + "/" + fileNameToCheck;
        String command = "cat " + filePath;
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);
        if (exitStatus == 0) {
            String[] fileLines = outputConsole.getNormalStreamMessages().split(OutputConsole.MESSAGE_SEPARATOR);
            String pathLine = null;
            String shareNameLine = null;

            String pathKey = "path";
            String pathKeyEqual = pathKey + "=";
            String pathKeySpace = pathKey + " ";

            String shareNameKey = "sharename";
            String shareNameKeyEqual = shareNameKey + "=";
            String shareNameKeySpace = shareNameKey + " ";

            for (int i=0; i<fileLines.length; i++) {
                String line = fileLines[i].trim();
                if (line.startsWith(pathKeyEqual) || line.startsWith(pathKeySpace)) {
                    pathLine = line;
                } else if (line.startsWith(shareNameKeyEqual) || line.startsWith(shareNameKeySpace)) {
                    shareNameLine = line;
                }
                if (shareNameLine != null && pathLine != null) {
                    String shareNameValue = extractPropertyValueFromLine(shareNameLine, shareNameKey);
                    if (shareNameValue.equals(shareNameToFind)) {
                        return extractPropertyValueFromLine(pathLine, pathKey);
                    }
                    break;
                }
            }
        } else {
            // failed to get the file content
            logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage("Failed to get the content of the file '"+filePath+"'.", command, outputConsole, exitStatus));
        }

        return null;
    }

    private static String extractPropertyValueFromLine(String fileContentLine, String propertyKey) {
        int equalIndex = fileContentLine.indexOf('=', propertyKey.length());
        if (equalIndex < propertyKey.length()) {
            throw new IllegalArgumentException("The line '"+ fileContentLine+"' does not contain '=' after the property key '" + propertyKey + "'.");
        }
        return fileContentLine.substring(equalIndex + 1).trim();
    }
}
