package org.esa.snap.remote.execution.machines.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.core.gpf.graph.GraphException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Created by jcoravu on 16/1/2019.
 */
public class LinuxRemoteMachineExecutor extends AbstractRemoteMachineExecutor {

    public LinuxRemoteMachineExecutor(String masterSharedFolderURL, String sharedFolderUsername, String sharedFolderPassword,
                                      RemoteMachineProperties serverCredentials, RemoteMachinesGraphHelper remoteMachinesGraphHelper) {

        super(masterSharedFolderURL, sharedFolderUsername, sharedFolderPassword, serverCredentials, remoteMachinesGraphHelper);
    }

    @Override
    public String normalizeFileSeparator(String path) {
        return path.replace('\\', '/');
    }

    @Override
    public char getFileSeparatorChar() {
        return '/';
    }

    @Override
    protected boolean runGraph(String gptFilePath, String graphFilePathToProcess) throws IOException, JSchException {
        String command = "sh" + " " + gptFilePath + " " + graphFilePathToProcess;
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command.toString(), null, consoleBuffer);

        if (exitStatus == 0) {
            // the graph has been successfully executed on the remote machine
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, buildSuccessfullyExecutedGraphLogMessage(graphFilePathToProcess, command, consoleBuffer, exitStatus));
            }
            return true;
        }

        // failed to execute the graph on the remote machine
        logger.log(Level.SEVERE, buildFailedExecutedGraphLogMessage(graphFilePathToProcess, command, consoleBuffer, exitStatus));

        return false;
    }

    @Override
    protected boolean runCommands() throws IOException, GraphException, SftpException, JSchException {
        boolean mountLocalSharedFolder = false;

        String superUserPassword = this.remoteMachineCredentials.getPassword();
        String normalizedSharedFolderPath = normalizeFileSeparator(this.remoteMachineCredentials.getSharedFolderPath());

        // make the shared folder on the remote machine
        boolean sharedFolderCreated = makeSharedFolder(normalizedSharedFolderPath, superUserPassword);

        // change mode of the shared folder on the remote machine
        if (changeModeSharedFolder(normalizedSharedFolderPath, superUserPassword)) {
            // mount the shared folder
            if (mountSharedFolder(normalizedSharedFolderPath, superUserPassword)) {
                // the shared folder has been mounted
                mountLocalSharedFolder = true;

                if (canContinueRunning()) {
                    runGraphs();
                }

                // unmount the shared folder
                unmountSharedFolder(normalizedSharedFolderPath, superUserPassword);
            }
        }

        if (sharedFolderCreated) {
            // remove the shared folder
            deleteSharedFolder(normalizedSharedFolderPath, superUserPassword);
        }

        return mountLocalSharedFolder;
    }

    private boolean changeModeSharedFolder(String normalizedSharedFolderPath, String superUserPassword) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildLinuxChangeModeFolderCommand(normalizedSharedFolderPath);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command, superUserPassword, consoleBuffer);

        if (exitStatus == 0) {
            // the changed mode command has been successfully executed on the shared folder
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully change mode of the shared folder '")
                        .append(normalizedSharedFolderPath)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }
            return true;
        }

        // failed to change mode
        StringBuilder message = new StringBuilder();
        message.append("Failed to change mode of the shared folder '")
                .append(normalizedSharedFolderPath)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }

    private boolean mountSharedFolder(String normalizedSharedFolderPath, String superUserPassword) throws IOException, GraphException, SftpException, JSchException {
        String normalizedMasterSharedFolderURL = normalizeFileSeparator(this.masterSharedFolderPath);

        // mount the local shared folder
        String command = CommandExecutorUtils.buildLinuxMountSharedFolderCommand(normalizedMasterSharedFolderURL, normalizedSharedFolderPath,
                this.masterSharedFolderUsername, this.masterSharedFolderPassword);

        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command, superUserPassword, consoleBuffer);

        // do not write the password in the log file
        command = CommandExecutorUtils.buildLinuxMountSharedFolderCommand(normalizedMasterSharedFolderURL, normalizedSharedFolderPath, this.masterSharedFolderUsername, "...");

        if (exitStatus == 0) {
            // the remote machine shared folder has been mounted successfully
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully mount the shared folder '")
                        .append(normalizedSharedFolderPath)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }
            return true;
        }

        // failed to mount the shared folder
        StringBuilder message = new StringBuilder();
        message.append("Failed to mount the shared folder '")
                .append(normalizedSharedFolderPath)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }

    private boolean unmountSharedFolder(String normalizedSharedFolderPath, String superUserPassword) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildUnmountUnixSharedFolderCommand(normalizedSharedFolderPath);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command, superUserPassword, consoleBuffer);

        if (exitStatus == 0) {
            // the remote machine shared folder has been unmounted successfully
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully unmounted the shared folder '")
                        .append(normalizedSharedFolderPath)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }
            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Failed to unmount the shared folder '")
                .append(normalizedSharedFolderPath)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }

    private boolean makeSharedFolder(String normalizedSharedFolderPath, String superUserPassword) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildUnixMakeFolderCommand(normalizedSharedFolderPath);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command, superUserPassword, consoleBuffer);

        if (exitStatus == 0) {
            // the shared folder has been successfully created
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully created the shared folder '")
                        .append(normalizedSharedFolderPath)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }

            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Failed to create the shared folder '")
                .append(normalizedSharedFolderPath)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return checkMakeSharedFolderMessage(consoleBuffer);
    }

    private boolean deleteSharedFolder(String normalizedSharedFolderPath, String superUserPassword) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildUnixRemoveFolderCommand(normalizedSharedFolderPath);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeLinuxCommand(command, superUserPassword, consoleBuffer);

        if (exitStatus == 0) {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully deleted the shared folder '")
                        .append(normalizedSharedFolderPath)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }
            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Failed to delete the shared folder '")
                .append(normalizedSharedFolderPath)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }

    private static boolean checkMakeSharedFolderMessage(OutputConsole consoleBuffer) {
        String valuesToCheck[] = new String[] { "cannot ", "file exists" };
        if (consoleBuffer.containsIgnoreCaseOnNormalStream(valuesToCheck)) {
            return false;
        }
        return true;
    }
}
