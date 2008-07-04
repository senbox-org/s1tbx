/*
 * $Id: BasicApp.java,v 1.43 2007/04/23 13:51:01 marcoz Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.jidesoft.action.*;
import com.jidesoft.docking.DefaultDockingManager;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.status.LabelStatusBarItem;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.LayoutPersistence;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.framework.ui.application.support.DefaultApplicationDescriptor;
import org.esa.beam.framework.ui.command.*;
import org.esa.beam.util.*;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.FileChooserFactory;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.logging.CacheHandler;

import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.UIDefaults;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * The <code>BasicApp</code> can be used as a base class for applications which use a single main frame as user
 * interface.
 * <p/>
 * <p> This class provides several useful capabilites as there are: <ld> <li> Action (command) management </li> <li>
 * Automatic user preferences loading and saving </li> <li> Logfile support </li> <li> Status bar support </li> <li>
 * Splash screen support </li> </ld>
 * <p/>
 * <p> And last but not least <code>BasicApp</code> automatically stores the file pathes a user visited in open- or save
 * dialog boxes.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see org.esa.beam.framework.ui.command.Command
 * @see org.esa.beam.framework.ui.command.ExecCommand
 * @see org.esa.beam.framework.ui.command.CommandGroup
 * @see org.esa.beam.framework.ui.command.CommandManager
 */
public class BasicApp {

    public static final String PROPERTY_KEY_APP_LAST_OPEN_DIR = "app.file.lastOpenDir";
    public static final String PROPERTY_KEY_APP_LAST_OPEN_FORMAT = "app.file.lastOpenFormat";
    public static final String PROPERTY_KEY_APP_LAST_SAVE_DIR = "app.file.lastSaveDir";
    public static final String PROPERTY_KEY_APP_LOG_ENABLED = "app.log.enabled";
    public static final String PROPERTY_KEY_APP_LOG_PREFIX = "app.log.prefix";
    public static final String PROPERTY_KEY_APP_LOG_LEVEL = "app.log.level";
    public static final String PROPERTY_KEY_APP_LOG_ECHO = "app.log.echo";
    public static final String PROPERTY_KEY_APP_DEBUG_ENABLED = "app.debug.enabled";
    public static final String PROPERTY_KEY_APP_UI_LAF = "app.ui.laf";
    public static final String PROPERTY_KEY_APP_UI_FONT_NAME = "app.ui.font.name";
    public static final String PROPERTY_KEY_APP_UI_FONT_SIZE = "app.ui.font.size";
    public static final String PROPERTY_KEY_APP_UI_USE_SYSTEM_FONT_SETTINGS = "app.ui.useSystemFontSettings";

    private static final String _IMAGE_RESOURCE_PATH = "/org/esa/beam/resources/images/";

    private boolean _uiDefaultsInitialized;

    private final ApplicationDescriptor applicationDescriptor;

    private File _preferencesFile;

    private CommandManager _commandManager;
    private CommandUIFactory _commandUIFactory;
    private MainFrame _mainFrame; // <JIDE/>
    private CommandBar _mainToolBar;  // <JIDE/>
    private com.jidesoft.status.StatusBar _statusBar; // <JIDE/>
    private PropertyMap _preferences;
    private SuppressibleOptionPane _suppressibleOptionPane;
    private FileHistory _fileHistory;
    private boolean _debugEnabled;
    private MouseListener _mouseOverActionHandler;
    private ResourceBundle _resourceBundle;
    private boolean _startedUp;
    private boolean _startingUp;
    private boolean _shuttingDown;
    private ContainerListener _popupMenuListener;
    private Logger _logger;
    private Formatter _logFormatter;
    private ActionListener _closeHandler;

    // todo - move somewhere else
    public static final String MESSAGE_STATUS_BAR_ITEM_KEY = "Message";
    public static final String POSITION_STATUS_BAR_ITEM_KEY = "Position";
    public static final String TIME_STATUS_BAR_ITEM_KEY = "Time";
    /**
     * @deprecated since BEAM 4.2, no replacement
     */
    @Deprecated
    public static final String OVRINS_STATUS_BAR_ITEM_KEY = "Message";
    public static final String MEMORY_STATUS_BAR_ITEM_KEY = "Memory";
    private File _beamUserDir;
    private File _appUserDir;
    private boolean _frameBoundsRestored;

    static {
        FileChooserFactory.getInstance().setDirChooserClass(FolderChooser.class);
    }


    public BasicApp(ApplicationDescriptor applicationDescriptor) {
        Assert.notNull(applicationDescriptor, "applicationDescriptor");
        this.applicationDescriptor = applicationDescriptor;
        if (applicationDescriptor.getSymbolicName() != null) {
            BeamLogManager.setSystemLoggerName(applicationDescriptor.getSymbolicName());
        }
        _fileHistory = new FileHistory(10, "recent.files");
        _mouseOverActionHandler = new MouseOverActionHandler();
        _closeHandler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shutDown();
            }
        };
    }

    /**
     * Constructs a new application frame and creates the GUI.
     *
     * @param appName          the application base name, appears in the frame's title bar
     * @param appSymbolicName  the symbolic name of the application
     * @param appVersion       the version string
     * @param appCopyrightInfo the copyright information text
     * @param appResource      the resource path of the application's resource bundle, can be <code>null</code> if the
     *                         application does not use resource bundles
     * @param appLoggerName    the logger name for the application logging, can be <code>null</code> if the application
     *                         does not use logging
     * @see java.util.ResourceBundle
     */
    protected BasicApp(String appName,
                       String appSymbolicName,
                       String appVersion,
                       String appCopyrightInfo,
                       String appResource,
                       String appLoggerName) {
        this(createApplicationDescriptor(appName,
                                         appSymbolicName,
                                         appVersion,
                                         appCopyrightInfo,
                                         appResource,
                                         appLoggerName));
    }

    public ApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    /**
     * @return the handler used if the user moves the mouse  over a menu item or toolbar button
     */
    public MouseListener getMouseOverActionHandler() {
        return _mouseOverActionHandler;
    }

    public boolean isFrameBoundsRestored() {
        return _frameBoundsRestored;
    }

    /**
     * @return the application's status bar
     */
    public com.jidesoft.status.StatusBar getStatusBar() {
        return _statusBar;
    }

    /**
     * @return the application's main tool bar
     */
    public CommandBar getMainToolBar() {
        return _mainToolBar;
    }

    /**
     * @return the user's preferences file
     */
    public File getPreferencesFile() {
        return _preferencesFile;
    }


    /**
     * @return the application logger
     */
    public Logger getLogger() {
        return _logger;
    }

    public boolean isStartedUp() {
        return _startedUp;
    }

    public boolean isShuttingDown() {
        return _shuttingDown;
    }

    protected void setShuttingDown(boolean shuttingDown) {
        _shuttingDown = shuttingDown;
    }

    /**
     * Starts up the application.
     * <p/>
     * <p>The startup sequence is as follows: <ol> <li>The method <code>createSplashScreen</code> is called. If it
     * returns non-null</li> the splash screen is shown.</li> <li> The method <code>loadPreferences</code> is called in
     * order to load user preferences.</li> <li> The method <code>startUp</code> is called with the splash screen.
     * Clients can override this method in order to implement application specific initialisation code.</li> <li> The
     * method <code>applyPreferences</code> is called.</li> <li> The main frame is shown.</li> <li> The splash screen
     * (if any) is closed.</li> </ol>
     *
     * @param pm a progress monitor, e.g. for splash-screen
     * @throws Exception if an error occurs
     */
    public void startUp(ProgressMonitor pm) throws Exception {
        if (_startedUp || _startingUp) {
            throw new IllegalStateException("startUp");
        }

        _startingUp = true;

        try {
            pm.beginTask("Starting " + getAppName(), 6);

            pm.setSubTaskName("Loading preferences...");
            initLogger();
            initBeamUserDir();
            initResources();
            initPreferences();
            initLogging();
            logStartUpInfo();
            pm.worked(1);

            pm.setSubTaskName("Creating main frame...");
            initCommandManager();
            initMainFrame();
            initShutdownHook();
            initLookAndFeel();
            configureLayoutPersitence();
            configureDockingManager();
            pm.worked(1);

            initClient(SubProgressMonitor.create(pm, 1));

            pm.setSubTaskName("Initialising UI components...");
            configureCommandsByResourceBundle();
            initMainMenuBar();
            initMainToolBar();
            initMainPane();
            initStatusBar();
            initFrameIcon();
            pm.worked(1);

            initClientUI(SubProgressMonitor.create(pm, 1));

            pm.setSubTaskName("Applying UI preferences...");
            applyPreferences();
            getMainFrame().getLayoutPersistence().loadLayoutData(); // </JIDE>
            clearStatusBarMessage();
            pm.worked(1);

        } finally {
            pm.done();
        }

        try {
            getMainFrame().setVisible(true);
            updateState();
        } finally {
            _startedUp = true;
            _startingUp = false;
        }
    }

    private void logStartUpInfo() {
        _logger.info("BEAM user directory is '" + _beamUserDir + "'");    /*I18N*/
        if (_resourceBundle != null) {
            _logger.info("Resource bundle loaded from '" + applicationDescriptor.getResourceBundleName() + "'"); /*I18N*/
        }
        if (_preferencesFile != null) {
            _logger.info("User preferences loaded from '" + _preferencesFile.getPath() + "'");/*I18N*/
        }
    }

    private void initFrameIcon() {
        ImageIcon imageIcon = createFrameIcon();
        if (imageIcon != null) {
            getMainFrame().setIconImage(imageIcon.getImage());
        }
    }

    private void initStatusBar() {
        _statusBar = createStatusBar();
        if (_statusBar != null) {
            getMainFrame().getContentPane().add(BorderLayout.SOUTH, _statusBar);
        }
    }

    private void initMainPane() {
        JComponent mainPane = createMainPane();
        if (mainPane != null) {
            // <old-UI>
            //            getMainFrame().getContentPane().add(BorderLayout.CENTER, mainPane);
            // </old-UI>
            // <JIDE>
            getMainFrame().getDockingManager().getWorkspace().setLayout(new BorderLayout());
            getMainFrame().getDockingManager().getWorkspace().add(mainPane, BorderLayout.CENTER);
            getMainFrame().getDockingManager().setDefaultFocusComponent(mainPane);
            // </JIDE>
        }
    }

    private void initMainToolBar() {
        _mainToolBar = createMainToolBar();
        if (_mainToolBar != null) {
            // <old-UI>
            //            getMainFrame().getContentPane().add(BorderLayout.NORTH, _mainToolBar);
            // </old-UI>
            // <JIDE>
            _mainToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            _mainToolBar.getContext().setInitIndex(1);
            _mainToolBar.getContext().setInitSubindex(0);
            getMainFrame().getDockableBarManager().addDockableBar(_mainToolBar);
            // </JIDE>
        }
    }

    private void initMainMenuBar() {
        CommandBar menuBar = createMainMenuBar();
        if (menuBar != null) {
            // <old-UI>
            //            getMainFrame().setJMenuBar(menuBar);
            // </old-UI>
            // <JIDE>
            menuBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            menuBar.getContext().setInitIndex(0);
            menuBar.getContext().setInitSubindex(0);
            getMainFrame().getDockableBarManager().addDockableBar(menuBar);
            // </JIDE>

            insertCommandMenuItems();
        }
    }

    private void configureDockingManager() {
        getMainFrame().getDockingManager().setProfileKey(getAppName());
        getMainFrame().getDockingManager().setInitBounds(new Rectangle(0, 0, 960, 800));
        getMainFrame().getDockingManager().setInitSplitPriority(DefaultDockingManager.SPLIT_SOUTH_NORTH_EAST_WEST);
        getMainFrame().getDockingManager().setInitDelay(100);
        getMainFrame().getDockingManager().setSteps(1);
        getMainFrame().getDockingManager().setStepDelay(0);
        getMainFrame().getDockingManager().setUndoLimit(0);
        getMainFrame().getDockingManager().setFloatable(true);
        getMainFrame().getDockingManager().setShowGripper(false);
        getMainFrame().getDockingManager().setDragGripperOnly(false);
        getMainFrame().getDockingManager().setContinuousLayout(true);
        getMainFrame().getDockingManager().setAutoDockingAsDefault(false);
        getMainFrame().getDockingManager().setHideFloatingFramesWhenDeactivate(true);
        getMainFrame().getDockingManager().setHideFloatingFramesOnSwitchOutOfApplication(true);
//        getMainFrame().getDockingManager().setOutlineMode(DockingManager.PARTIAL_OUTLINE_MODE);
//        getMainFrame().getDockingManager().setOutlineMode(DockingManager.MIX_OUTLINE_MODE);
        getMainFrame().getDockingManager().setOutlineMode(DockingManager.FULL_OUTLINE_MODE);
    }

    private void configureLayoutPersitence() {
        getMainFrame().getLayoutPersistence().setProfileKey(getAppName());
        getMainFrame().getLayoutPersistence().setUsePref(false);
        getMainFrame().getLayoutPersistence().setLayoutDirectory(_appUserDir.getPath());
        getMainFrame().getLayoutPersistence().beginLoadLayoutData();
        getMainFrame().getDockableBarManager().setProfileKey(getAppName());
    }

    private void initCommandManager() {
        _commandManager = new DefaultCommandManager();
        _commandUIFactory = new DefaultCommandUIFactory();
        _commandUIFactory.setCommandManager(_commandManager);
    }

    private void initMainFrame() {
        _mainFrame = new MainFrame();
        _mainFrame.setTitle(getMainFrameTitle());
        _mainFrame.setName("mainFrame" + getAppName());
        _mainFrame.setDefaultCloseOperation(MainFrame.DO_NOTHING_ON_CLOSE);
        _mainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                Debug.trace("BasicApp: application main frame is closing, calling exit handler...");
                _closeHandler.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "close"));
            }
        });
    }

    public String getAppCopyright() {
        return applicationDescriptor.getCopyright();
    }

    private void initLogger() {
        _logFormatter = BeamLogManager.createFormatter(getAppName(), getAppVersion(), getAppCopyright());
        BeamLogManager.configureSystemLogger(_logFormatter, false);
        _logger = BeamLogManager.getSystemLogger();
    }

    private boolean initLookAndFeel() {
        String currentLafClassName = UIManager.getLookAndFeel().getClass().getName();
        String newLafClassName = getPreferences().getPropertyString(PROPERTY_KEY_APP_UI_LAF,
                                                                    UIManager.getSystemLookAndFeelClassName());
        if (!_uiDefaultsInitialized || !currentLafClassName.equals(newLafClassName)) {
            try {
                UIManager.setLookAndFeel(newLafClassName);
                LookAndFeelFactory.installJideExtension(LookAndFeelFactory.XERTO_STYLE);
                getPreferences().setPropertyString(PROPERTY_KEY_APP_UI_LAF, newLafClassName);
                // Uncomment this, if we want icons to be displayed on title pane of a DockableFrame
                // UIManager.getDefaults().put("DockableFrameTitlePane.showIcon", Boolean.TRUE);
                return true;
            } catch (Throwable e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Called by the <code>exit</code> method. The default implementation first saves the user preferences.
     * You should not call this method directly. If you override this method, make sure to call the base
     * class version as well.
     *
     * @see #savePreferences()
     */
    protected void handleImminentExit() {
        Debug.trace(getAppName() + ": handleImminentExit entered");
        if (!unexpectedShutdown) {  // fix BEAM-712 (nf 2007.11.02)
            Debug.trace("(1)");
            LayoutPersistence layoutPersistence = getMainFrame().getLayoutPersistence();
            if (layoutPersistence != null) {
                layoutPersistence.saveLayoutData(); // produces a dead lock, method is certainly not threadsafe!
            }
        }
        Debug.trace("(2)");
        savePreferences();
        Debug.trace("(3)");
        HelpSys.dispose(); // todo - disposing the HelpSystem should be done in the BeamUiActivator.stop() method
        Debug.trace(getAppName() + ": handleImminentExit exited");
    }


    private boolean unexpectedShutdown;  // fix BEAM-712 (nf 2007.11.02)

    private void initShutdownHook() {

        Thread shutdownHook = new Thread(getAppName() + " shut-down hook") {

            @Override
            public void run() {
                if (isStartedUp() && !isShuttingDown()) {
                    unexpectedShutdown = true; // fix BEAM-712 (nf 2007.11.02)
                    _logger.severe("Unexpectedly shutting down " + getAppName());/*I18N*/
                    handleImminentExit();
                } else {
                    _logger.severe("Nominally shutting down " + getAppName());/*I18N*/
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * @return the tool bar for this application.
     */
    protected CommandBar createMainToolBar() {
        return null;
    }


    /**
     * @return the main pane for this application.
     */
    protected JComponent createMainPane() {
        return new JPanel();
    }

    /**
     * @return the menu bar for this application.
     */
    protected CommandBar createMainMenuBar() {
        return null;
    }

    /**
     * @return a standard status bar for this application.
     */
    protected com.jidesoft.status.StatusBar createStatusBar() {
        return null;
    }

    /**
     * Creates a default frame icon for this application.
     * <p/> Override this method if you want another behaviour.
     *
     * @return the frame icon, or <code>null</code> if no icon is used
     */
    protected ImageIcon createFrameIcon() {
        final String path = applicationDescriptor.getFrameIconPath();
        if (path == null) {
            return null;
        }
        URL iconURL = getClass().getResource(path);
        if (iconURL == null) {
            return null;
        }
        return new ImageIcon(iconURL);
    }

    /**
     * Starts up the client application.
     * <p/>Called from {@link #startUp(com.bc.ceres.core.ProgressMonitor)} before
     * {@link #initClientUI(com.bc.ceres.core.ProgressMonitor)} is called.
     * <p/>Clients should override this method in order to initialize non-UI components, e.g. load plugins.
     * <p/>The default implementation does nothing.
     *
     * @param pm a progress monitor, can be used to signal progress
     * @throws Exception if an error occures
     */
    protected void initClient(ProgressMonitor pm) throws Exception {
    }

    /**
     * Initializes the client user interface.
     * <p/>Called from {@link #startUp(com.bc.ceres.core.ProgressMonitor)} after
     * {@link #initClient(com.bc.ceres.core.ProgressMonitor)} is called.
     * <p/>Clients should override this method in order to initialize their UI components.
     * <p/>The default implementation does nothing.
     *
     * @param pm a progress monitor, can be used to signal progress
     * @throws Exception if an error occures
     */
    protected void initClientUI(ProgressMonitor pm) throws Exception {
    }

    public MainFrame getMainFrame() {
        return _mainFrame;
    }

    public boolean isToolBarVisible(String toolbarKey) {
        DockableBarManager barManager = getMainFrame().getDockableBarManager();
        if (barManager != null) {
            DockableBar dockableBar = barManager.getDockableBar(toolbarKey);
            return (dockableBar != null) && !dockableBar.isHidden();
        }
        return false;
    }

    public void setToolBarVisible(boolean visbile, String toolBarKey) {
        DockableBarManager dockableBarManager = getMainFrame().getDockableBarManager();
        if (visbile) {
            dockableBarManager.showDockableBar(toolBarKey);
        } else {
            dockableBarManager.hideDockableBar(toolBarKey);
        }
    }

    public boolean isStatusBarVisible() {
        return (_statusBar != null) && _statusBar.isVisible();
    }

    public void setStatusBarVisible(boolean visible) {
        if (_statusBar != null) {
            _statusBar.setVisible(visible);
        }
    }

    /**
     * @return The (display) name of this application.
     */
    public String getAppName() {
        return applicationDescriptor.getDisplayName();
    }

    /**
     * @return The symbolic name of this application.
     */
    public String getAppSymbolicName() {
        return applicationDescriptor.getSymbolicName();
    }

    /**
     * @return The version string of this application.
     */
    public String getAppVersion() {
        return applicationDescriptor.getVersion();
    }

    /**
     * Returns the resource bundle used by this application.
     *
     * @return the resource bundle, or <code>null</code> if a bundle does not exist
     */
    public ResourceBundle getResourceBundle() {
        return _resourceBundle;
    }

    /**
     * @return The path where application images such as toolbar icons are stored.
     *         The returned path is relative to this
     *         application's class path.
     */
    public String getImageResourcePath() {
        return _IMAGE_RESOURCE_PATH;
    }

    /**
     * @return The <code>LabelStatusBarItem</code> component used for displaying status messages.
     *         <p> If the application does notr have
     *         a status bar, <code>null</code> should be returned.
     *         <p> The default implementation searches for a label the
     *         status bar and returns it.
     */
    private LabelStatusBarItem getStatusBarLabel() {
        if (_statusBar != null) {
            return (LabelStatusBarItem) _statusBar.getItemByName(MESSAGE_STATUS_BAR_ITEM_KEY);
        }
        return null;
    }

    /**
     * Returns the command manager used for this application. In order to use another command manager than the default
     * manager, override this method to return an alternative command manager, or call <code>setCommandManager</code>
     * with your alternative implementation.
     *
     * @return the command manager, never <code>null</code>
     */
    public CommandManager getCommandManager() {
        return _commandManager;
    }

    /**
     * Sets the command manager to be used for this application.
     *
     * @param commandManager he command manager, must not be <code>null</code>
     */
    public void setCommandManager(CommandManager commandManager) {
        Guardian.assertNotNull("commandManager", commandManager);
        _commandManager = commandManager;
        _commandUIFactory.setCommandManager(commandManager);
    }

    public CommandUIFactory getCommandUIFactory() {
        return _commandUIFactory;
    }

    /**
     * Updates the appication's (UI-) state. The default implementation calls <code>updateState</code> on each
     * registered command.
     */
    public final void updateState() {
        Debug.trace("BasicApp: updating application state...");
        _commandManager.updateState();
    }

    /**
     * Sets the status bar message to the given message string.
     *
     * @param message the message to display
     */
    public final void setStatusBarMessage(String message) {
        LabelStatusBarItem label = getStatusBarLabel();
        if (label != null && message != null) {
            label.setText(message);
        }
    }

    /**
     * Clears the status bar message. Simply calls <code>setStatusBarMessage(&quot; Ready.&quot;)</code>.
     */
    public final void clearStatusBarMessage() {
        setStatusBarMessage("Ready."); /*I18N*/
    }

    /**
     * @return The current status bar message string.
     */
    public final String getStatusBarMessage() {
        LabelStatusBarItem label = getStatusBarLabel();
        if (label != null) {
            return label.getText();
        }
        return null;
    }

    public final JMenu findMainMenu(String name) {
        return findMenu(name, false);
    }

    public final JMenu findMenu(String name) {
        return findMenu(name, true);
    }

    public final String[] getToolBarGroups() {
        return new String[]{"file", "edit", "view", null, "tools", "help"};
    }

    protected final void insertCommandToolBarButtons(JToolBar toolbar) {

        String[] toolBarGroups = getToolBarGroups();
        if (toolBarGroups == null) {
            return;
        }

        boolean toolAdded = false;
        for (String toolBarGroup : toolBarGroups) {
            if (toolAdded) {
                toolbar.addSeparator();
                toolAdded = false;
            }
            for (int j = 0; j < _commandManager.getNumCommands(); j++) {
                Command command = _commandManager.getCommandAt(j);
                // @todo 2 nf/nf - ask whether location=toolbar or not
                if (command instanceof ExecCommand && command.getLargeIcon() != null) {
                    String rootParent = findRootParent(command);
                    if ((rootParent == null
                            && toolBarGroup == null)
                            || (rootParent != null
                            && rootParent.equalsIgnoreCase(toolBarGroup))) {
                        AbstractButton button = command.createToolBarButton();
                        if (button != null) {
                            toolbar.add(button);
                        }
                        toolAdded = true;
                    }
                }
            }
        }
    }

     // The 'root' parent name of the given command.
    private String findRootParent(Command command) {
        String parent = command.getParent();
        if (parent != null) {
            CommandGroup commandGroup = _commandManager.getCommandGroup(parent);
            if (commandGroup != null) {
                return findRootParent(commandGroup);
            }
        }
        return parent;
    }


    protected final void insertCommandMenuItems() {
        for (int i = 0; i < _commandManager.getNumCommands(); i++) {
            insertCommandMenuItem(_commandManager.getCommandAt(i));
        }
    }

    protected final void configureCommandsByResourceBundle() {
        ResourceBundle resourceBundle = getResourceBundle();
        if (resourceBundle == null) {
            return;
        }
        for (int i = 0; i < _commandManager.getNumCommands(); i++) {
            _commandManager.getCommandAt(i).configure(resourceBundle);
        }
    }

    protected final void insertCommandMenuItem(Command command) {
        JMenu menu = null;
        Object parent = command.getParent();
        if (parent != null) {
            menu = findMenu(parent.toString());
        }
        if (menu == null) {
            menu = findMainMenu("tools"); // @todo 3 nf/nf - "tools" = getDefaultMenuName()
        }
        if (menu != null) {
            CommandMenuUtils.insertCommandMenuItem(menu.getPopupMenu(), command, getCommandManager());
        }
    }

    protected final JMenu findMenu(String name, boolean deepSearch) {
        JMenuBar menuBar = getMainFrame().getJMenuBar();
        if (menuBar == null) {
            return null;
        }
        return UIUtils.findMenu(menuBar, name, deepSearch);
    }

    protected final JMenu createJMenu(String name, String text, char mnemonic) {
        JideMenu menu = new JideMenu();  // <JIDE>
        menu.setName(name); /*I18N*/
        menu.setText(text);
        menu.setMnemonic(mnemonic);
        menu.getPopupMenu().addContainerListener(getOrCreatePopupMenuListener());
        return menu;
    }

    private ContainerListener getOrCreatePopupMenuListener() {
        if (_popupMenuListener == null) {
            _popupMenuListener = new ContainerListener() {
                public void componentAdded(ContainerEvent e) {
                    Component component = e.getChild();
                    component.addMouseListener(getMouseOverActionHandler());
                }

                public void componentRemoved(ContainerEvent e) {
                    Component component = e.getChild();
                    component.removeMouseListener(getMouseOverActionHandler());
                }
            };
        }
        return _popupMenuListener;
    }


    /**
     * Exits the application. <p>The base class implementation calls <code>setShuttingDown(true)</code>, calls
     * {@link #handleImminentExit()} and disposes the main frame.
     * <p/>
     * <p> When this method is overridden, this base class's implementation should always be called last. However, the
     * preferred way to change the shutdown behaviour is to override the {@link #handleImminentExit()} method.
     */
    public void shutDown() {
        setShuttingDown(true);
        handleImminentExit();
        getMainFrame().dispose();
        System.exit(0);
    }

    /////////////////////////////////////////////////////////////////////////
    // User Preferences Support

    protected final void loadPreferences() {

        if (_preferencesFile == null) {
            return;
        }

        // Note, at this point the logging file is still not open!
        try {
            getPreferences().load(_preferencesFile);
        } catch (IOException e) {
            _logger.warning("Failed to load user preferences from " + _preferencesFile);
            _logger.warning("Using application default values...");
        }
    }

    protected final void savePreferences() {

        if (_preferencesFile == null) {
            return;
        }

        setPreferences();

        try {
            _logger.info("Storing user preferences in '" + _preferencesFile.getPath() + "'...");/*I18N*/
            getPreferences().store(_preferencesFile, getAppName() + " " + getAppVersion() + " - User preferences file");
            _logger.info("User preferences stored");/*I18N*/
        } catch (IOException e) {
            _logger.warning("Failed to store user preferences");/*I18N*/
        }
    }

    protected final void setPreferences() {

        //////////////////////////////////////////////////////////////
        // Store file history

        _fileHistory.copyInto(getPreferences());

        //////////////////////////////////////////////////////////////
        // Store frame properties

        getPreferences().setPropertyInt("frame.location.x", getMainFrame().getLocation().x);
        getPreferences().setPropertyInt("frame.location.y", getMainFrame().getLocation().y);
        getPreferences().setPropertyInt("frame.size.width", getMainFrame().getSize().width);
        getPreferences().setPropertyInt("frame.size.height", getMainFrame().getSize().height);

        getPreferences().setPropertyBool("frame.ui.dblbuf",
                                         RepaintManager.currentManager(getMainFrame()).isDoubleBufferingEnabled());

        getPreferences().setPropertyString("frame.ui.laf",
                                           UIManager.getLookAndFeel().getClass().getName());

        //////////////////////////////////////////////////////////////

        getPreferences().setPropertyBool(PROPERTY_KEY_APP_DEBUG_ENABLED, _debugEnabled);
    }


    protected void applyPreferences() {

        Boolean bV;

        ExecCommand command;

        _logger.info("Applying user preferences...");  /*I18N*/

        //////////////////////////////////////////////////////////////
        // Set frame bounds

        int x = getPreferences().getPropertyInt("frame.location.x", -1);
        int y = getPreferences().getPropertyInt("frame.location.y", -1);
        int width = getPreferences().getPropertyInt("frame.size.width", 0);
        int height = getPreferences().getPropertyInt("frame.size.height", 0);

        _frameBoundsRestored = false;
        if (x >= 0 && y >= 0 && width > 0 && height > 0) {
            getMainFrame().setBounds(x, y, width, height);
            _frameBoundsRestored = true;
        }

        //////////////////////////////////////////////////////////////
        // Set menu bar visibility

        bV = getPreferences().getPropertyBool("view.showToolBar", Boolean.TRUE);
        command = _commandManager.getExecCommand("showToolBar");
        if (command != null) {
            command.setSelected(bV);
        }

        //////////////////////////////////////////////////////////////
        // Set status bar visibility

        bV = getPreferences().getPropertyBool("view.showStatusBar", Boolean.TRUE);
        command = _commandManager.getExecCommand("showStatusBar");
        if (command != null) {
            command.setSelected(bV);
        }

        //////////////////////////////////////////////////////////////
        // Initialize file history

        _fileHistory.initBy(getPreferences());

        //////////////////////////////////////////////////////////////

        bV = getPreferences().getPropertyBool(PROPERTY_KEY_APP_DEBUG_ENABLED, Boolean.FALSE);
        _debugEnabled = bV;
        if (_debugEnabled) {
            Debug.setEnabled(true);
        }

        //////////////////////////////////////////////////////////////

        applyLookAndFeelPreferences();

        //////////////////////////////////////////////////////////////

        _logger.info("User preferences applied");/*I18N*/
    }

    /**
     * This method should be called after prefernces that affect the current look & feel have changed.
     */
    protected final void applyLookAndFeelPreferences() {
        boolean startingUp = !isStartedUp();
        // Don't reset look-and-feel if already done on start-up!
        if (startingUp && _uiDefaultsInitialized) {
            return;
        }

        boolean mustUpdateComponentTreeUI = initLookAndFeel();

        final UIDefaults uiDefaults = UIManager.getLookAndFeel().getDefaults();
        // Don't remove this out-commented code, its useful to find out default UI key/value pairs
        //        Enumeration enum = uiDefaults.keys();
        //        while (enum.hasMoreElements()) {
        //            Object key = enum.nextElement();
        //            Object val = uiDefaults.get(key);
        //            System.out.println("[\"" + key.toString() + "\"] : [" + (null != val ? val.toString() : "(null)") + "]");
        //        }
        final boolean currentUseSystemFontSettings = uiDefaults.getBoolean("Application.useSystemFontSettings");
        final boolean useSystemFontSettings = getPreferences().getPropertyBool(
                PROPERTY_KEY_APP_UI_USE_SYSTEM_FONT_SETTINGS, currentUseSystemFontSettings);
        if (currentUseSystemFontSettings != useSystemFontSettings) {
            uiDefaults.put("Application.useSystemFontSettings", useSystemFontSettings);
            mustUpdateComponentTreeUI = true;
        }

        if (!useSystemFontSettings) {
            final Font currentMenuFont = uiDefaults.getFont("Menu.font");
            final String fontName = getPreferences().getPropertyString(PROPERTY_KEY_APP_UI_FONT_NAME,
                                                                       currentMenuFont.getName());
            final int fontSize = getPreferences().getPropertyInt(PROPERTY_KEY_APP_UI_FONT_SIZE,
                                                                 currentMenuFont.getSize());
            if (!currentMenuFont.getName().equalsIgnoreCase(fontName) || currentMenuFont.getSize() != fontSize) {
                changeUIDefaultsFonts(uiDefaults, fontName, fontSize);
                mustUpdateComponentTreeUI = true;
            }
        }

        if (mustUpdateComponentTreeUI && getMainFrame().isVisible()) {
            updateComponentTreeUI();
        }

        _uiDefaultsInitialized = true;
    }

    private void changeUIDefaultsFonts(final UIDefaults uiDefaults, final String fontName, final int fontSize) {

        final String[] smallFontKeys = new String[]{
                "ToolTip.font",
                "Menu.acceleratorFont",
                "MenuItem.acceleratorFont",
                "CheckBoxMenuItem.acceleratorFont",
                "RadioButtonMenuItem.acceleratorFont",
        };

        final String[] plainFontKeys = new String[]{
                "ToolBar.font",
                "MenuBar.font",
                "Menu.font",
                "MenuItem.font",
                "CheckBoxMenuItem.font",
                "RadioButtonMenuItem.font",
                "PopupMenu.font",

                "ComboBox.font",
                "List.font",
                "Tree.font",
                "Table.font",
                "TableHeader.font",

                "Button.font",
                "ToggleButton.font",
                "RadioButton.font",

                "Panel.font",
                "ScrollPane.font",
                "Viewport.font",
                "TabbedPane.font",
                "ProgressBar.font",
                "Spinner.font",

                "TitledBorder.font",
                "Label.font",
                "CheckBox.font",

                "EditorPane.font",
                "TextPane.font",
                "TextField.font",
                "FormattedTextField.font",
                "PasswordField.font",
                "TextArea.font",

                "FileChooser.listFont",
                "ColorChooser.font",
                "OptionPane.font",
                "OptionPane.buttonFont",
                "OptionPane.messageFont",
        };

        final FontUIResource plainFont = new FontUIResource(new Font(fontName, Font.PLAIN, fontSize));
        for (String plainFontKey : plainFontKeys) {
            uiDefaults.put(plainFontKey, plainFont);
        }

        final FontUIResource smallFont = new FontUIResource(plainFont.deriveFont(0.8f * fontSize));
        for (String smallFontKey : smallFontKeys) {
            uiDefaults.put(smallFontKey, smallFont);
        }
    }

    /**
     * Called after the look & feel has changed. The method simply calls <code>SwingUtilities.updateComponentTreeUI(getMainFrame())</code>
     * in order to reflect changes of the look-and-feel.
     * <p/>
     * <p>You might want to override this method in order to call <code>SwingUtilities.updateComponentTreeUI()</code> on
     * other top-level containers beside the main frame.
     */
    protected void updateComponentTreeUI() {
        _mainFrame.getDockableBarManager().updateComponentTreeUI(); // <JIDE>
        _mainFrame.getDockingManager().updateComponentTreeUI(); // <JIDE>
        SwingUtilities.updateComponentTreeUI(getMainFrame());
    }

    /////////////////////////////////////////////////////////////////////////
    // Message Dialog Support

    /**
     * Sets the current document title which appears in this frame's title bar.
     *
     * @param currentDocTitle the title
     */
    public final void setCurrentDocTitle(String currentDocTitle) {
        final String mainFrameTitle = getMainFrameTitle();
        if (!StringUtils.isNullOrEmpty(currentDocTitle)) {
            getMainFrame().setTitle(currentDocTitle + " - " + mainFrameTitle);
        } else {
            getMainFrame().setTitle(mainFrameTitle);
        }
    }

    /**
     * Centers this application frame within the screen area.
     */
    public final void center() {
        UIUtils.centerComponent(getMainFrame());
    }

    /////////////////////////////////////////////////////////////////////////
    // File Dialog Support

    /**
     * Opens a standard file-open dialog box.
     *
     * @param title      a dialog-box title
     * @param dirsOnly   whether or not to select only directories
     * @param fileFilter the file filter to be used, can be <code>null</code>
     * @return the file selected by the user or <code>null</code> if the user canceled file selection
     */
    public final File showFileOpenDialog(String title, boolean dirsOnly, FileFilter fileFilter) {
        return showFileOpenDialog(title, dirsOnly, fileFilter, PROPERTY_KEY_APP_LAST_OPEN_DIR);
    }

    /**
     * Opens a standard file-open dialog box.
     *
     * @param title              a dialog-box title
     * @param dirsOnly           whether or not to select only directories
     * @param fileFilter         the file filter to be used, can be <code>null</code>
     * @param lastDirPropertyKey the key under which the last directory the user visited is stored
     * @return the file selected by the user or <code>null</code> if the user canceled file selection
     */
    public final File showFileOpenDialog(String title, boolean dirsOnly, FileFilter fileFilter,
                                         String lastDirPropertyKey) {

        Guardian.assertNotNullOrEmpty("lastDirPropertyKey", lastDirPropertyKey);
        String lastDir = getPreferences().getPropertyString(lastDirPropertyKey,
                                                            SystemUtils.getUserHomeDir().getPath());
        File currentDir = new File(lastDir);

        BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(getAppName() + " - " + title);
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            // todo replace getAbsolutPath() by getPath()?
            String lastDirPath = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (lastDirPath != null) {
                getPreferences().setPropertyString(lastDirPropertyKey, lastDirPath);
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                return null;
            }
            // to replace getAbsolutPath() replaced by getPath()?
            file = file.getAbsoluteFile();
            historyPush(file);
            return file;
        }
        return null;
    }

    /**
     * Opens a standard file-safe dialog box.
     *
     * @param title            a dialog-box title
     * @param dirsOnly         whether or not to select only directories
     * @param fileFilter       the file filter to be used, can be <code>null</code>
     * @param defaultExtension the extension used as default
     * @param fileName         the initial filename
     * @return the file selected by the user or <code>null</code> if the user canceled file selection
     */
    public final File showFileSaveDialog(String title,
                                         boolean dirsOnly,
                                         FileFilter fileFilter,
                                         String defaultExtension,
                                         final String fileName) {
        return showFileSaveDialog(title,
                                  dirsOnly,
                                  fileFilter,
                                  defaultExtension,
                                  fileName,
                                  PROPERTY_KEY_APP_LAST_SAVE_DIR);
    }

    /**
     * Opens a standard file-safe dialog box.
     *
     * @param title              a dialog-box title
     * @param dirsOnly           whether or not to select only directories
     * @param fileFilter         the file filter to be used, can be <code>null</code>
     * @param defaultExtension   the extension used as default
     * @param fileName           the initial filename
     * @param lastDirPropertyKey the key under which the last directory the user visited is stored
     * @return the file selected by the user or <code>null</code> if the user canceled file selection
     */
    public final File showFileSaveDialog(String title,
                                         boolean dirsOnly,
                                         FileFilter fileFilter,
                                         String defaultExtension,
                                         final String fileName,
                                         final String lastDirPropertyKey) {
        Guardian.assertNotNullOrEmpty("lastDirPropertyKey", lastDirPropertyKey);
        String lastDir = getPreferences().getPropertyString(lastDirPropertyKey,
                                                            SystemUtils.getUserHomeDir().getPath());
        File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        if (fileName != null) {
            fileChooser.setSelectedFile(new File(FileUtils.exchangeExtension(fileName, defaultExtension)));
        }
        fileChooser.setDialogTitle(getAppName() + " - " + title);
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);

        int result = fileChooser.showSaveDialog(getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            // to replace getAbsolutPath() replaced by getPath()?
            String lastDirPath = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (lastDirPath != null) {
                getPreferences().setPropertyString(lastDirPropertyKey, lastDirPath);
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                return null;
            }
            // to replace getAbsolutPath() replaced by getPath()?
            String absolutePath = file.getAbsolutePath();
            if (defaultExtension != null) {
                if (!absolutePath.toLowerCase().endsWith(defaultExtension.toLowerCase())) {
                    absolutePath = absolutePath.concat(defaultExtension);
                }
            }
            file = new File(absolutePath);
            historyPush(file);
            return file;
        }
        return null;
    }

    protected void historyPush(File file) {
        // to replace getAbsolutPath() replaced by getPath()?
        _fileHistory.push(file.getAbsolutePath());
    }

    /////////////////////////////////////////////////////////////////////////
    // Job management (used for SwingWorker and  ObservableTask instances)

    /**
     * Registers a job (any job-like object). The application will not exit, unless all registered jobs have been
     * deregistered using the <code>{@link #unregisterJob}</code> method.
     *
     * @param job any job-like object
     */
    public final synchronized void registerJob(Object job) {

    }

    /**
     * Deregisters a job (any job-like object). The application will not exit, unless all job registrations using the
     * <code>{@link #registerJob}</code> method have been deregistered.
     *
     * @param job any job-like object
     */
    public final synchronized void unregisterJob(Object job) {
    }

    /////////////////////////////////////////////////////////////////////////
    // General error handling

    public final void handleUnknownException(Throwable e) {
        Debug.trace(e);

        String message;
        if (e == null) {
            message = "An unknown error occured."; /*I18N*/
        } else if (e.getMessage() == null) {
            message = "An exception occured:\n"
                    + "   Type: " + e.getClass().getName() + "\n"
                    + "   No message text available."; /*I18N*/
        } else {
            message = "An exception occured:\n"
                    + "   Type: " + e.getClass().getName() + "\n"
                    + "   Message: " + e.getMessage(); /*I18N*/
        }

        getMainFrame().setCursor(Cursor.getDefaultCursor());
        setStatusBarMessage("Error.");
        _logger.severe(message);
        showErrorDialog("Error", message);
        clearStatusBarMessage();
    }

/////////////////////////////////////////////////////////////////////////
// Message Dialog Support

    public final SuppressibleOptionPane getSuppressibleOptionPane() {
        return _suppressibleOptionPane;
    }

    public final void showErrorDialog(String message) {
        showErrorDialog("Error", message);
    }

    public final void showErrorDialog(String title, String message) {
        showMessageDialog(title, message, JOptionPane.ERROR_MESSAGE, null);
    }

    public final void showWarningDialog(String message) {
        showMessageDialog("Warning", message, JOptionPane.WARNING_MESSAGE, null); /*I18N*/
    }

    public final void showWarningDialog(String title, String message) {
        showMessageDialog(title, message, JOptionPane.WARNING_MESSAGE, null);
    }

    public final void showInfoDialog(String message, String preferencesKey) {
        showInfoDialog("Information", message, preferencesKey); /*I18N*/
    }

    public final void showInfoDialog(String title, String message, String preferencesKey) {
        showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE, preferencesKey);
    }

    public final void showOutOfMemoryErrorDialog(String message) {
        showErrorDialog("Out of Memory",
                        getAppName() + " is out of memory.\n" +
                                message + "\n\n" +
                                "You can try to release memory by closing products or image views which\n" +
                                "you currently not really need.\n" +
                                "If this does not help, you can increase the amount of virtual memory\n" +
                                "as described on the BEAM website at http://envisat.esa.int/services/beam/.");
    }

    public final void showMessageDialog(String title, String message, int messageType, String preferencesKey) {
        if (_suppressibleOptionPane != null && !StringUtils.isNullOrEmpty(preferencesKey)) {
            _suppressibleOptionPane.showMessageDialog(preferencesKey, getMainFrame(),
                                                      message,
                                                      getAppName() + " - " + title,
                                                      messageType);
        } else {
            JOptionPane.showMessageDialog(getMainFrame(),
                                          message,
                                          getAppName() + " - " + title,
                                          messageType);
        }
    }

    public final int showQuestionDialog(String message, String preferencesKey) {
        return showQuestionDialog("Question", message, preferencesKey); /*I18N*/
    }

    public final int showQuestionDialog(String title, String message, String preferencesKey) {
        return showQuestionDialog(title, message, false, preferencesKey);
    }

    public final int showQuestionDialog(String title, String message, boolean allowCancel, String preferencesKey) {
        if (_suppressibleOptionPane != null && !StringUtils.isNullOrEmpty(preferencesKey)) {
            return _suppressibleOptionPane.showConfirmDialog(preferencesKey,
                                                             getMainFrame(),
                                                             message,
                                                             getAppName() + " - " + title,
                                                             allowCancel
                                                                     ? JOptionPane.YES_NO_CANCEL_OPTION
                                                                     : JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.QUESTION_MESSAGE);
        } else {
            return JOptionPane.showConfirmDialog(getMainFrame(),
                                                 message,
                                                 getAppName() + " - " + title,
                                                 allowCancel
                                                         ? JOptionPane.YES_NO_CANCEL_OPTION
                                                         : JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);
        }
    }

    public final void showWarningsDialog(String message, String[] warnings) {
        if (warnings != null && warnings.length > 0) {
            JPanel messagePanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            String[] messageStrings = StringUtils.toStringArray(message, "\n");
            for (int i = 0; i < messageStrings.length; i++) {
                gbc.gridy = i + 1;
                messagePanel.add(new JLabel(messageStrings[i]), gbc);
            }

            JTextArea ta = new JTextArea();
            for (String warning : warnings) {
                ta.append(warning + "\n");
            }
            final JScrollPane details = new JScrollPane(ta);
            details.setPreferredSize(new Dimension(400, 150));
            details.setVisible(false);

            final JButton detailsButton = new JButton();
            detailsButton.setText("Show Details");
            detailsButton.setMnemonic('S');
            detailsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    details.setVisible(!details.isVisible());
                    if (details.isVisible()) {
                        detailsButton.setText("Hide Details");
                        detailsButton.setMnemonic('H');
                    } else {
                        detailsButton.setText("Show Details");
                        detailsButton.setMnemonic('S');
                    }
                    Window windowAncestor = SwingUtilities.getWindowAncestor(detailsButton);
                    if (windowAncestor != null) {
                        windowAncestor.pack();
                    }
                }
            });
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(detailsButton, BorderLayout.EAST);

            JPanel messageMainPanel = new JPanel(new BorderLayout(0, 5));
            messageMainPanel.add(messagePanel, BorderLayout.NORTH);
            messageMainPanel.add(buttonPanel, BorderLayout.CENTER);
            messageMainPanel.add(details, BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(getMainFrame(), messageMainPanel, "Warning", JOptionPane.WARNING_MESSAGE);
        } else {
            showWarningDialog(message);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Logging Support

    /**
     * Writes the application specific logging file header.
     */
    protected final void logAppInfo() {
    }

    public final void initLogging() {
        BeamLogManager.removeRootLoggerHandlers();
        if (getAppSymbolicName() != null) {
            if (!getPreferences().getProperties().containsKey(PROPERTY_KEY_APP_LOG_ENABLED)) {
                getPreferences().setPropertyBool(PROPERTY_KEY_APP_LOG_ENABLED, false);
            }
            final boolean logEnabled = getPreferences().getPropertyBool(PROPERTY_KEY_APP_LOG_ENABLED);
            if (logEnabled) {
                final String defaultLogPrefix = FileUtils.createValidFilename(getAppName().toLowerCase());
                String logPrefix = getPreferences().getPropertyString(PROPERTY_KEY_APP_LOG_PREFIX, defaultLogPrefix);
                String logLevelStr = getPreferences().getPropertyString(PROPERTY_KEY_APP_LOG_LEVEL,
                                                                        SystemUtils.LLS_INFO);
                int logLevel = SystemUtils.getLogLevel(logLevelStr);
                boolean echoOn = getPreferences().getPropertyBool(PROPERTY_KEY_APP_LOG_ECHO, Boolean.FALSE);
                registerSystemLogHandler(logPrefix, logLevel, echoOn);
                if (logLevel == SystemUtils.LL_DEBUG) {
                    Debug.setEnabled(true);
                    Debug.setLogging(true);
                }
            }
        }
    }

    public final void registerSystemLogHandler(String logPrefix, int logLevel, boolean echoOn) {
        if (logPrefix != null) {
            String logPattern = BeamLogManager.getLogFilePattern(logPrefix);
            CacheHandler cacheHandler = BeamLogManager.getRegisteredCacheHandler();
            try {
                BeamLogManager.ensureLogPathFromPatternExists(logPattern);
                Handler handler = new FileHandler(logPattern);
                handler.setFormatter(_logFormatter);
                if (cacheHandler != null) {
                    cacheHandler.transferRecords(handler);
                }
                _logger.addHandler(handler);
                _logger.setLevel(logLevel == SystemUtils.LL_DEBUG ? Level.FINEST :
                        logLevel == SystemUtils.LL_INFO ? Level.INFO :
                                logLevel == SystemUtils.LL_WARNING ? Level.WARNING :
                                        logLevel == SystemUtils.LL_ERROR ? Level.SEVERE :
                                                Level.INFO);
                getPreferences().setPropertyString(PROPERTY_KEY_APP_LOG_PREFIX, logPrefix);
                getPreferences().setPropertyString(PROPERTY_KEY_APP_LOG_LEVEL,
                                                   logLevel == SystemUtils.LL_DEBUG ? SystemUtils.LLS_DEBUG :
                                                           logLevel == SystemUtils.LL_ERROR ? SystemUtils.LLS_ERROR :
                                                                   logLevel == SystemUtils.LL_WARNING ? SystemUtils.LLS_WARNING : SystemUtils.LLS_INFO);
                getPreferences().setPropertyBool(PROPERTY_KEY_APP_DEBUG_ENABLED, Debug.isEnabled());
                getPreferences().setPropertyBool(PROPERTY_KEY_APP_LOG_ECHO, echoOn);
            } catch (SecurityException e) {
                System.err.println("error: failed to create log file " + logPattern + ": " + e.getMessage());
                System.err.println("logging will be redirected to console...");
                echoOn = true;
            } catch (IOException e) {
                System.err.println("error: failed to create log file " + logPattern + ": " + e.getMessage());
                System.err.println("logging will be redirected to console...");
                echoOn = true;
            }
            if (echoOn) {
                final ConsoleHandler handler = new ConsoleHandler();
                handler.setFormatter(_logFormatter);
                handler.setLevel(Level.FINE);
                if (cacheHandler != null) {
                    cacheHandler.transferRecords(handler);
                }
                _logger.addHandler(handler);
            }
        }
    }

    /**
     * Prompts a question dialog asking the user whether or not he/she wants to overwrite an existing file. If the given
     * file does not exists, the question dialog does not comes up.
     *
     * @param file the file to check for existance
     * @return <code>true</code> if the user confirmes the dialog with 'yes' or the given file does not exist.
     * @throws IllegalArgumentException if <code>file</code> is <code>null</code>
     */
    public final boolean promptForOverwrite(File file) {
        Guardian.assertNotNull("file", file);
        if (!file.exists()) {
            return true;
        }
        int answer = showQuestionDialog("File Exists",
                                        "The file\n"
                                                + "'" + file.getPath() + "'\n"
                                                + "already exists.\n\n"
                                                + "Do you really want to overwrite it?\n", null);
        return answer == JOptionPane.YES_OPTION;
    }

    public final PropertyMap getPreferences() {
        return _preferences;
    }

    public final FileHistory getFileHistory() {
        return _fileHistory;
    }

    public ActionListener getCloseHandler() {
        return _closeHandler;
    }

    public void setCloseHandler(ActionListener closeHandler) {
        Guardian.assertNotNull("closeHandler", closeHandler);
        _closeHandler = closeHandler;
    }

    private String getMainFrameTitle() {
        return getAppName() + " " + getAppVersion();
    }

//    private void initHelpSystem(SplashScreen splashScreen) {
//        if (HelpSys.isInitialized()) {
//            return;
//        }
//        if (_appHelpsetPath != null && _appHelpsetPath.length() > 0) {
//            if (splashScreen != null) {
//                splashScreen.setMessage("Initializing application help...");
//            }
//            HelpSys.init(_appHelpsetPath);
//        }
//        JFrame frame = getMainFrame();
//        if (frame != null) {
//            HelpSys.enableHelpKey(frame, "top");
//        }
//    }

    /**
     * Adds a new JavaHelp {@link javax.help.HelpSet} to the existing help.
     * The helpset is as a resource path to the JavaHelp helpset XML file (*.hs).
     * The helpset and associated resources must be accessible by the given class-loader.
     * <p/>
     * Note that you also can add help-set instances directly to the BEAM help system by using the
     * static {@link  HelpSys#add(javax.help.HelpSet)} method.
     * </p>
     * <p/>
     * For more information on the JavaHelp architecture and API please refer to
     * the <a href="http://java.sun.com/products/javahelp/">JavaHelp home page</a>.
     * </p>
     *
     * @param classLoader         the class loader used to load the help resources
     * @param helpsetResourcePath the resource path to the helpset file (*.hs)
     */
    public final void addHelp(final ClassLoader classLoader, final String helpsetResourcePath) {
        Guardian.assertNotNull("classLoader", classLoader);
        Guardian.assertNotNullOrEmpty("helpsetResourcePath", helpsetResourcePath);
        final URL url = HelpSet.findHelpSet(classLoader, helpsetResourcePath);
        if (url == null) {
            getLogger().log(Level.SEVERE,
                            "Helpset not found: " + helpsetResourcePath + "");
            return;
        }
        try {
            final HelpSet helpSet = new HelpSet(classLoader, url);
            HelpSys.add(helpSet);
        } catch (HelpSetException e) {
            getLogger().log(Level.SEVERE,
                            "Helpset could not be added: " + helpsetResourcePath + "",
                            e);
        }
    }

    private void initBeamUserDir() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("Java system property 'user.home' not set");
        }
        _beamUserDir = SystemUtils.getApplicationDataDir(true);
        _appUserDir = new File(_beamUserDir, getAppSymbolicName());
        _appUserDir.mkdir();
    }


    // Loads locale-specific resources: strings, images, et cetera
    private void initResources() throws MissingResourceException {
        if (applicationDescriptor.getResourceBundleName() != null) {
            _resourceBundle = ResourceBundle.getBundle(applicationDescriptor.getResourceBundleName(), Locale.getDefault(), getClass().getClassLoader());
        } else {
            _resourceBundle = null;
        }
    }

    private void initPreferences() {
        _preferences = new PropertyMap();

        _preferences.setPropertyBool(PROPERTY_KEY_APP_DEBUG_ENABLED, false);

        _preferences.setPropertyString(PROPERTY_KEY_APP_UI_FONT_NAME, "SansSerif");
        _preferences.setPropertyInt(PROPERTY_KEY_APP_UI_FONT_SIZE, 12);

        _preferences.setPropertyBool(PROPERTY_KEY_APP_LOG_ENABLED, false);
        _preferences.setPropertyString(PROPERTY_KEY_APP_LOG_PREFIX, getAppName());
        _preferences.setPropertyString(PROPERTY_KEY_APP_LOG_LEVEL, SystemUtils.LLS_INFO);
        _preferences.setPropertyBool(PROPERTY_KEY_APP_LOG_ECHO, false);

        _preferencesFile = new File(_appUserDir, "preferences.properties");
        loadPreferences();

        _suppressibleOptionPane = new SuppressibleOptionPane(getPreferences());
    }

    /**
     * Handles mouse-over-action events.
     */
    final class MouseOverActionHandler extends MouseAdapter {

        private String _oldMessage;

        @Override
        public final void mouseEntered(MouseEvent evt) {
            if (evt.getSource() instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) evt.getSource();
                Action action = button.getAction();
                if (action != null) {
                    String message = (String) action.getValue(Action.SHORT_DESCRIPTION);
                    if (message != null && message.length() > 0) {
                        _oldMessage = getStatusBarMessage();
                        setStatusBarMessage(message);
                    }
                }
            }
        }

        @Override
        public final void mouseExited(MouseEvent evt) {
            if (evt.getSource() instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) evt.getSource();
                Action action = button.getAction();
                if (action != null) {
                    setStatusBarMessage(_oldMessage != null ? _oldMessage : " ");
                }
            }
        }
    }

    private static ApplicationDescriptor createApplicationDescriptor(String appName,
                                                                     String appSymbolicName,
                                                                     String appVersion,
                                                                     String appCopyrightInfo,
                                                                     String appResource,
                                                                     String appLoggerName) {
        final DefaultApplicationDescriptor applicationDescriptor = new DefaultApplicationDescriptor();
        applicationDescriptor.setDisplayName(appName);
        applicationDescriptor.setSymbolicName(appSymbolicName);
        applicationDescriptor.setVersion(appVersion);
        applicationDescriptor.setCopyright(appCopyrightInfo);
        applicationDescriptor.setResourceBundleName(appResource);
        applicationDescriptor.setLoggerName(appLoggerName);
        return applicationDescriptor;
    }

    public static class MainFrame extends DefaultDockableBarDockableHolder {
        public MainFrame() throws HeadlessException {
        }
    }
}



