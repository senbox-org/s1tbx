package org.esa.nest.util.watch;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;


public class FileWatch {

    public FileWatch() {
        fileList = new ArrayList();
        listeners = new ArrayList();
    }

    synchronized public int getNumFiles() {
        return fileList.size();
    }

    synchronized public void add(File file) {
        if (file != null) {
            if (!file.isFile()) {
                throw new IllegalArgumentException("The argument: " + file.getPath() + " is not a file");
            }
            final int index = getFileIndex(file);
            if (index < 0) {
                FileTimeContainer container = new FileTimeContainer();
                container.setFile(file);
                container.setLastModified(file.lastModified());
                fileList.add(container);
            }
        }
    }

    synchronized public void remove(File file) {
        final int index = getFileIndex(file);
        if (index >= 0) {
            fileList.remove(index);
        }
    }

    synchronized public int getNumListeners() {
        return listeners.size();
    }

    synchronized public void addListener(FileWatchListener listener) {
        if ((listener != null) && (!listeners.contains(listener))) {
            listeners.add(listener);
        }
    }

    synchronized public void removeListener(FileWatchListener listener) {
        listeners.remove(listener);
    }

    public void start(long rate) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new FileWatchTask(), 0, rate);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private final ArrayList fileList;
    private final ArrayList listeners;
    private Timer timer;

    protected void checkForModifiedFiles() {
        File[] changedFiles = getChangedFiles();

        if (changedFiles.length > 0) {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext(); ) {
                FileWatchListener fileWatchListener = (FileWatchListener) iterator.next();
                fileWatchListener.filesChanged(changedFiles);
            }
        }
    }

    protected File[] getChangedFiles() {
        ArrayList changedFiles = new ArrayList();

        for (Iterator iterator = fileList.iterator(); iterator.hasNext(); ) {
            final FileTimeContainer container = (FileTimeContainer) iterator.next();
            final File file = container.getFile();
            final long lastModified = file.lastModified();
            if (container.getLastModified() != lastModified) {
                container.setLastModified(lastModified);
                changedFiles.add(file);
            }
        }
        File[] fileArray = new File[changedFiles.size()];
        return (File[]) changedFiles.toArray(fileArray);
    }

    private int getFileIndex(File file) {
        final FileTimeContainer container = new FileTimeContainer();
        container.setFile(file);
        return fileList.indexOf(container);
    }


    private class FileTimeContainer {
        private long lastModified;
        private File file;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        /**
         * @noinspection InstanceofInterfaces
         */
        public boolean equals(Object obj) {
            if (obj instanceof FileTimeContainer) {
                FileTimeContainer other = (FileTimeContainer) obj;
                return other.getFile().equals(file);
            }
            return false;
        }
    }

    private class FileWatchTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            checkForModifiedFiles();
        }
    }
}
