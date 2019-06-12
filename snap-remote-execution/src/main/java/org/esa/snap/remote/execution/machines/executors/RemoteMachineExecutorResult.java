package org.esa.snap.remote.execution.machines.executors;

/**
 * Created by jcoravu on 23/5/2019.
 */
public enum RemoteMachineExecutorResult {
    CONNECTED_AND_MOUNT_LOCAL_SHARED_FOLDER,
    FAILED_SSH_COONECTION,
    CONNECTED_AND_FAILED_MOUNT_LOCAL_SHARED_FOLDER,
    FAILED_EXECUTION,
    CONNECTED_AND_STOP_TO_CONTINUE,
    STOP_TO_CONTINUE,
}
