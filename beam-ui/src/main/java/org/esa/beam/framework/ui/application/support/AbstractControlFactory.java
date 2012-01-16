/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.ui.application.ControlFactory;

import javax.swing.*;

/**
 * A control factory that only creates it's control when requested.
 *
 * @author Marco Peters (original by Keith Donald of Spring RCP project)
 */
public abstract class AbstractControlFactory implements ControlFactory {

    private boolean singleton;

    private JComponent control;

    protected AbstractControlFactory() {
        singleton = true;
    }

    protected final synchronized boolean isSingleton() {
        return singleton;
    }

    protected final synchronized void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public final synchronized JComponent getControl() {
        if (isSingleton()) {
            if (control == null) {
                this.control = createControl();
            }
            return control;
        }
        return createControl();
    }

    public final synchronized boolean isControlCreated() {
        return isSingleton() && control != null;
    }

    protected synchronized void createControlIfNecessary() {
        if (isSingleton() && control == null) {
            getControl();
        }
    }

    protected abstract JComponent createControl();
}