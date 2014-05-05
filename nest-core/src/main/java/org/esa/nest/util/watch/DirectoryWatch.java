package org.esa.nest.util.watch;

import org.esa.beam.util.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class DirectoryWatch {

    private final ArrayList<File> directoryList = new ArrayList<File>();
    private final ArrayList<FileContainer> watchedFiles = new ArrayList<FileContainer>();
    private final ArrayList directoriesContent = new ArrayList();
    private final ArrayList<DirectoryWatchListener> listeners = new ArrayList<DirectoryWatchListener>();
    private final ArrayList<File> addedFileList = new ArrayList<File>();
    private final ArrayList<File> removedFileList = new ArrayList<File>();

    private Timer timer;

    public DirectoryWatch() {
    }

    synchronized public int getNumDirectories() {
        return directoryList.size();
    }

    synchronized public void add(File dir) {
        if (dir != null) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("The argument: " + dir.getPath() + " is not a directory");
            }
            if (getDirectoryIndex(dir) < 0) {
                directoryList.add(dir);
            }
        }
    }

    synchronized public void remove(File dir) {
        final int index = getDirectoryIndex(dir);
        if (index >= 0) {
            directoryList.remove(index);

            ArrayList toDelete = new ArrayList();
            for (Object aDirectoriesContent : directoriesContent) {
                File file = (File) aDirectoriesContent;
                if (dir.equals(file.getParentFile())) {
                    toDelete.add(file);
                }
            }

            for (Object aToDelete : toDelete) {
                File file = (File) aToDelete;
                directoriesContent.remove(file);
            }
        }
    }

    synchronized public void removeAll() {
        while (!directoryList.isEmpty()) {
            remove(directoryList.get(0));
        }
    }

    synchronized public int getNumListeners() {
        return listeners.size();
    }

    synchronized public void addListener(DirectoryWatchListener listener) {
        if ((listener != null) && (!listeners.contains(listener))) {
            listeners.add(listener);
        }
    }

    synchronized public void removeListener(DirectoryWatchListener listener) {
        listeners.remove(listener);
    }

    synchronized public File[] getDirectoriesContent() {
        File result[] = new File[directoriesContent.size()];
        return (File[]) directoriesContent.toArray(result);
    }

    public void start(long rate) {
        //System.out.println("DirWatch started");
        timer = new Timer();
        timer.scheduleAtFixedRate(new DirectoryWatchTask(), 0, rate);
    }

    public void stop() {
        //System.out.println("DirWatch stopped");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    synchronized File[] getWatchedFiles() {
        File[] result = new File[watchedFiles.size()];
        int idx = 0;
        for (Object watchedFile : watchedFiles) {
            FileContainer container = (FileContainer) watchedFile;
            result[idx] = container.getFile();
            ++idx;
        }
        return result;
    }

    synchronized void checkDirectories() {
        addNewFilesToWatch();
        checkForFileChanges();
        checkForRemovedFiles();
        notifyListeners();
    }

    private int getDirectoryIndex(File dir) {
        return directoryList.indexOf(dir);
    }

    private void checkForRemovedFiles() {
        removedFileList.clear();
        for (Object aDirectoriesContent : directoriesContent) {
            File file = (File) aDirectoriesContent;
            if (!file.exists()) {
                removedFileList.add(file);
            }
        }

        for (Object aRemovedFileList : removedFileList) {
            File file = (File) aRemovedFileList;
            directoriesContent.remove(file);
        }
    }

    private void checkForFileChanges() {
        final ArrayList stableFiles = new ArrayList();

        addedFileList.clear();
        for (Object watchedFile : watchedFiles) {
            final FileContainer container = (FileContainer) watchedFile;
            final File file = container.getFile();
            final long length = file.length();
            final long lastModified = file.lastModified();

            if ((container.getLastModified() == lastModified)
                    && (container.getSize() == length)) {
                container.setStableCount(1 + container.getStableCount());
                if (container.getStableCount() > 2) {
                    stableFiles.add(file);
                }
            } else {
                container.setLastModified(lastModified);
                container.setSize(length);
                container.setStableCount(0);
            }
        }

        FileContainer comparer = new FileContainer();
        for (Object stableFile : stableFiles) {
            final File file = (File) stableFile;

            addedFileList.add(file);
            directoriesContent.add(file);
            Debug.trace(file.getName() + " added to dirContents");
            comparer.setFile(file);
            watchedFiles.remove(comparer);
        }
    }

    private void notifyListeners() {
        if (addedFileList.size() > 0) {
            File added[] = new File[addedFileList.size()];
            added = addedFileList.toArray(added);

            for (Object listener1 : listeners) {
                DirectoryWatchListener listener = (DirectoryWatchListener) listener1;
                listener.filesAdded(added);
            }
            addedFileList.clear();
        }

        if (removedFileList.size() > 0) {
            File removed[] = new File[removedFileList.size()];
            removed = removedFileList.toArray(removed);

            for (Object listener1 : listeners) {
                DirectoryWatchListener listener = (DirectoryWatchListener) listener1;
                listener.filesRemoved(removed);
            }

            removedFileList.clear();
        }
    }

    private void addNewFilesToWatch() {
        final FileContainer comparer = new FileContainer();

        for (Object aDirectoryList : directoryList) {
            File dir = (File) aDirectoryList;
            File fileArray[] = dir.listFiles();
            if (fileArray == null)
                continue;
            for (File file : fileArray) {
                if (!directoriesContent.contains(file)) {
                    comparer.setFile(file);
                    if (!watchedFiles.contains(comparer)) {
                        final FileContainer container = new FileContainer();
                        container.setFile(file);
                        container.setLastModified(file.lastModified());
                        container.setSize(file.length());
                        container.setStableCount(0);
                        watchedFiles.add(container);
                    }
                }
            }
        }
    }

    private class FileContainer {
        private File file;
        private long lastModified;
        private long size;
        private int stableCount;

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

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public int getStableCount() {
            return stableCount;
        }

        public void setStableCount(int stableCount) {
            this.stableCount = stableCount;
        }

        /**
         * @noinspection instanceof Interfaces
         */
        public boolean equals(Object obj) {
            boolean result = false;

            if (obj instanceof FileContainer) {
                result = this.file.equals(((FileContainer) obj).getFile());
            }

            return result;
        }
    }

    private class DirectoryWatchTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            checkDirectories();
        }
    }
}
