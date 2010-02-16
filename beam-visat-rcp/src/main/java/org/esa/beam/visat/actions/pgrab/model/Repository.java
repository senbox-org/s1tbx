package org.esa.beam.visat.actions.pgrab.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.DataProvider;

public class Repository {

    public static final String REPOSITORY_CACHE_DIR_NAME = ".productGrabber-Cache";
    public static final String PROPERTIES_FILE_NAME = "repository.properties";
    private DataProvider[] _dataProviders;
    private PropertyMap _propertyMap;
    private File _storageDir;
    private File _baseDir;
    private List _entryList;
    private List _listenerList;

    public Repository(final File baseDir) {
        _baseDir = baseDir;
        _entryList = new ArrayList(32);
        _listenerList = new ArrayList(4);
    }

    public File getBaseDir() {
        return _baseDir;
    }

    public File getStorageDir() {
        if(_storageDir == null){
            _storageDir = createStorageDir();
        }
        return _storageDir;
    }

    /**
     * Retrieves the {@link PropertyMap} for this repository.
     *
     * @return the property map.
     */
    public PropertyMap getPropertyMap() {
        if(_propertyMap == null) {
            _propertyMap = loadPropertyMap();
        }
        return _propertyMap;
    }

    /**
     * Saves the property map.
     */
    public void savePropertyMap() {
        try {
            _propertyMap.store(getPropertiesFile(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(final RepositoryEntry entry) {
        if (entry != null && !_entryList.contains(entry)) {
            final boolean added = _entryList.add(entry);
            if (added) {
                entry.setOwner(this);
                final int index = _entryList.indexOf(entry);
                fireEntryAdded(entry, index);
            }
        }
    }

    public void removeEntry(final RepositoryEntry entry) {
        if (entry != null) {
            final int index = _entryList.indexOf(entry);
            if (_entryList.remove(entry)) {
                fireEntryRemoved(entry, index);
            }
        }
    }

    /**
     * Sets the <code>DataProviders</code> to this <code>Repository</code>.
     * A call to this method is only allowed once, other wise an {@link IllegalStateException} is thrown.
     */
    public void setDataProviders(final DataProvider[] providers) {
        if (_dataProviders != null) {
            throw new IllegalStateException("DataProviders are already set.");
        }
        _dataProviders = providers;
    }
    
    public DataProvider[] getDataProviders() {
        if (_dataProviders == null) {
            return new DataProvider[0];
        }
        return _dataProviders;
    }

    
    public void addListener(final RepositoryListener repositoryListener) {
        if (repositoryListener != null && !_listenerList.contains(repositoryListener)) {
            _listenerList.add(repositoryListener);
        }
    }

    public void removeListener(final RepositoryListener repositoryListener) {
        if (repositoryListener != null) {
            _listenerList.remove(repositoryListener);
        }
    }

    public RepositoryListener[] getListeners() {
        return (RepositoryListener[]) _listenerList.toArray(new RepositoryListener[0]);
    }

    public int getEntryCount() {
        return _entryList.size();
    }

    public RepositoryEntry getEntry(final int index) {
        return (RepositoryEntry) _entryList.get(index);
    }

    public int indexOf(final RepositoryEntry entry) {
        return _entryList.indexOf(entry);
    }

    @Override
    public String toString() {
        return _baseDir.getPath();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Repository) {
            final Repository repository = (Repository) obj;
            return this.getBaseDir().equals(repository.getBaseDir());
        }
        return false;
    }

    
    private File getPropertiesFile() {
        final File storageDir = getStorageDir();
        return new File(storageDir, PROPERTIES_FILE_NAME);
    }

    private PropertyMap loadPropertyMap() {
        final File propertiesFile = getPropertiesFile();
        if (!propertiesFile.exists()) {
            try {
                propertiesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final PropertyMap propertyMap = new PropertyMap();
        try {
            propertyMap.load(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return propertyMap;
    }

    private File createStorageDir() {
        final File defaultBeamCacheDir = SystemUtils.getDefaultBeamCacheDir();
        final File cacheDir = new File(defaultBeamCacheDir, REPOSITORY_CACHE_DIR_NAME);
        if(!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File storageDir = null;
        try {
            final URL fileUrl = FileUtils.getFileAsUrl(_baseDir);
            final String path = fileUrl.getPath();
            storageDir = new File(cacheDir, path.replace(':', '_'));
            storageDir.mkdirs();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return storageDir;
    }

    private void fireEntryAdded(final RepositoryEntry entry, final int index) {
        for (int i = 0; i < _listenerList.size(); i++) {
            final RepositoryListener repositoryListener = (RepositoryListener) _listenerList.get(i);
            repositoryListener.handleEntryAdded(entry, index);
        }
    }

    private void fireEntryRemoved(final RepositoryEntry entry, final int index) {
        for (int i = 0; i < _listenerList.size(); i++) {
            final RepositoryListener repositoryListener = (RepositoryListener) _listenerList.get(i);
            repositoryListener.handleEntryRemoved(entry, index);
        }
    }

    public static interface RepositoryListener {

        void handleEntryAdded(RepositoryEntry entry, int index);

        void handleEntryRemoved(RepositoryEntry entry, int index);
    }

}
