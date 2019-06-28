package org.esa.snap.remote.execution.machines.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.file.system.IRemoteMachineFileSystem;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.executors.SSHConnection;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.core.gpf.graph.GraphException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 22/1/2019.
 */
public abstract class AbstractRemoteMachineExecutor implements IRemoteMachineFileSystem {

    protected static final Logger logger = Logger.getLogger(AbstractRemoteMachineExecutor.class.getName());

    protected final String masterSharedFolderPath;
    protected final String masterSharedFolderUsername;
    protected final String masterSharedFolderPassword;
    protected final RemoteMachineProperties remoteMachineCredentials;
    protected final RemoteMachinesGraphHelper remoteMachinesGraphHelper;
    protected final SSHConnection sshConnection;

    private final List<String> createdOutputProductsRelativeFilePath;

    protected AbstractRemoteMachineExecutor(String masterSharedFolderPath, String masterSharedFolderUsername, String masterSharedFolderPassword,
                                            RemoteMachineProperties remoteMachineCredentials, RemoteMachinesGraphHelper remoteMachinesGraphHelper) {

        this.masterSharedFolderPath = masterSharedFolderPath;
        this.masterSharedFolderUsername = masterSharedFolderUsername;
        this.masterSharedFolderPassword = masterSharedFolderPassword;
        this.remoteMachineCredentials = remoteMachineCredentials;
        this.remoteMachinesGraphHelper = remoteMachinesGraphHelper;

        this.createdOutputProductsRelativeFilePath = new ArrayList<String>();
        this.sshConnection = new SSHConnection(this.remoteMachineCredentials.getHostName(), this.remoteMachineCredentials.getPortNumber(),
                                               this.remoteMachineCredentials.getUsername(), this.remoteMachineCredentials.getPassword());
    }

    protected abstract boolean runGraph(String gptFilePath, String graphFilePathToProcess) throws IOException, JSchException;

    protected abstract boolean runCommands() throws IOException, GraphException, SftpException, JSchException;

    public final void stopRunningCommand() throws Exception {
        this.sshConnection.stopRunningCommand();
    }

    public final RemoteMachineExecutorResult doExecute() throws JSchException, SftpException, IOException, GraphException {
        RemoteMachineExecutorResult result;
        if (canContinueRunning()) {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Execute the graphs on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, message.toString());
            }

            try {
                this.sshConnection.connect();
                // the remote machine is available
                try {
                    if (canContinueRunning()) {
                        boolean mountLocalSharedFolder = runCommands();
                        if (mountLocalSharedFolder) {
                            result = RemoteMachineExecutorResult.CONNECTED_AND_MOUNT_LOCAL_SHARED_FOLDER;
                        } else {
                            result = RemoteMachineExecutorResult.CONNECTED_AND_FAILED_MOUNT_LOCAL_SHARED_FOLDER;
                        }
                    } else {
                        result = RemoteMachineExecutorResult.CONNECTED_AND_STOP_TO_CONTINUE;
                    }
                } finally {
                    this.sshConnection.disconnect();
                }
            } catch (JSchException exception) {
                if (exception.getCause() instanceof java.net.UnknownHostException || exception.getCause() instanceof java.net.ConnectException) {
                    result = RemoteMachineExecutorResult.FAILED_SSH_COONECTION;
                    StringBuilder message = new StringBuilder();
                    message.append("Failed to connect to the remote machine '")
                            .append(this.remoteMachineCredentials.getHostName())
                            .append("'.");
                    logger.log(Level.SEVERE, message.toString(), exception);
                    setExceptionOccurredOnRemoteMachine(exception);
                } else {
                    throw exception;
                }
            } finally {
                this.sshConnection.disconnect(); // invoke again the disconnect method
            }
        } else {
            result = RemoteMachineExecutorResult.STOP_TO_CONTINUE;
        }
        return result;
    }

    public final void setExceptionOccurredOnRemoteMachine(Exception exception) {
        this.remoteMachinesGraphHelper.setExceptionOccurredOnRemoteMachine(exception);
    }

    protected final boolean canContinueRunning() {
        return this.remoteMachinesGraphHelper.canContinueIfExceptionOccurredOnRemoteMachines() && !this.sshConnection.isStopped();
    }

    protected final void runGraphs() throws IOException, GraphException, SftpException, JSchException {
        boolean canRunGraphs = true;
        while (canRunGraphs && canContinueRunning()) {
            RemoteMachineExecutorInputData remoteMachineMetadata = this.remoteMachinesGraphHelper.computeNextGraphToRun(this.remoteMachineCredentials, this);
            if (remoteMachineMetadata == null) {
                canRunGraphs = false;
            } else {
                boolean outputProductCreated = false;
                try {
                    String relativeFilePath = normalizeFileSeparator(remoteMachineMetadata.getGraphRelativeFilePath());
                    if (relativeFilePath.charAt(0) != getFileSeparatorChar()) {
                        relativeFilePath = getFileSeparatorChar() + relativeFilePath;
                    }
                    String graphFilePathToProcess = "\"" + normalizeFileSeparator(this.remoteMachineCredentials.getSharedFolderPath() + relativeFilePath) + "\"";

                    String gptFilePath = this.remoteMachineCredentials.getGPTFilePath();
                    if (StringUtils.isBlank(gptFilePath)) {
                        gptFilePath = "gpt";
                    } else {
                        gptFilePath = normalizeFileSeparator(gptFilePath);
                        if (!gptFilePath.startsWith("\"") && !gptFilePath.endsWith("\"")) {
                            gptFilePath = "\"" + gptFilePath + "\"";
                        }
                    }

                    boolean success = runGraph(gptFilePath, graphFilePathToProcess);
                    if (success) {
                        // the graph has not been successfully executed
                        Path localSharedFolder = this.remoteMachinesGraphHelper.getLocalOutputFolder().getParent();
                        String relativePath = this.remoteMachinesGraphHelper.getLocalMachineFileSystem().normalizeFileSeparator(remoteMachineMetadata.getOutputProductRelativeFilePath());
                        Path localOutputProductFilePath = localSharedFolder.resolve(relativePath);
                        if (Files.exists(localOutputProductFilePath)) {
                            outputProductCreated = true;
                        }
                    }
                } finally {
                    if (outputProductCreated) {
                        this.createdOutputProductsRelativeFilePath.add(remoteMachineMetadata.getOutputProductRelativeFilePath());
                    } else {
                        this.remoteMachinesGraphHelper.addUnprocessedGraphSourceProducts(this.remoteMachineCredentials, remoteMachineMetadata);
                    }
                }
            }
        }
    }

    public List<String> getCreatedOutputProductsRelativeFilePath() {
        return createdOutputProductsRelativeFilePath;
    }

    protected final String buildSuccessfullyExecutedGraphLogMessage(String graphFilePathToProcess, String command, OutputConsole consoleBuffer, int exitStatus) {
        StringBuilder message = new StringBuilder();
        message.append("Successfully executed the graph '")
                .append(graphFilePathToProcess)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        return CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus);
    }

    protected final String buildFailedExecutedGraphLogMessage(String graphFilePathToProcess, String command, OutputConsole consoleBuffer, int exitStatus) {
        StringBuilder message = new StringBuilder();
        message.append("Failed to execute the graph '")
                .append(graphFilePathToProcess)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        return CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus);
    }

    public final String buildFailedLogMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Failed to execute the graph on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        return message.toString();
    }
}
