/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationPage;
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

    private String toolBarId;

    private State initState;

    private DockSide initSide;

    private int initIndex;

    private int dockedWidth;

    private int dockedHeight;

    // todo: add XStream converter?
    private Rectangle floatingBounds;

    // todo: add XStream converter?
    private Dimension preferredSize;

    private transient Icon smallIcon;
    private transient Icon largeIcon;
    private transient PropertyContainer propertyContainer;

    public DefaultToolViewDescriptor() {
        propertyContainer = PropertyContainer.createObjectBacked(this);
        initState = State.HIDDEN;
        preferredSize = new Dimension(320, 200);
        floatingBounds = new Rectangle(100, 100, 960, 600);
        initSide = DockSide.WEST;
        initIndex = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getHelpId() {
        return helpId;
    }

    @Override
    public void setHelpId(String helpId) {
        setValue(PROPERTY_KEY_HELP_ID, helpId);
    }

    @Override
    public String getTitle() {
        if (title == null) {
            title = tabTitle;
        }
        return title;
    }

    @Override
    public void setTitle(String title) {
        setValue(PROPERTY_KEY_TITLE, title);
    }

    /**
     * @return The window tab-title.
     */
    @Override
    public String getTabTitle() {
        if (tabTitle == null) {
            tabTitle = title;
        }
        return tabTitle;
    }

    /**
     * @param tabTitle The window tab-title.
     */
    @Override
    public void setTabTitle(String tabTitle) {
        setValue(PROPERTY_KEY_TAB_TITLE, tabTitle);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        setValue(PROPERTY_KEY_DESCRIPTION, description);
    }

    @Override
    public String getToolBarId() {
        return toolBarId;
    }

    @Override
    public void setToolBarId(String toolBarId) {
        this.toolBarId = toolBarId;
    }

    @Override
    public Icon getSmallIcon() {
        if (smallIconPath != null && smallIcon == null) {
            smallIcon = UIUtils.loadImageIcon(smallIconPath, toolViewClass);
        }
        return smallIcon;
    }

    @Override
    public Icon getLargeIcon() {
        if (largeIconPath != null && largeIcon == null) {
            largeIcon = UIUtils.loadImageIcon(largeIconPath, toolViewClass);
        }
        return largeIcon;
    }

    @Override
    public void setSmallIcon(Icon smallIcon) {
        setValue(PROPERTY_KEY_SMALL_ICON, smallIcon);
    }

    @Override
    public void setLargeIcon(Icon largeIcon) {
        setValue(PROPERTY_KEY_LARGE_ICON, largeIcon);
    }

    @Override
    public char getMnemonic() {
        return mnemonic;
    }

    @Override
    public void setMnemonic(char mnemonic) {
        setValue(PROPERTY_KEY_MNEMONIC, mnemonic);
    }

    @Override
    public KeyStroke getAccelerator() {
        return accelerator;
    }

    @Override
    public void setAccelerator(KeyStroke accelerator) {
        setValue(PROPERTY_KEY_ACCELERATOR, accelerator);
    }

    /**
     * @return The initial state.
     */
    @Override
    public State getInitState() {
        return initState;
    }

    /**
     * @param initState The initial state.
     */
    @Override
    public void setInitState(State initState) {
        setValue("initState", initState);
    }

    /**
     * @return The initial side.
     */
    @Override
    public DockSide getInitSide() {
        return initSide;
    }

    /**
     * @param initSide The initial side.
     */
    @Override
    public void setInitSide(DockSide initSide) {
        setValue("initSide", initSide);
    }

    /**
     * @return The initial index.
     */
    @Override
    public int getInitIndex() {
        return initIndex;
    }

    /**
     * @param initIndex The initial index.
     */
    @Override
    public void setInitIndex(int initIndex) {
        setValue("initIndex", initIndex);
    }

    /**
     * @return The docked width in pixels.
     */
    @Override
    public int getDockedWidth() {
        return dockedWidth;
    }

    /**
     * @param dockedWidth The docked width in pixels.
     */
    @Override
    public void setDockedWidth(int dockedWidth) {
        setValue("dockedWidth", dockedWidth);
    }

    /**
     * @return The docked height in pixels.
     */
    @Override
    public int getDockedHeight() {
        return dockedHeight;
    }

    /**
     * @param dockedHeight The docked height in pixels.
     */
    @Override
    public void setDockedHeight(int dockedHeight) {
        setValue("dockedHeight", dockedHeight);
    }

    @Override
    public Rectangle getFloatingBounds() {
        return floatingBounds;
    }

    @Override
    public void setFloatingBounds(Rectangle floatingBounds) {
        setValue("floatingBounds", floatingBounds);
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        setValue("preferredSize", preferredSize);
    }

    @Override
    public boolean isHidable() {
        // todo
        return false;
    }

    @Override
    public void setHidable(boolean state) {
        // todo
    }

    @Override
    public boolean isDockable() {
        // todo
        return false;
    }

    @Override
    public void setDockable(boolean state) {
        // todo
    }

    @Override
    public boolean isFloatable() {
        // todo
        return false;
    }

    @Override
    public void setFloatable(boolean state) {
        // todo
    }

    @Override
    public boolean isAutohidable() {
        // todo
        return false;
    }

    @Override
    public void setAutohidable(boolean state) {
        // todo
    }

    @Override
    public boolean isMaximizable() {
        // todo
        return false;
    }

    @Override
    public void setMaximizable(boolean state) {
        // todo
    }

    // todo - the following actually are not descriptor properties!
    // {{{
    @Override
    public boolean isVisible() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setVisible(boolean state) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isEnabled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setEnabled(boolean state) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    // }}}

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyContainer.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyContainer.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyContainer.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyContainer.removePropertyChangeListener(propertyName, listener);
    }

    @Override
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
     * @param applicationPage The window
     *
     * @return The show page component command.
     */
    @Override
    public Command createShowViewCommand(final ApplicationPage applicationPage) {
        final String commandId = getId() + ".showCmd";
        final ExecCommand command = applicationPage.getCommandManager().createExecCommand(commandId, new CommandAdapter() {
            @Override
            public void updateState(CommandEvent event) {
                PageComponent pageComponent = applicationPage.getToolView(getId());
                ExecCommand ecmd = (ExecCommand) event.getCommand();
                // Use the control of the page component's pane, because
                // using the  control of the page component directly would trigger its creation.
                // This is not intended.
                JComponent control = pageComponent.getContext().getPane().getControl();
                ecmd.setEnabled(control.isEnabled());
            }

            @Override
            public void actionPerformed(CommandEvent event) {
                applicationPage.showToolView(getId());
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

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
    }

    private void setValue(String key, Object value) {
        propertyContainer.setValue(key, value);
    }

}
