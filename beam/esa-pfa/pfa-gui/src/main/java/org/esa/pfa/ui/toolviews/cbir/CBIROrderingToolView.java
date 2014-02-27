/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir;

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;

import javax.swing.*;

/**
 * Product Ordering Toolview
 */
public class CBIROrderingToolView extends AbstractToolView implements Patch.PatchListener, CBIRSession.CBIRSessionListener {

    public final static String ID = "org.esa.pfa.ui.toolviews.cbir.CBIROrderingToolView";

    private CBIRSession session;

    public CBIROrderingToolView() {
        CBIRSession.Instance().addListener(this);
    }

    public JComponent createControl() {
        return new JLabel("Under construction...");
    }


    @Override
    public void notifyNewSession() {
        session = CBIRSession.Instance();
    }

    @Override
    public void notifyNewTrainingImages() {
    }

    @Override
    public void notifyModelTrained() {
    }

    @Override
    public void notifyStateChanged(final Patch patch) {
    }
}
