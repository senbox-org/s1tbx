package org.esa.nest.util.watch;

import java.io.File;

public interface FileWatchListener {

    void filesChanged(File[] filesChanged);
}
