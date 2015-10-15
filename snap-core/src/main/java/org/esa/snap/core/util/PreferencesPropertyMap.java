package org.esa.snap.core.util;

import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Implementation of the {@link PropertyMap} interface backed by a {@link Preferences} instance.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class PreferencesPropertyMap extends AbstractPropertyMap implements PreferenceChangeListener {
    private final Preferences preferences;
    private PropertyChangeSupport propertyChangeSupport;

    public PreferencesPropertyMap(Preferences preferences) {
        Assert.notNull(preferences, "preferences");
        this.preferences = preferences;
    }

    @Override
    protected String get(String key) {
        return preferences.get(key, null);
    }

    @Override
    protected String get(String key, String defaultValue) {
        return preferences.get(key, defaultValue);
    }

    @Override
    protected String set(String key, String value) {
        String oldValue = preferences.get(key, null);
        if (value != null) {
            preferences.put(key, value);
        } else {
            preferences.remove(key);
        }
        return oldValue;
    }

    @Override
    protected void firePropertyChange(String key, String oldValue, String newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(key, oldValue, newValue);
        }
    }

    @Override
    public void load(Path file) throws IOException {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        for (String key : properties.stringPropertyNames()) {
            preferences.put(key, properties.getProperty(key));
        }
    }

    @Override
    public void store(Path file, String header) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            getProperties().store(writer, header);
        }
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        try {
            for (String propertyKey : preferences.keys()) {
                properties.put(propertyKey, preferences.get(propertyKey, null));
            }
        } catch (BackingStoreException e) {
            // ok
        }
        return properties;
    }

    @Override
    public Set<String> getPropertyKeys() {
        try {
            return new HashSet<>(Arrays.asList(preferences.keys()));
        } catch (BackingStoreException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        maybeAddPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String key, PropertyChangeListener listener) {
        maybeAddPropertyChangeSupport().addPropertyChangeListener(key, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
            maybeRemovePropertyChangeSupport();
        }
    }

    @Override
    public void removePropertyChangeListener(String key, PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(key, listener);
            maybeRemovePropertyChangeSupport();
        }
    }

    /**
     * This method gets called when a preference is added, removed or when
     * its value is changed.
     * <p>
     *
     * @param evt A PreferenceChangeEvent object describing the event source
     *            and the preference that has changed.
     */
    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        firePropertyChange(evt.getKey(), null, evt.getNewValue());
    }

    private synchronized PropertyChangeSupport maybeAddPropertyChangeSupport() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
            preferences.addPreferenceChangeListener(this);
        }
        return propertyChangeSupport;
    }

    private synchronized void maybeRemovePropertyChangeSupport() {
        if (!propertyChangeSupport.hasListeners(null)) {
            propertyChangeSupport = null;
            preferences.removePreferenceChangeListener(this);
        }
    }

}
