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
package org.esa.nest.dat.wizards;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
    Instructions Panel
 */
public abstract class AbstractInstructPanel extends WizardPanel {
    private final String title;
    protected BufferedImage image = null;
    protected int imgPosX = 0;
    protected int imgPosY = 0;

    public AbstractInstructPanel(final String title) {
        super("Instructions");
        this.title = title;

        createPanel();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return true;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public abstract WizardPanel getNextPanel();

    public boolean validateInput() {
        return true;
    }

    protected abstract String getDescription();

    protected abstract String getInstructions();

    private void createPanel() {

        final JPanel instructPanel1 = new JPanel(new BorderLayout(2, 2));
        instructPanel1.setBorder(BorderFactory.createTitledBorder(title));
        final JTextPane desciptionPane = new JTextPane();
        desciptionPane.setText(getDescription());
        instructPanel1.add(desciptionPane, BorderLayout.NORTH);
        this.add(instructPanel1, BorderLayout.NORTH);

        final JPanel instructPanel2 = new JPanel(new BorderLayout(2, 2));
        instructPanel2.setBorder(BorderFactory.createTitledBorder("Instructions"));
        final JTextPane instructionPane = new JTextPane();
        instructionPane.setText(getInstructions());
        instructPanel2.add(instructionPane, BorderLayout.CENTER);
        this.add(instructPanel2, BorderLayout.CENTER);
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);

        if(image != null) {
            g.drawImage(image, imgPosX, imgPosY, null);
        }
    }
}