package org.esa.beam.visat;

import com.bc.swing.desktop.TabbedDesktopPane;
import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.esa.beam.framework.ui.application.DocView;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentPane;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.application.support.AbstractApplicationPage;
import org.esa.beam.framework.ui.application.support.DefaultToolViewPane;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import java.beans.PropertyVetoException;

public class VisatApplicationPage extends AbstractApplicationPage {

    private final DockingManager dockingManager;
    private final TabbedDesktopPane documentPane;

    public VisatApplicationPage(TabbedDesktopPane documentPane,
                                DockingManager dockingManager) {
        this.dockingManager = dockingManager;
        this.documentPane = documentPane;
    }

    @Override
    public ToolViewDescriptor getToolViewDescriptor(String id) {
        return VisatActivator.getInstance().getToolViewDescriptor(id);
    }

    @Override
    protected void doAddToolView(final ToolView toolView) {
        DockableFrame dockableFrame = (DockableFrame) toolView.getContext().getPane().getControl();
        dockableFrame.addDockableFrameListener(new DockableFrameAdapter() {
            @Override
            public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent) {
                fireHidden(toolView.getContext().getPane().getPageComponent());
            }

            @Override
            public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
                setActiveComponent();
            }

            @Override
            public void dockableFrameDeactivated(DockableFrameEvent dockableFrameEvent) {
                setActiveComponent();
            }
        });
        dockingManager.addFrame(dockableFrame);
    }

    @Override
    protected void doRemoveToolView(ToolView toolView) {
        dockingManager.removeFrame(toolView.getId());
    }

    @Override
    protected void doShowToolView(ToolView toolView) {
        dockingManager.showFrame(toolView.getId());
        if (shouldFloat(toolView)) {
            dockingManager.floatFrame(toolView.getId(), null, false);
        }
    }

    @Override
    protected void doHideToolView(ToolView toolView) {
        dockingManager.hideFrame(toolView.getId());
    }

    @Override
    protected boolean giveFocusTo(PageComponent pageComponent) {
        if (pageComponent instanceof ToolView) {
            dockingManager.activateFrame(pageComponent.getId());
        } else if (pageComponent instanceof DocView) {
            JInternalFrame frame = (JInternalFrame) pageComponent.getContext().getPane().getControl();
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
                // ignore
            }
        } else {
            throw new IllegalArgumentException(pageComponent.getClass() + " not handled");
        }
        return getActiveComponent() == pageComponent;
    }

    @Override
    protected PageComponentPane createToolViewPane(ToolView toolView) {
        return new DefaultToolViewPane(toolView);
    }

    @Override
    protected JComponent createControl() {
        return dockingManager.getDockedFrameContainer();
    }

    @Override
    protected void setActiveComponent() {
        ToolView toolView = null;
        DockableFrame activeFrame = dockingManager.getActiveFrame();
        if (activeFrame != null) {
            toolView = getToolView(activeFrame);
        }
        setActiveComponent(toolView);
    }

    private ToolView getToolView(DockableFrame activeFrame) {
        ToolView[] toolViews = getToolViews();
        for (ToolView toolView : toolViews) {
            if (activeFrame == toolView.getContext().getPane().getControl()) {
                return toolView;
            }
        }
        return null;
    }

    private boolean shouldFloat(ToolView toolView) {
        DockableFrame frame = dockingManager.getFrame(toolView.getId());
        return frame != null
               && frame.getContext().getHiddenPreviousState() == null
               && frame.getContext().getDockPreviousState() == null
               && frame.getContext().getFloatPreviousState() == null
               && frame.getInitMode() == DockContext.STATE_HIDDEN;
    }


}
