package org.esa.snap.vfs.preferences.model;

import java.util.List;

/**
 * VFS Remote File Repository data structure for storing VFS connection data.
 *
 * @author Adrian Drăghici
 */
public final class VFSRemoteFileRepository {

    private String name;
    private String scheme;
    private String address;
    private List<Property> properties;

    /**
     * Creates a new VFS Remote File Repository data structure.
     *
     * @param name       The name of VFS Remote File Repository
     * @param schema     The scheme of VFS Remote File Repository
     * @param address    The address of VFS Remote File Repository
     * @param properties The properties of VFS Remote File Repository
     */
    public VFSRemoteFileRepository(String name, String schema, String address, List<Property> properties) {
        this.name = name;
        this.scheme = schema;
        this.address = address;
        this.properties = properties;
    }

    /**
     * Gets the name of VFS Remote File Repository
     *
     * @return The name of VFS Remote File Repository
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the root of VFS Remote File Repository
     *
     * @return The root of VFS Remote File Repository
     */
    public String getRoot() {
        return name + ":";
    }

    /**
     * Gets the scheme of VFS Remote File Repository
     *
     * @return The scheme of VFS Remote File Repository
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Gets the address of VFS Remote File Repository
     *
     * @return The address of VFS Remote File Repository
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the properties of VFS Remote File Repository
     *
     * @return The properties of VFS Remote File Repository
     */
    public List<Property> getProperties() {
        return properties;
    }
}
