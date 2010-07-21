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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ToolCommand;
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
    private static final String INTERACTOR_ELEMENT_NAME = "interactor";
    private static final String INTERACTOR_LISTENER_ELEMENT_NAME = "interactorListener";

    public ToolAction() {
        super(ToolAction.class.getName());
        activationHandler = new ToolActivationHandler();
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProductSceneView() != null);
        setSelected(getInteractor().isActive());
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        VisatApp.getApp().setActiveInteractor(getInteractor());
    }

    private class ToolActivationHandler extends AbstractInteractorListener {

        @Override
        public void interactorActivated(Interactor interactor) {
            if (VisatApp.getApp().getActiveInteractor() != interactor) {
                VisatApp.getApp().setActiveInteractor(interactor);
            }
            setSelected(true);
        }

        @Override
        public void interactorDeactivated(Interactor interactor) {
            if (VisatApp.getApp().getActiveInteractor() == interactor) {
                VisatApp.getApp().setActiveInteractor(NullInteractor.INSTANCE);
            }
            setSelected(false);
        }

    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        configureInteractor(config);
        super.configure(config);
    }

    private void configureInteractor(ConfigurationElement config) throws CoreException {
        String interactorClassName = getConfigString(config, INTERACTOR_ELEMENT_NAME);
        if (interactorClassName != null) {
            Interactor interactor = loadObjectInstance(config, Interactor.class, interactorClassName);
            interactor.addListener(activationHandler);
            String interactorListenerClassName = getConfigString(config, INTERACTOR_LISTENER_ELEMENT_NAME);
            if (interactorListenerClassName != null) {
                InteractorListener interactorListener = loadObjectInstance(config, InteractorListener.class, interactorListenerClassName);
                interactor.addListener(interactorListener);
            }
            setInteractor(interactor);
        } else {
            String msg = MessageFormat.format("[{0}]: Missing element [{1}] whose value must be an instance of [{2}]",
                                              config.getDeclaringExtension().getDeclaringModule().getName(),
                                              INTERACTOR_ELEMENT_NAME,
                                              Interactor.class.getName());
            throw new CoreException(msg);
        }
    }

    private <T> T loadObjectInstance(ConfigurationElement config, Class<? extends T> baseClass, String implClassName) throws CoreException {
        Class<?> derivedClass;
        try {
            derivedClass = config.getDeclaringExtension().getDeclaringModule().loadClass(implClassName);
        } catch (ClassNotFoundException e) {
            String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                              config.getDeclaringExtension().getDeclaringModule().getName(),
                                              implClassName);
            throw new CoreException(msg, e);
        }
        Class<? extends T> interactorClass;
        if (baseClass.isAssignableFrom(derivedClass)) {
            interactorClass = (Class<? extends T>) derivedClass;
        } else {
            String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derived from [{2}]",
                                              config.getDeclaringExtension().getDeclaringModule().getName(),
                                              implClassName,
                                              baseClass.getName());
            throw new CoreException(msg);
        }
        final T instance;
        try {
            instance = interactorClass.newInstance();
        } catch (Exception e) {
            String msg = MessageFormat.format("[{0}]: Not able to create new instance of class [{1}]",
                                              config.getDeclaringExtension().getDeclaringModule().getName(),
                                              interactorClass.getName());
            throw new CoreException(msg, e);
        }
        return instance;
    }

}
