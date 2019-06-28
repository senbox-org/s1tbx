package org.esa.snap.remote.execution.file.system;

/**
 * Created by jcoravu on 22/2/2019.
 */
public interface IRemoteMachineFileSystem {

    public String normalizeFileSeparator(String path);

    public char getFileSeparatorChar();
}
