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
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ToolCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import java.text.MessageFormat;

/**
 * Tool actions are used to interact with a {@link com.bc.ceres.swing.figure.FigureEditor FigureEditor},
 * such as the VISAT product scene view.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ToolAction extends ToolCommand {

    private static Interactor activeInteractor = NullInteractor.INSTANCE;

    private InteractorListener activationHandler;

    public ToolAction() {
        super(ToolAction.class.getName());
        activationHandler = new ToolActivationHandler();
    }

    // todo - this shoud be moved to a ToolManager or something similar
    public static Interactor getActiveInteractor() {
        return activeInteractor;
    }

    @Override
    public void updateState(final CommandEvent event) {
        super.updateState(event);
        setEnabled(VisatApp.getApp().getSelectedProductSceneView() != null);
    }


    private static class ToolActivationHandler extends AbstractInteractorListener {

        private boolean zoomTipShown;
        private boolean figureTipShown;

        @Override
        public void interactorActivated(Interactor interactor) {
            activeInteractor = interactor;
            final FigureEditor drawingEditor = VisatApp.getApp().getSelectedProductSceneView();
            if (drawingEditor != null) {
                drawingEditor.setInteractor(activeInteractor);
            }
            maybeShowInteractorUsageTip();
        }

        private void maybeShowInteractorUsageTip() {
            VisatApp visatApp = VisatApp.getApp();
            final ProductSceneView productSceneView = visatApp.getSelectedProductSceneView();
            if (!figureTipShown
                && activeInteractor instanceof FigureEditorInteractor
                && productSceneView != null
                && productSceneView.getCurrentShapeFigure() != null) {
                visatApp.showInfoDialog("Tip:\n"
                                        + "Hold down the SHIFT key to add the new shape to the existing shape.\n"
                                        + "Hold down the CONTROL key to subtract the new shape from the existing shape.\n"
                                        + "If no key is pressed, the new shape replaces the old one."
                        , "CreateFigureTool.tip");
                figureTipShown = true;
            }
            if (!zoomTipShown
                && activeInteractor instanceof ZoomInteractor
                && productSceneView != null) {
                visatApp.showInfoDialog("Tip:\n"
                                        + "You can select a region or just click to zoom in.\n"
                                        + "Hold down the CONTROL key in order to zoom out."
                        , "ZoomTool.tip");
                zoomTipShown = true;
            }
        }
    }


    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        String interactorClassName = getConfigString(config, "interactor");
        if (interactorClassName != null) {
            Class<?> aClass;
            try {
                aClass = config.getDeclaringExtension().getDeclaringModule().loadClass(interactorClassName);
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  interactorClassName);
                throw new CoreException(msg, e);
            }
            Class<Interactor> interactorClass;
            if (Interactor.class.isAssignableFrom(aClass)) {
                interactorClass = (Class<Interactor>) aClass;
            } else {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derieved from [{2}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  interactorClassName,
                                                  Interactor.class.getName());
                throw new CoreException(msg);
            }

            try {
                setTool(interactorClass.newInstance());
            } catch (Exception e) {
                String msg = MessageFormat.format("[{0}]: Not able to create new instance of class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  interactorClass.getName());
                throw new CoreException(msg, e);
            }


        }
        super.configure(config);
        getTool().addListener(activationHandler);

    }

}
