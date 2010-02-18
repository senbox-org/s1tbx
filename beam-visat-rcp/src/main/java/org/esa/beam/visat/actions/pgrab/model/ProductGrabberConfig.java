package org.esa.beam.visat.actions.pgrab.model;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

/**
 * This class handles the configuration of the ProductGrabber.
 */
public class ProductGrabberConfig {

    private static final String REPOSITORIES_KEY = "productGrabber.repository.%1$d.dirPath";
    private static final String CURRENT_REPSOITORY_KEY = "productGrabber.repository.selected.dirPath";
    private static final String WINDOW_LOCATION_X_KEY = "productGrabber.window.locationX";
    private static final String WINDOW_LOCATION_Y_KEY = "productGrabber.window.locationY";
    private static final String WINDOW_WIDTH_KEY = "productGrabber.window.width";
    private static final String WINDOW_HEIGHT_KEY = "productGrabber.window.height";

    private final PropertyMap _properties;

    /**
     * Creates a new instance with the given {@link PropertyMap}.
     * The property map which is used to load and store the configuration.
     *
     * @param configuration the {@link PropertyMap}.
     */
    public ProductGrabberConfig(final PropertyMap configuration) {
        Guardian.assertNotNull("configuration", configuration);
        _properties = configuration;
    }

    /**
     * Sets the repositories.
     *
     * @param repositories the repositories.
     */
    public void setRepositories(final Repository[] repositories) {
        clearRepositoryList();
        for (int i = 0; i < repositories.length; i++) {
            final Repository repository = repositories[i];
            final String propertyName = createRepositoryPropertyName(i);
            _properties.setPropertyString(propertyName, repository.getBaseDir().getPath());
        }
    }

    /**
     * Retrieves the stored repositories.
     *
     * @return the stored repositories.
     */
    public Repository[] getRepositories() {
        final ArrayList repositoryList = new ArrayList();
        for (int i = 0; i < _properties.getProperties().size(); i++) {
            final String propertyName = createRepositoryPropertyName(i);
            final String baseDirPath = _properties.getPropertyString(propertyName);
            if (baseDirPath != null) {
                final File baseDir = new File(baseDirPath);
                if (baseDir.exists()) {
                    repositoryList.add(new Repository(baseDir));
                }
            }
        }
        return (Repository[]) repositoryList.toArray(new Repository[repositoryList.size()]);
    }

    /**
     * Sets the last selected repository.
     *
     * @param repsository the last selected repsoitory.
     */
    public void setLastSelectedRepository(final Repository repsository) {
        if (repsository != null) {
            _properties.setPropertyString(CURRENT_REPSOITORY_KEY, repsository.getBaseDir().getPath());
        }
    }

    /**
     * Retrieves thelast selected repository.
     *
     * @return the last selected repsoitory, maybe <code>null</code>.
     */
    public String getLastSelectedRepositoryDir() {
        return _properties.getPropertyString(CURRENT_REPSOITORY_KEY, null);
    }

    /**
     * Sets the window bounds of the ProductGrabber dialog.
     *
     * @param windowBounds the window bounds.
     */
    public void setWindowBounds(final Rectangle windowBounds) {
        _properties.setPropertyInt(WINDOW_LOCATION_X_KEY, windowBounds.x);
        _properties.setPropertyInt(WINDOW_LOCATION_Y_KEY, windowBounds.y);
        _properties.setPropertyInt(WINDOW_WIDTH_KEY, windowBounds.width);
        _properties.setPropertyInt(WINDOW_HEIGHT_KEY, windowBounds.height);
    }

    /**
     * Retrieves the window bounds of the ProductGrabber dialog.
     *
     * @return the window bounds.
     */
    public Rectangle getWindowBounds() {
        final int x = _properties.getPropertyInt(WINDOW_LOCATION_X_KEY, 50);
        final int y = _properties.getPropertyInt(WINDOW_LOCATION_Y_KEY, 50);
        final int width = _properties.getPropertyInt(WINDOW_WIDTH_KEY, 700);
        final int height = _properties.getPropertyInt(WINDOW_HEIGHT_KEY, 450);

        return new Rectangle(x, y, width, height);
    }

    private void clearRepositoryList() {
        for (int i = 0; i < _properties.getProperties().size(); i++) {
            final String propertyName = createRepositoryPropertyName(i);
            if (_properties.getPropertyString(propertyName) != "") {
                _properties.setPropertyString(propertyName, null);
            }
        }
    }

    private String createRepositoryPropertyName(final int i) {
        return String.format(REPOSITORIES_KEY, new Object[]{new Integer(i)});
    }

}
