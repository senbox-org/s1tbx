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

package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;

import java.awt.event.InputEvent;

public class AbstractInteractorListener implements InteractorListener {
    @Override
    public void interactorActivated(Interactor interactor) {
    }

    @Override
    public void interactorDeactivated(Interactor interactor) {
    }

    @Override
    public void interactionStarted(Interactor interactor, InputEvent inputEvent) {
    }

    @Override
    public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
    }

    @Override
    public void interactionCancelled(Interactor interactor, InputEvent inputEvent) {
    }
}
