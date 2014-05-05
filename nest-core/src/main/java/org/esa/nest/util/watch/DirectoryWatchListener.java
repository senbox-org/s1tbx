package org.esa.nest.util.watch;

import java.io.File;


public interface DirectoryWatchListener {

    void filesAdded(File[] files);

    void filesRemoved(File[] files);
}
