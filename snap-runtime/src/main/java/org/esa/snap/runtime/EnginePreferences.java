package org.esa.snap.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Utility class used to manage Engine configurations in a uniform way. Preference values containing placeholders of
 * the form {@code ${variable}} are resolved before they are returned by this class.
 * Resolution of {@code variable} is done in this order:
 * <ol>
 * <li>The value of key {@code variable} in this preferences instance</li>
 * <li>The value of key {@code variable} in the parent of this preferences instance</li>
 * <li>The value of a system property named {@code variable}</li>
 * <li>The value of the environment variable named {@code variable}</li>
 * </ol>
 * The resolution of single values is continued until no further replacements can be applied.
 *
 * @author Norman Fomferra
 * @see Launcher
 * @see Engine
 * @since SNAP 2.0
 */
class EnginePreferences extends AbstractPreferences {

    private static EnginePreferences root = new EnginePreferences(null, "");

    private Properties properties;
    private final String keyPrefix;
    private Path backingStorePath;

    EnginePreferences(String name) {
        this(root, name);
    }

    EnginePreferences(AbstractPreferences parent, String name) {
        super(parent, name);
        this.properties = new Properties();
        this.keyPrefix = name + ".";
    }

    Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }


    /**
     * Put the given key-value association into this preference node.  It is
     * guaranteed that <tt>key</tt> and <tt>value</tt> are non-null and of
     * legal length.  Also, it is guaranteed that this node has not been
     * removed.  (The implementor needn't check for any of these things.)
     * <p>
     * <p>This method is invoked with the lock on this node held.
     *
     * @param key   the key
     * @param value the value
     */
    @Override
    protected void putSpi(String key, String value) {
        if (key.startsWith(keyPrefix)) {
            String globalValue = System.getProperty(key);
            if (globalValue != null) {
                System.setProperty(key, value);
            }
        }
        properties.put(key, value);
    }

    /**
     * Return the value associated with the specified key at this preference
     * node, or <tt>null</tt> if there is no association for this key, or the
     * association cannot be determined at this time.  It is guaranteed that
     * <tt>key</tt> is non-null.  Also, it is guaranteed that this node has
     * not been removed.  (The implementor needn't check for either of these
     * things.)
     * <p>
     * <p> Generally speaking, this method should not throw an exception
     * under any circumstances.  If, however, if it does throw an exception,
     * the exception will be intercepted and treated as a <tt>null</tt>
     * return value.
     * <p>
     * <p>This method is invoked with the lock on this node held.
     *
     * @param key the key
     * @return the value associated with the specified key at this preference
     * node, or <tt>null</tt> if there is no association for this
     * key, or the association cannot be determined at this time.
     */
    @Override
    protected String getSpi(String key) {
        if (key.startsWith(keyPrefix)) {
            String globalValue = System.getProperty(key);
            if (globalValue != null) {
                return resolveValue(globalValue);
            }
        }
        return resolveKey(key);
    }

    /**
     * Remove the association (if any) for the specified key at this
     * preference node.  It is guaranteed that <tt>key</tt> is non-null.
     * Also, it is guaranteed that this node has not been removed.
     * (The implementor needn't check for either of these things.)
     * <p>
     * <p>This method is invoked with the lock on this node held.
     *
     * @param key the key
     */
    @Override
    protected void removeSpi(String key) {
        if (key.startsWith(keyPrefix)) {
            String globalValue = System.getProperty(key);
            if (globalValue != null) {
                System.clearProperty(key);
            }
        }
        properties.remove(key);
    }

    /**
     * Removes this preference node, invalidating it and any preferences that
     * it contains.  The named child will have no descendants at the time this
     * invocation is made (i.e., the {@link java.util.prefs.Preferences#removeNode()} method
     * invokes this method repeatedly in a bottom-up fashion, removing each of
     * a node's descendants before removing the node itself).
     * <p>
     * <p>This method is invoked with the lock held on this node and its
     * parent (and all ancestors that are being removed as a
     * result of a single invocation to {@link java.util.prefs.Preferences#removeNode()}).
     * <p>
     * <p>The removal of a node needn't become persistent until the
     * <tt>flush</tt> method is invoked on this node (or an ancestor).
     * <p>
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #removeNode()}
     * invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *                               due to a failure in the backing store, or inability to
     *                               communicate with it.
     */
    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        properties.clear();
    }

    /**
     * Returns all of the keys that have an associated value in this
     * preference node.  (The returned array will be of size zero if
     * this node has no preferences.)  It is guaranteed that this node has not
     * been removed.
     * <p>
     * <p>This method is invoked with the lock on this node held.
     * <p>
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #keys()} invocation.
     *
     * @return an array of the keys that have an associated value in this
     * preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *                               due to a failure in the backing store, or inability to
     *                               communicate with it.
     */
    @Override
    protected String[] keysSpi() throws BackingStoreException {
        Set<String> names = properties.stringPropertyNames();
        return names.toArray(new String[names.size()]);
    }

    /**
     * Returns the names of the children of this preference node.  (The
     * returned array will be of size zero if this node has no children.)
     * This method need not return the names of any nodes already cached,
     * but may do so without harm.
     * <p>
     * <p>This method is invoked with the lock on this node held.
     * <p>
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #childrenNames()}
     * invocation.
     *
     * @return an array containing the names of the children of this
     * preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *                               due to a failure in the backing store, or inability to
     *                               communicate with it.
     */
    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];
    }

    /**
     * Returns the named child of this preference node, creating it if it does
     * not already exist.  It is guaranteed that <tt>name</tt> is non-null,
     * non-empty, does not contain the slash character ('/'), and is no longer
     * than {@link #MAX_NAME_LENGTH} characters.  Also, it is guaranteed that
     * this node has not been removed.  (The implementor needn't check for any
     * of these things.)
     * <p>
     * <p>Finally, it is guaranteed that the named node has not been returned
     * by a previous invocation of this method or {@link #getChild(String)}
     * after the last time that it was removed.  In other words, a cached
     * value will always be used in preference to invoking this method.
     * Subclasses need not maintain their own cache of previously returned
     * children.
     * <p>
     * <p>The implementer must ensure that the returned node has not been
     * removed.  If a like-named child of this node was previously removed, the
     * implementer must return a newly constructed <tt>AbstractPreferences</tt>
     * node; once removed, an <tt>AbstractPreferences</tt> node
     * cannot be "resuscitated."
     * <p>
     * <p>If this method causes a node to be created, this node is not
     * guaranteed to be persistent until the <tt>flush</tt> method is
     * invoked on this node or one of its ancestors (or descendants).
     * <p>
     * <p>This method is invoked with the lock on this node held.
     *
     * @param name The name of the child node to return, relative to
     *             this preference node.
     * @return The named child node.
     */
    @Override
    protected AbstractPreferences childSpi(String name) {
        return null;
    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to synchronize any cached preferences stored at this node
     * with any stored in the backing store.  (It is perfectly possible that
     * this node does not exist on the backing store, either because it has
     * been deleted by another VM, or because it has not yet been created.)
     * Note that this method should <i>not</i> synchronize the preferences in
     * any subnodes of this node.  If the backing store naturally syncs an
     * entire subtree at once, the implementer is encouraged to override
     * sync(), rather than merely overriding this method.
     * <p>
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #sync()} invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *                               due to a failure in the backing store, or inability to
     *                               communicate with it.
     */
    @Override
    protected void syncSpi() throws BackingStoreException {
        load();
        store();
    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to force any cached changes in the contents of this
     * preference node to the backing store, guaranteeing their persistence.
     * (It is perfectly possible that this node does not exist on the backing
     * store, either because it has been deleted by another VM, or because it
     * has not yet been created.)  Note that this method should <i>not</i>
     * flush the preferences in any subnodes of this node.  If the backing
     * store naturally flushes an entire subtree at once, the implementer is
     * encouraged to override flush(), rather than merely overriding this
     * method.
     * <p>
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #flush()} invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *                               due to a failure in the backing store, or inability to
     *                               communicate with it.
     */
    @Override
    protected void flushSpi() throws BackingStoreException {
        store();
    }

    private String resolveKey(String key) {
        String value = properties.getProperty(key, null);
        return value != null ? resolveValue(value) : null;
    }

    private String resolveValue(String value) {
        boolean change;
        int pos1 = 0;
        do {
            change = false;
            pos1 = value.indexOf("${", pos1);
            if (pos1 >= 0) {
                int pos2 = value.indexOf("}", pos1 + 2);
                if (pos2 != -1) {
                    String varKey = value.substring(pos1 + 2, pos2);
                    String varValue = getVariableValue(varKey);
                    if (varValue != null) {
                        String newValue = value.replace("${" + varKey + "}", varValue);
                        change = !newValue.equals(value);
                        value = newValue;
                    }
                }
            } else {
                break;
            }
        } while (change);

        return value;
    }

    private String getVariableValue(String varKey) {
        String varValue = properties.getProperty(varKey);
        if (varValue == null && parent() != null) {
            varValue = parent().get(varKey, null);
        }
        if (varValue == null) {
            varValue = System.getProperty(varKey, null);
        }
        if (varValue == null) {
            varValue = System.getenv(varKey);
        }
        return varValue;
    }

    private void load() throws BackingStoreException {
        Properties properties = new Properties();

        Path userConfigFile = getBackingStorePath();
        try {
            try (Reader reader = Files.newBufferedReader(userConfigFile)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            throw new BackingStoreException(e);
        }

        properties.putAll(this.properties);

        this.properties.clear();
        this.properties.putAll(properties);
    }

    private void store() throws BackingStoreException {
        Path userConfigFile = getBackingStorePath();

        if (!Files.isDirectory(userConfigFile.getParent())) {
            try {
                Files.createDirectories(userConfigFile.getParent());
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }

        try {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(userConfigFile)) {
                properties.store(bufferedWriter, "SNAP configuration '" + name() + "'");
            }
        } catch (IOException e) {
            throw new BackingStoreException(e);
        }
    }

    public Path getBackingStorePath() {
        return backingStorePath != null ? backingStorePath : getDefaultBackingStorePath();
    }

    public void setBackingStorePath(Path backingStorePath) {
        this.backingStorePath = backingStorePath;
    }

    private Path getDefaultBackingStorePath() {
        return EngineConfig.instance().userDir().resolve("etc").resolve(name() + Config.CONFIG_FILE_EXT);
    }

}
