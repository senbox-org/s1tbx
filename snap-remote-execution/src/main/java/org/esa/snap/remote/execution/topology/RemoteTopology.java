package org.esa.snap.remote.execution.topology;

import org.esa.snap.remote.execution.machines.RemoteMachineProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcoravu on 8/1/2019.
 */
public class RemoteTopology {

    private final String remoteSharedFolderURL;
    private final String remoteUsername;
    private final String remotePassword;
    private final List<RemoteMachineProperties> remoteMachines;

    private String localSharedFolderPath;
    private String localPassword;

    public RemoteTopology(String remoteSharedFolderURL, String remoteUsername, String remotePassword) {
        this.remoteSharedFolderURL = remoteSharedFolderURL;
        this.remoteUsername = remoteUsername;
        this.remotePassword = remotePassword;
        this.remoteMachines = new ArrayList<RemoteMachineProperties>();
    }

    public void setLocalMachineData(String localSharedFolderPath, String localSharedFolderPassword) {
        this.localSharedFolderPath = localSharedFolderPath;
        this.localPassword = localSharedFolderPassword;
    }

    public void addRemoteMachine(RemoteMachineProperties remoteMatchineToAdd) {
        this.remoteMachines.add(remoteMatchineToAdd);
    }

    public List<RemoteMachineProperties> getRemoteMachines() {
        return remoteMachines;
    }

    public String getRemotePassword() {
        return remotePassword;
    }

    public String getRemoteSharedFolderURL() {
        return remoteSharedFolderURL;
    }

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public String getLocalSharedFolderPath() {
        return localSharedFolderPath;
    }

    public String getLocalPassword() {
        return localPassword;
    }
}
