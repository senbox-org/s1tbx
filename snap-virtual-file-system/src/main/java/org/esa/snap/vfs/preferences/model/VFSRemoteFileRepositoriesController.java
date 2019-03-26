package org.esa.snap.vfs.preferences.model;

import org.esa.snap.vfs.preferences.validators.RepositoryAddressValidator;
import org.esa.snap.vfs.preferences.validators.RepositoryNameValidator;
import org.esa.snap.vfs.preferences.validators.RepositorySchemaValidator;
import org.esa.snap.runtime.EngineConfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller for VFS Remote File Repositories.
 * Used for establish a strategy with storing VFS connection data.
 *
 * @author Adrian Draghici
 */
public final class VFSRemoteFileRepositoriesController {

    /**
     * The flag for update mode for remote file repositories/remote file repository properties: add.
     */
    private static final int MODE_ADD = 0;

    /**
     * The flag for update mode for remote file repositories/remote file repository properties: remove.
     */
    private static final int MODE_REMOVE = 1;

    /**
     * The separator used on remote file repositories/remote file repository properties ids list.
     */
    public static final String LIST_ITEM_SEPARATOR = ";";

    /**
     * The pattern for remote file repository.
     */
    private static final String REPO_ID_KEY = "%repo_id%";

    /**
     * The pattern for remote file repository property.
     */
    private static final String PROP_ID_KEY = "%prop_id%";

    /**
     * The preference key for remote file repositories list.
     */
    public static final String PREFERENCE_KEY_VFS_REPOSITORIES = "vfs.repositories";

    /**
     * The preference key for remote file repository item.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY = PREFERENCE_KEY_VFS_REPOSITORIES + ".repository_" + REPO_ID_KEY;

    /**
     * The preference key for remote file repository item name.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_NAME = PREFERENCE_KEY_VFS_REPOSITORY + ".name";

    /**
     * The preference key for remote file repository item schema.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA = PREFERENCE_KEY_VFS_REPOSITORY + ".schema";

    /**
     * The preference key for remote file repository item address.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_ADDRESS = PREFERENCE_KEY_VFS_REPOSITORY + ".address";

    /**
     * The preference key for remote file repository properties list.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES = PREFERENCE_KEY_VFS_REPOSITORY + ".properties";

    /**
     * The preference key for remote file repository property item.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES + ".property_" + PROP_ID_KEY;

    /**
     * The preference key for remote file repository property item name.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY + ".name";

    /**
     * The preference key for remote file repository property item value.
     */
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY + ".value";

    /**
     * The pattern for credential remote file repository property name.
     */
    public static final String CREDENTIAL_PROPERTY_NAME_REGEX = "((.*)((key)|(password)|(secret))(.*))";

    private static final String CONFIG_FILE = EngineConfig.instance().userDir().toString() + "/config/Preferences/vfs.properties";

    private static Logger logger = Logger.getLogger(VFSRemoteFileRepositoriesController.class.getName());

    private final Properties properties = new Properties();

    private String remoteRepositoriesIds;

    /**
     * Creates the new VFS Remote File Repositories Controller.
     */
    public VFSRemoteFileRepositoriesController() {
        loadProperties();
    }

    /**
     * Loads the VFS Remote File Repositories Properties from SNAP configuration file.
     */
    public void loadProperties() {
        try {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                Files.createFile(Paths.get(CONFIG_FILE));
            }
            InputStream inputStream = new FileInputStream(CONFIG_FILE);
            properties.load(inputStream);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to reload VFS Remote File Repositories Properties from SNAP configuration file. Details: " + ex.getMessage());
        }
    }

    /**
     * Writes the VFS Remote File Repositories Properties on SNAP configuration file.
     */
    public void saveProperties() {
        try {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                Files.createFile(Paths.get(CONFIG_FILE));
            }
            OutputStream outputStream = new FileOutputStream(CONFIG_FILE);
            properties.store(outputStream, "");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to save VFS properties to SNAP configuration file. Details: " + ex.getMessage());
        }
    }

    /**
     * Check whether VFS Remote File Repositories Properties is changed.
     *
     * @return {@code true} if VFS Remote File Repositories Properties is changed
     */
    public boolean isChanged() {
        Properties newProperties = new Properties();
        try (InputStream inputStream = new FileInputStream(CONFIG_FILE)) {
            newProperties.load(inputStream);
            return !newProperties.equals(properties);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Unable to check changes for VFS Remote File Repositories Properties. Details: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Writes the property on SNAP configuration file.
     *
     * @param property The property
     */
    private void writeProperty(Property property) {
        if (property != null) {
            try {
                properties.setProperty(property.getName(), property.getValue());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to write the property on SNAP configuration file. Details: " + ex.getMessage());
            }
        }
    }

    /**
     * Reads the property from SNAP configuration file.
     *
     * @param propertyKey The key of property
     * @return The property
     * @see Property
     */
    private Property getProperty(String propertyKey) {
        return new Property(propertyKey, properties.getProperty(propertyKey, ""));
    }

    /**
     * Removes the property from SNAP configuration file.
     *
     * @param property The property
     */
    private void removeProperty(Property property) {
        if (property != null) {
            try {
                properties.remove(property.getName());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to remove the property from SNAP configuration file. Details: " + ex.getMessage());
            }
        }
    }

    /**
     * Gets the remote file repositories ids list.
     *
     * @return The remote file repositories ids list
     */
    public Property getRemoteRepositoriesIds() {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORIES);
    }

    /**
     * Updates the remote file repositories ids list as indicated by the flag (add/remove).
     *
     * @param remoteRepositoryId The remote file repositories ids list
     * @param mode               The mode flag
     */
    private void updateRemoteRepositoriesIds(String remoteRepositoryId, int mode) {
        Property remoteRepositoryIdsProperty = getRemoteRepositoriesIds();
        remoteRepositoriesIds = remoteRepositoryIdsProperty.getValue();
        remoteRepositoriesIds = remoteRepositoriesIds != null ? remoteRepositoriesIds : "";
        switch (mode) {
            case MODE_ADD:
                remoteRepositoriesIds = !remoteRepositoriesIds.isEmpty() ? remoteRepositoriesIds + LIST_ITEM_SEPARATOR + remoteRepositoryId : remoteRepositoryId;
                break;
            case MODE_REMOVE:
                remoteRepositoriesIds = remoteRepositoriesIds.replaceAll("((" + remoteRepositoryId + "(" + LIST_ITEM_SEPARATOR + ")?)|(" + LIST_ITEM_SEPARATOR + ")?" + remoteRepositoryId + ")", "");
                break;
            default:
                break;
        }
        remoteRepositoryIdsProperty.setValue(remoteRepositoriesIds);
        writeProperty(remoteRepositoryIdsProperty);
    }

    /**
     * Gets the remote file repository properties ids list.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The remote file repository properties ids list
     */
    public Property getRemoteRepositoryPropertiesIds(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    /**
     * Updates the remote file repository properties ids list as indicated by the flag (add/remove).
     *
     * @param remoteRepositoryId         The remote file repository id
     * @param remoteRepositoryPropertyId The remote file repository property id
     * @param mode                       The mode flag
     */
    private void updateRemoteRepositoryPropertiesIds(String remoteRepositoryId, String remoteRepositoryPropertyId, int mode) {
        Property remoteRepositoryPropertiesIdsProperty = getRemoteRepositoryPropertiesIds(remoteRepositoryId);
        String remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIdsProperty.getValue();
        remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIds != null ? remoteRepositoryPropertiesIds : "";
        switch (mode) {
            case MODE_ADD:
                remoteRepositoryPropertiesIds = !remoteRepositoryPropertiesIds.isEmpty() ? remoteRepositoryPropertiesIds + LIST_ITEM_SEPARATOR + remoteRepositoryPropertyId : remoteRepositoryPropertyId;
                break;
            case MODE_REMOVE:
                remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIds.replaceAll("((" + remoteRepositoryPropertyId + "(" + LIST_ITEM_SEPARATOR + ")?)|(" + LIST_ITEM_SEPARATOR + ")?" + remoteRepositoryPropertyId + ")", "");
                break;
            default:
                break;
        }
        remoteRepositoryPropertiesIdsProperty.setValue(remoteRepositoryPropertiesIds);
        writeProperty(remoteRepositoryPropertiesIdsProperty);
    }

    /**
     * Creates the remote file repository instance and add his id to remote file repositories ids list.
     *
     * @return The id of new remote file repository instance
     * @see VFSRemoteFileRepository
     */
    public String registerNewRemoteRepository() {
        String remoteRepositoryId = "" + System.currentTimeMillis();
        updateRemoteRepositoriesIds(remoteRepositoryId, MODE_ADD);
        return remoteRepositoryId;
    }

    /**
     * Creates the remote file repository property instance and add his id to remote file repository properties ids list.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The id of new remote file repository property instance
     * @see VFSRemoteFileRepository
     * @see Property
     */
    public String registerNewRemoteRepositoryProperty(String remoteRepositoryId) {
        String remoteRepositoryPropertyId = "" + System.currentTimeMillis();
        updateRemoteRepositoryPropertiesIds(remoteRepositoryId, remoteRepositoryPropertyId, MODE_ADD);
        return remoteRepositoryPropertyId;
    }

    /**
     * Gets the remote file repository name.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The remote file repository name
     */
    public Property getRemoteRepositoryName(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_NAME.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    /**
     * Sets the remote file repository name.
     *
     * @param remoteRepositoryId   The remote file repository id
     * @param remoteRepositoryName The remote file repository name
     */
    public void setRemoteRepositoryName(String remoteRepositoryId, String remoteRepositoryName) {
        Property remoteRepositoryNameProperty = new Property(PREFERENCE_KEY_VFS_REPOSITORY_NAME.replace(REPO_ID_KEY, remoteRepositoryId), remoteRepositoryName);
        writeProperty(remoteRepositoryNameProperty);
    }

    /**
     * Gets the remote file repository schema.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The remote file repository schema
     */
    public Property getRemoteRepositorySchema(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    /**
     * Sets the remote file repository schema.
     *
     * @param remoteRepositoryId     The remote file repository id
     * @param remoteRepositorySchema The remote file repository schema
     */
    public void setRemoteRepositorySchema(String remoteRepositoryId, String remoteRepositorySchema) {
        Property remoteRepositorySchemaProperty = new Property(PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA.replace(REPO_ID_KEY, remoteRepositoryId), remoteRepositorySchema);
        writeProperty(remoteRepositorySchemaProperty);
    }

    /**
     * Gets the remote file repository address.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The remote file repository address
     */
    public Property getRemoteRepositoryAddress(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_ADDRESS.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    /**
     * Sets the remote file repository address.
     *
     * @param remoteRepositoryId      The remote file repository id
     * @param remoteRepositoryAddress The remote file repository address
     */
    public void setRemoteRepositoryAddress(String remoteRepositoryId, String remoteRepositoryAddress) {
        Property remoteRepositorySchemaProperty = new Property(PREFERENCE_KEY_VFS_REPOSITORY_ADDRESS.replace(REPO_ID_KEY, remoteRepositoryId), remoteRepositoryAddress);
        writeProperty(remoteRepositorySchemaProperty);
    }

    /**
     * Gets the remote file repository property name.
     *
     * @param remoteRepositoryId         The remote file repository id
     * @param remoteRepositoryPropertyId The remote file repository property id
     * @return The remote file repository property name
     */
    public Property getRemoteRepositoryPropertyName(String remoteRepositoryId, String remoteRepositoryPropertyId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId));
    }

    /**
     * Sets the remote file repository property name.
     *
     * @param remoteRepositoryId           The remote file repository id
     * @param remoteRepositoryPropertyId   The remote file repository property id
     * @param remoteRepositoryPropertyName The remote file repository property name
     */
    public void setRemoteRepositoryPropertyName(String remoteRepositoryId, String remoteRepositoryPropertyId, String remoteRepositoryPropertyName) {
        Property remoteRepositoryPropertyNameProperty = new Property(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId), remoteRepositoryPropertyName);
        writeProperty(remoteRepositoryPropertyNameProperty);
    }

    /**
     * Gets the remote file repository property value.
     *
     * @param remoteRepositoryId         The remote file repository id
     * @param remoteRepositoryPropertyId The remote file repository property id
     * @return The remote file repository property value
     */
    public Property getRemoteRepositoryPropertyValue(String remoteRepositoryId, String remoteRepositoryPropertyId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId));
    }

    /**
     * Sets the remote file repository property value.
     *
     * @param remoteRepositoryId            The remote file repository id
     * @param remoteRepositoryPropertyId    The remote file repository property id
     * @param remoteRepositoryPropertyValue The remote file repository property value
     */
    public void setRemoteRepositoryPropertyValue(String remoteRepositoryId, String remoteRepositoryPropertyId, String remoteRepositoryPropertyValue) {
        Property remoteRepositoryPropertyValueProperty = new Property(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId), remoteRepositoryPropertyValue);
        writeProperty(remoteRepositoryPropertyValueProperty);
    }

    /**
     * Deletes permanently the remote file repository property.
     *
     * @param remoteRepositoryId         The remote file repository id
     * @param remoteRepositoryPropertyId The remote file repository property id
     */
    public void removeRemoteRepositoryProperty(String remoteRepositoryId, String remoteRepositoryPropertyId) {
        removeProperty(getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId));
        removeProperty(getRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId));
        updateRemoteRepositoryPropertiesIds(remoteRepositoryId, remoteRepositoryPropertyId, MODE_REMOVE);
    }

    /**
     * Gets the list of remote file repository properties.
     *
     * @param remoteRepositoryId The remote file repository id
     * @return The list of remote file repository properties
     */
    private java.util.List<Property> getVFSRemoteFileRepositoryProperties(String remoteRepositoryId) {
        java.util.List<Property> vfsRemoteFileRepositoryProperties = new ArrayList<>();
        String remoteRepositoryPropertiesIds = getRemoteRepositoryPropertiesIds(remoteRepositoryId).getValue();
        if (remoteRepositoryPropertiesIds != null && !remoteRepositoryPropertiesIds.isEmpty()) {
            String[] remoteRepositoryPropertiesIdsList = remoteRepositoryPropertiesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryPropertyId : remoteRepositoryPropertiesIdsList) {
                String remoteRepositoryPropertyName = getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
                String remoteRepositoryPropertyValue = getRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
                if (remoteRepositoryPropertyName != null && !remoteRepositoryPropertyName.isEmpty() && remoteRepositoryPropertyValue != null && !remoteRepositoryPropertyValue.isEmpty()) {
                    vfsRemoteFileRepositoryProperties.add(new Property(remoteRepositoryPropertyName, remoteRepositoryPropertyValue));
                }
            }
        }
        return vfsRemoteFileRepositoryProperties;
    }

    /**
     * Gets the list of remote file repositories.
     *
     * @return The list of remote file repositories
     * @see VFSRemoteFileRepository
     */
    public static List<VFSRemoteFileRepository> getVFSRemoteFileRepositories() {
        List<VFSRemoteFileRepository> vfsRemoteFileRepositories = new ArrayList<>();
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController();
        String remoteRepositoriesIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds().getValue();
        if (remoteRepositoriesIds != null && !remoteRepositoriesIds.isEmpty()) {
            String[] remoteRepositoriesIdsList = remoteRepositoriesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryId : remoteRepositoriesIdsList) {
                String remoteRepositoryName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(remoteRepositoryId).getValue();
                String remoteRepositorySchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(remoteRepositoryId).getValue();
                String remoteRepositoryAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(remoteRepositoryId).getValue();
                java.util.List<Property> vfsRemoteFileRepositoryProperties = vfsRemoteFileRepositoriesController.getVFSRemoteFileRepositoryProperties(remoteRepositoryId);
                vfsRemoteFileRepositories.add(new VFSRemoteFileRepository(remoteRepositoryName, remoteRepositorySchema, remoteRepositoryAddress, vfsRemoteFileRepositoryProperties));
            }
        }
        return vfsRemoteFileRepositories;
    }

    /**
     * Installs (registers) the new remote file repository.
     *
     * @param vfsRemoteFileRepository The new remote file repository
     * @throws IOException If an I/O error occurs
     * @see VFSRemoteFileRepository
     */
    public static void installVFSRemoteFileRepository(VFSRemoteFileRepository vfsRemoteFileRepository) throws IOException {
        String remoteRepositoryId = null;
        VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController();
        try {
            if (new RepositoryNameValidator().isValid(vfsRemoteFileRepository.getName()) && vfsRemoteFileRepositoriesController.isUniqueRemoteRepositoryName(vfsRemoteFileRepository.getName()) && new RepositorySchemaValidator().isValid(vfsRemoteFileRepository.getScheme()) && new RepositoryAddressValidator().isValid(vfsRemoteFileRepository.getAddress())) {
                remoteRepositoryId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
                for (Property remoteRepositoryProperty : vfsRemoteFileRepository.getProperties()) {
                    if (new RepositoryNameValidator().isValid(remoteRepositoryProperty.getName()) && vfsRemoteFileRepositoriesController.isUniqueRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryProperty.getName()) && !remoteRepositoryProperty.getValue().isEmpty()) {
                        String remoteRepositoryPropertyId = vfsRemoteFileRepositoriesController.registerNewRemoteRepositoryProperty(remoteRepositoryId);
                        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId, remoteRepositoryProperty.getName());
                        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId, remoteRepositoryProperty.getValue());
                    } else {
                        throw new IllegalArgumentException("Please check property: " + remoteRepositoryProperty.getName());
                    }
                }
                vfsRemoteFileRepositoriesController.setRemoteRepositoryName(remoteRepositoryId, vfsRemoteFileRepository.getName());
                vfsRemoteFileRepositoriesController.setRemoteRepositorySchema(remoteRepositoryId, vfsRemoteFileRepository.getScheme());
                vfsRemoteFileRepositoriesController.setRemoteRepositoryAddress(remoteRepositoryId, vfsRemoteFileRepository.getAddress());
            } else {
                throw new IllegalArgumentException("Please check VFS data to meets following requirements:\n- Name must be unique\n- Name must be alphanumeric.\n- Underscores are allowed in name.\n- Length of name is between 3 and 25 characters.\nAddress must contains URL specific characters.");
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.FINE, "Invalid remote file repository data entered. Details: " + ex.getMessage());
            if (remoteRepositoryId != null && !remoteRepositoryId.isEmpty()) {
                try {
                    vfsRemoteFileRepositoriesController.removeRemoteRepository(remoteRepositoryId);
                } catch (Exception ignored) {
                    logger.log(Level.SEVERE, "Unable to remove fragment of remote file repository. Details: " + ex.getMessage());
                }
            }
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Deletes permanently the remote file repository
     *
     * @param remoteRepositoryId The remote file repository id
     * @throws IllegalArgumentException If a value validation failure occurs
     */
    public void removeRemoteRepository(String remoteRepositoryId) {
        removeProperty(getRemoteRepositoryName(remoteRepositoryId));
        removeProperty(getRemoteRepositorySchema(remoteRepositoryId));
        removeProperty(getRemoteRepositoryAddress(remoteRepositoryId));
        Property remoteRepositoryPropertiesIdsProperty = getRemoteRepositoryPropertiesIds(remoteRepositoryId);
        String remoteRepositoriesPropertiesIds = remoteRepositoryPropertiesIdsProperty.getValue();
        String[] remoteRepositoriesPropertiesIdsList0 = remoteRepositoriesPropertiesIds.split(LIST_ITEM_SEPARATOR);
        for (String remoteRepositoriesPropertyId : remoteRepositoriesPropertiesIdsList0) {
            removeRemoteRepositoryProperty(remoteRepositoryId, remoteRepositoriesPropertyId);
        }
        removeProperty(remoteRepositoryPropertiesIdsProperty);
        updateRemoteRepositoriesIds(remoteRepositoryId, MODE_REMOVE);
    }

    /**
     * Tells whether the new remote file repository name is unique.
     *
     * @param newRemoteRepositoryName The remote file repository name
     * @return {@code true} if the new remote file repository name is unique
     */
    public boolean isUniqueRemoteRepositoryName(String newRemoteRepositoryName) {
        remoteRepositoriesIds = getRemoteRepositoriesIds().getValue();
        if (remoteRepositoriesIds != null && !remoteRepositoriesIds.isEmpty()) {
            String[] remoteRepositoriesIdsList = remoteRepositoriesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryId : remoteRepositoriesIdsList) {
                String remoteRepositoryName = getRemoteRepositoryName(remoteRepositoryId).getValue();
                if (remoteRepositoryName != null && remoteRepositoryName.contentEquals(newRemoteRepositoryName)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether the new remote file repository schema is unique.
     *
     * @param newRemoteRepositorySchema The remote file repository schema
     * @return {@code true} if the new remote file repository schema is unique
     */
    public boolean isUniqueRemoteRepositorySchema(String newRemoteRepositorySchema) {
        remoteRepositoriesIds = getRemoteRepositoriesIds().getValue();
        if (remoteRepositoriesIds != null && !remoteRepositoriesIds.isEmpty()) {
            String[] remoteRepositoriesIdsList = remoteRepositoriesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryId : remoteRepositoriesIdsList) {
                String remoteRepositorySchema = getRemoteRepositorySchema(remoteRepositoryId).getValue();
                if (remoteRepositorySchema != null && remoteRepositorySchema.contentEquals(newRemoteRepositorySchema)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether the new remote file repository property name is unique.
     *
     * @param remoteRepositoryId              The remote file repository id
     * @param newRemoteRepositoryPropertyName The remote file repository property name
     * @return {@code true} if the new remote file repository property name is unique
     */
    public boolean isUniqueRemoteRepositoryPropertyName(String remoteRepositoryId, String newRemoteRepositoryPropertyName) {
        String remoteRepositoryPropertiesIds = getRemoteRepositoryPropertiesIds(remoteRepositoryId).getValue();
        if (remoteRepositoryPropertiesIds != null && !remoteRepositoryPropertiesIds.isEmpty()) {
            String[] remoteRepositoriesPropertiesIdsList = remoteRepositoryPropertiesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryPropertyId : remoteRepositoriesPropertiesIdsList) {
                String remoteRepositoryPropertyName = getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
                if (remoteRepositoryPropertyName != null && remoteRepositoryPropertyName.contentEquals(newRemoteRepositoryPropertyName)) {
                    return false;
                }
            }
        }
        return true;
    }


}
