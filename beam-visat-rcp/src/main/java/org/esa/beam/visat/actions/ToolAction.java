/*
 * $Id: ToolAction.java,v 1.1 2006/11/22 13:05:35 marcop Exp $
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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ToolCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolAdapter;
import org.esa.beam.framework.ui.tool.ToolEvent;
import org.esa.beam.framework.ui.tool.impl.AbstractCreateFigureTool;
import org.esa.beam.framework.ui.tool.impl.ZoomTool;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.text.MessageFormat;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ToolAction extends ToolCommand {

    private static Tool activeTool;

    private ToolActivationHandler toolActivationHandler;

    public ToolAction() {
        super(ToolAction.class.getName());
        toolActivationHandler = new ToolActivationHandler();
    }

    // todo - this shoud be moved to a ToolManager or something similar
    public static Tool getActiveTool() {
        return activeTool;
    }

    @Override
    public void updateState(final CommandEvent event) {
        super.updateState(event);
        setEnabled(VisatApp.getApp().getSelectedDrawingEditor() != null);
    }


    private class ToolActivationHandler extends ToolAdapter {

        private boolean zoomTipGiven;
        private boolean figureTipGiven;

        /**
         * Invoked if a tool was activated.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolActivated(final ToolEvent toolEvent) {
            activeTool = toolEvent.getTool();
            Debug.trace("VisatApp.ToolActivationHandler.toolActivated: " + activeTool);
            VisatApp.getApp().getCommandManager().toggleToolActivatedState(activeTool);
            final DrawingEditor drawingEditor = VisatApp.getApp().getSelectedDrawingEditor();
            if (drawingEditor != null) {
                drawingEditor.setTool(activeTool);
            }
            maybeShowToolUsageTip();
        }

        /**
         * Invoked if a tool was activated.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolDeactivated(final ToolEvent toolEvent) {
            Debug.trace("VisatApp.ToolActivationHandler.toolDeactivated: " + getTool());
        }

        /**
         * Invoked if the tool was canceled while it was active and not finished (the method does nothing).
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolCanceled(final ToolEvent toolEvent) {
            Debug.trace("VisatApp.ToolActivationHandler.toolCanceled: " + getTool());
        }

        /**
         * Invoked if the user finished the work with this tool (the method does nothing).
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolFinished(final ToolEvent toolEvent) {
            Debug.trace("VisatApp.ToolActivationHandler.toolFinished: " + getTool());
            VisatApp.getApp().getCommandManager().getToolCommand("selectTool").getTool().activate();
            VisatApp.getApp().updateState();
        }

        private void maybeShowToolUsageTip() {
            VisatApp visatApp = VisatApp.getApp();
            final ProductSceneView productSceneView = visatApp.getSelectedProductSceneView();
            if (!figureTipGiven
                && activeTool instanceof AbstractCreateFigureTool
                && productSceneView != null
                && productSceneView.getCurrentShapeFigure() != null) {
                visatApp.showInfoDialog("Tip:\n"
                                        + "Hold down the SHIFT key to add the new shape to the existing shape.\n"
                                        + "Hold down the CONTROL key to subtract the new shape from the existing shape.\n"
                                        + "If no key is pressed, the new shape replaces the old one."
                        , "CreateFigureTool.tip");
                figureTipGiven = true;
            }
            if (!zoomTipGiven
                && activeTool instanceof ZoomTool
                && productSceneView != null) {
                visatApp.showInfoDialog("Tip:\n"
                                        + "You can select a region or just click to zoom in.\n"
                                        + "Hold down the CONTROL key in order to zoom out."
                        , "ZoomTool.tip");
                zoomTipGiven = true;
            }
        }
    }


    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        String toolClassName = getConfigString(config, "tool");
        if (toolClassName != null) {
            Class<?> aClass;
            try {
                aClass = config.getDeclaringExtension().getDeclaringModule().loadClass(toolClassName);
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  toolClassName);
                throw new CoreException(msg, e);
            }
            Class<Tool> toolClass;
            if (Tool.class.isAssignableFrom(aClass)) {
                toolClass = (Class<Tool>) aClass;
            } else {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derieved from [{2}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  toolClassName,
                                                  Tool.class.getName());
                throw new CoreException(msg);
            }

            try {
                setTool(toolClass.newInstance());
            } catch (Exception e) {
                String msg = MessageFormat.format("[{0}]: Not able to create new instance of class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  toolClass.getName());
                throw new CoreException(msg, e);
            }


        }
        super.configure(config);
        getTool().addToolListener(toolActivationHandler);

    }

}
