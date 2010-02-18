package org.esa.beam.framework.ui.application;

import org.esa.beam.framework.ui.command.Command;

import javax.swing.KeyStroke;
import java.awt.Rectangle;


/**
 * Metadata about a view; a view descriptor is effectively a singleton view
 * definition. A descriptor also acts as a factory which produces new instances
 * of a given view when requested, typically by a requesting application page. A
 * view descriptor can also produce a command which launches a view for display
 * on the page within the current active window.
 *
 * @author Marco Peters (original by Keith Donald of Spring RCP project)
 */
public interface ToolViewDescriptor extends PageComponentDescriptor {

    /**
     * Represents all possible tool window states.
     */
    enum State {

        UNKNOWN,
        DOCKED,
        FLOATING,
        ICONIFIED,
        ICONIFIED_SHOWING,
        HIDDEN,
        MAXIMIZED
    }

    /**
     * Represents all possible tool window docking sides.
     */
    enum DockSide {

        UNKNOWN,
        ALL,
        CENTER,
        EAST,
        WEST,
        NORTH,
        SOUTH,
    }

    String PROPERTY_KEY_MNEMONIC = "mnemonic";

    String PROPERTY_KEY_ACCELERATOR = "accelerator";

    String PROPERTY_KEY_INIT_STATE = "initState";

    String PROPERTY_KEY_INIT_SIDE = "initSide";

    String PROPERTY_KEY_INIT_INDEX = "initIndex";

    String PROPERTY_KEY_TOOL_BAR_ID = "toolBarId";

    /**
     * @return The mnemonic.
     */
    char getMnemonic();

    /**
     * @param mnemonic The mnemonic.
     */
    void setMnemonic(char mnemonic);

    /**
     * @return The accelerator.
     */
    KeyStroke getAccelerator();

    /**
     * @param accelerator The accelerator.
     */
    void setAccelerator(KeyStroke accelerator);

    /**
     * @return The initial state.
     */
    State getInitState();

    /**
     * @param initState The initial state.
     */
    void setInitState(State initState);

    /**
     * @return The initial side.
     */
    DockSide getInitSide();

    /**
     * @param initSide The initial side.
     */
    void setInitSide(DockSide initSide);

    /**
     * @return The initial index.
     */
    int getInitIndex();

    /**
     * @param initIndex The initial index.
     */
    void setInitIndex(int initIndex);

    /**
     * @return The docked width in pixels.
     */
    int getDockedWidth();

    /**
     * @param dockedWidth The docked width in pixels.
     */
    void setDockedWidth(int dockedWidth);

    /**
     * @return The docked height in pixels.
     */
    int getDockedHeight();

    /**
     * @param dockedHeight The docked height in pixels.
     */
    void setDockedHeight(int dockedHeight);

    /**
     * @return The bounds of the tool window when in floating mode.
     */
    Rectangle getFloatingBounds();

    /**
     * @param floatingBounds The bounds of the tool window when in floating mode.
     */
    void setFloatingBounds(Rectangle floatingBounds);

    /**
     * @return <code>true</code> if the window is hidable, <code>false</code> otherwise.
     */
    boolean isHidable();

    /**
     * @param state <code>true</code> if the window is hidable, <code>false</code> otherwise.
     */
    void setHidable(boolean state);

    /**
     * @return <code>true</code> if the window is dockable, <code>false</code> otherwise.
     */
    boolean isDockable();

    /**
     * @param state <code>true</code> if the window is dockable, <code>false</code> otherwise.
     */
    void setDockable(boolean state);

    /**
     * @return <code>true</code> if the window is floatable, <code>false</code> otherwise.
     */
    boolean isFloatable();

    /**
     * @param state <code>true</code> if the window is floatable, <code>false</code> otherwise.
     */
    void setFloatable(boolean state);

    /**
     * @return <code>true</code> if the window is auto-hidable, <code>false</code> otherwise.
     */
    boolean isAutohidable();

    /**
     * @param state <code>true</code> if the window is auto-hidable, <code>false</code> otherwise.
     */
    void setAutohidable(boolean state);

    /**
     * Gets the ID of the tool bar in which the associated "show view" command will be placed.
     * If the ID is {@code null}, the associated "show view" command will be placed in the
     * default tool bar (ID="viewsToolBar").
     *
     * @return The ID of the tool bar.
     *
     * @see #createShowViewCommand(ApplicationPage)
     */
    String getToolBarId();

    /**
     * @param id The ID of the tool bar.
     *
     * @see #getToolBarId()
     */
    void setToolBarId(String id);

    /**
     * Create a command that when executed, will attempt to show the
     * page component described by this descriptor in the provided
     * application window.
     *
     * @param applicationPage The application page.
     *
     * @return The show page component command.
     *
     * @see #getToolBarId()
     */
    Command createShowViewCommand(ApplicationPage applicationPage);
}
