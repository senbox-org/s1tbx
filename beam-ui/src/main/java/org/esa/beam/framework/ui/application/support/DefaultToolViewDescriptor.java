package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationWindow;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandAdapter;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.xstream.converters.KeyStrokeConverter;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;

/**
 * Provides a standard implementation for {@link org.esa.beam.framework.ui.application.ToolViewDescriptor}.
 */
@XStreamAlias("toolView")
public class DefaultToolViewDescriptor implements ToolViewDescriptor, ConfigurableExtension {

    private String id;

    @XStreamAlias("class")
    private Class toolViewClass;

    private String title;

    private String tabTitle;

    private String description;

    @XStreamAlias("smallIcon")
    private String smallIconPath;

    @XStreamAlias("largeIcon")
    private String largeIconPath;

    private String helpId;

    @XStreamConverter(KeyStrokeConverter.class)
    private KeyStroke accelerator;

    private char mnemonic;

    private State initState = State.HIDDEN;

    private DockSide initSide = DockSide.WEST;

    private int initIndex = 0;

    private int dockedWidth;

    private int dockedHeight;

    // todo: add XStream converter?
    private Rectangle floatingBounds = new Rectangle(100, 100, 320, 200);

    // todo: add XStream converter?
    private Dimension preferredSize = new Dimension(320, 200);

    private transient Icon smallIcon;
    private transient Icon largeIcon;
    private transient PojoWrapper pojoWrapper;

    public DefaultToolViewDescriptor() {
        pojoWrapper = new PojoWrapper(this);
    }

    public String getId() {
        return id;
    }

    public String getHelpId() {
        return helpId;
    }

    public void setHelpId(String helpId) {
        pojoWrapper.setValue(PROPERTY_KEY_HELP_ID, helpId);
    }

    public String getTitle() {
        if (title == null) {
            title = tabTitle;
        }
        return title;
    }

    public void setTitle(String title) {
        pojoWrapper.setValue(PROPERTY_KEY_TITLE, title);
    }

    /**
     * @return The window tab-title.
     */
    public String getTabTitle() {
        if (tabTitle == null) {
            tabTitle = title;
        }
        return tabTitle;
    }

    /**
     * @param tabTitle The window tab-title.
     */
    public void setTabTitle(String tabTitle) {
        pojoWrapper.setValue(PROPERTY_KEY_TAB_TITLE, tabTitle);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        pojoWrapper.setValue(PROPERTY_KEY_DESCRIPTION, description);
    }

    public Icon getSmallIcon() {
        if (smallIconPath != null && smallIcon == null) {
            smallIcon = UIUtils.loadImageIcon(smallIconPath, toolViewClass);
        }
        return smallIcon;
    }

    public Icon getLargeIcon() {
        if (largeIconPath != null && largeIcon == null) {
            largeIcon = UIUtils.loadImageIcon(largeIconPath, toolViewClass);
        }
        return largeIcon;
    }

    public void setSmallIcon(Icon smallIcon) {
        pojoWrapper.setValue(PROPERTY_KEY_SMALL_ICON, smallIcon);
    }

    public void setLargeIcon(Icon largeIcon) {
        pojoWrapper.setValue(PROPERTY_KEY_LARGE_ICON, largeIcon);
    }

    public char getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(char mnemonic) {
        pojoWrapper.setValue(PROPERTY_KEY_MNEMONIC, mnemonic);
    }

    public KeyStroke getAccelerator() {
        return accelerator;
    }

    public void setAccelerator(KeyStroke accelerator) {
        pojoWrapper.setValue(PROPERTY_KEY_ACCELERATOR, accelerator);
    }

    /**
     * @return The initial state.
     */
    public State getInitState() {
        return initState;
    }

    /**
     * @param initState The initial state.
     */
    public void setInitState(State initState) {
        pojoWrapper.setValue("initState", initState);
    }

    /**
     * @return The initial side.
     */
    public DockSide getInitSide() {
        return initSide;
    }

    /**
     * @param initSide The initial side.
     */
    public void setInitSide(DockSide initSide) {
        pojoWrapper.setValue("initSide", initSide);
    }

    /**
     * @return The initial index.
     */
    public int getInitIndex() {
        return initIndex;
    }

    /**
     * @param initIndex The initial index.
     */
    public void setInitIndex(int initIndex) {
        pojoWrapper.setValue("initIndex", initIndex);
    }

    /**
     * @return The docked width in pixels.
     */
    public int getDockedWidth() {
        return dockedWidth;
    }

    /**
     * @param dockedWidth The docked width in pixels.
     */
    public void setDockedWidth(int dockedWidth) {
        pojoWrapper.setValue("dockedWidth", dockedWidth);
    }

    /**
     * @return The docked height in pixels.
     */
    public int getDockedHeight() {
        return dockedHeight;
    }

    /**
     * @param dockedHeight The docked height in pixels.
     */
    public void setDockedHeight(int dockedHeight) {
        pojoWrapper.setValue("dockedHeight", dockedHeight);
    }

    public Rectangle getFloatingBounds() {
        return floatingBounds;
    }

    public void setFloatingBounds(Rectangle floatingBounds) {
        pojoWrapper.setValue("floatingBounds", floatingBounds);
    }

    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public void setPreferredSize(Dimension preferredSize) {
        pojoWrapper.setValue("preferredSize", preferredSize);
    }

    public boolean isHidable() {
        // todo
        return false;
    }

    public void setHidable(boolean state) {
        // todo
    }

    public boolean isDockable() {
        // todo
        return false;
    }

    public void setDockable(boolean state) {
        // todo
    }

    public boolean isFloatable() {
        // todo
        return false;
    }

    public void setFloatable(boolean state) {
        // todo
    }

    public boolean isAutohidable() {
        // todo
        return false;
    }

    public void setAutohidable(boolean state) {
        // todo
    }

    public boolean isMaximizable() {
        // todo
        return false;
    }

    public void setMaximizable(boolean state) {
        // todo
    }

    // todo - the following actually are not descriptor properties!
    // {{{
    public boolean isVisible() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setVisible(boolean state) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEnabled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setEnabled(boolean state) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    // }}}

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pojoWrapper.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pojoWrapper.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pojoWrapper.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pojoWrapper.removePropertyChangeListener(propertyName, listener);
    }

    public PageComponent createPageComponent() {
        return createToolView();
    }

    protected ToolView createToolView() {
        Assert.state(toolViewClass != null, "toolViewClass != null");
        Object object;
        try {
            object = toolViewClass.newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException("viewClass.newInstance()", e);
        }
        Assert.state(object instanceof ToolView, "object instanceof ToolView");
        ToolView toolView = (ToolView) object;
        toolView.setDescriptor(this);
        return toolView;
    }

    /**
     * Create a command that when executed, will attempt to show the
     * page component described by this descriptor in the provided
     * application window.
     *
     * @param window The window
     *
     * @return The show page component command.
     */
    public Command createShowViewCommand(final ApplicationWindow window) {
        final String commandId = getId() + ".showCmd";
        final ExecCommand command = window.getCommandManager().createExecCommand(commandId, new CommandAdapter() {
            @Override
            public void updateState(CommandEvent event) {
                PageComponent pageComponent = window.getPage().getToolView(getId());
                ExecCommand ecmd = (ExecCommand) event.getCommand();
                // Use the control of the page component's pane, because
                // using the  control of the page component directly would trigger its creation.
                // This is not intended.
                JComponent control = pageComponent.getContext().getPane().getControl();
                ecmd.setEnabled(control.isEnabled());
            }

            @Override
            public void actionPerformed(CommandEvent event) {
                window.getPage().showToolView(getId());
            }
        });

        command.setParent("showToolViews");
        command.setSmallIcon(getSmallIcon());
        command.setLargeIcon(getLargeIcon());
        command.setShortDescription(getDescription());
        command.setLongDescription(getDescription());
        command.setText(getTabTitle());
        KeyStroke accelerator = getAccelerator();
        if (accelerator != null) {
            command.setAccelerator(accelerator);
        }
        char mnemonic = getMnemonic();
        if (mnemonic != -1) {
            command.setMnemonic(mnemonic);
        }

        return command;
    }

    public void configure(ConfigurationElement config) throws CoreException {
    }
}
