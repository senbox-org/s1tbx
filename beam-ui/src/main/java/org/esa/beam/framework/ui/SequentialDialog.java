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
package org.esa.beam.framework.ui;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class SequentialDialog {

    public static final int ID_BACK = 0x0001;
    public static final int ID_NEXT = 0x0002;
    public static final int ID_FINISH = 0x0004;
    public static final int ID_CANCEL = 0x0008;
    public static final int ID_HELP = 0x0010;

    private JFrame frame;
    private JDialog dialog;
    private int buttonID = ID_CANCEL;
    private Vector names = new Vector();
    private int cardIndex = -1;
    private String titleBase;

    private JButton backButton = new JButton();
    private JButton nextButton = new JButton();
    private JButton finishButton = new JButton();
    private JButton cancelButton = new JButton();
    private JButton helpButton = new JButton();

    private CardLayout cardLayout = new CardLayout(6, 6);
    private JPanel cardPanel = new JPanel(cardLayout);

    public SequentialDialog(JFrame frame, String titleBase) {
        this(frame, titleBase, null, false);
    }

    public SequentialDialog(JFrame frame, String titleBase, Icon image, boolean hasHelp) {

        this.frame = frame;
        this.cardIndex = -1;
        this.dialog = new JDialog(frame, titleBase, true);
        this.titleBase = titleBase;

        // Panel for buttons
        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel panel1 = new JPanel(new BorderLayout());
        panel1.add(BorderLayout.NORTH, new HorizontalLine(4, 4));
        panel1.add(BorderLayout.SOUTH, buttonRow);

        if (image != null) {
            dialog.getContentPane().add(BorderLayout.WEST, new JLabel(image));
        }
        dialog.getContentPane().add(BorderLayout.CENTER, cardPanel);
        dialog.getContentPane().add(BorderLayout.SOUTH, panel1);

        backButton.setText("< Back");
        backButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buttonID = ID_BACK;
                onBack();
            }
        });

        nextButton.setText("Next >");
        nextButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buttonID = ID_NEXT;
                onNext();
            }
        });

        finishButton.setText("Finish");
        finishButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buttonID = ID_FINISH;
                onFinish();
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buttonID = ID_CANCEL;
                onCancel();
            }
        });

        helpButton.setText("Help");
        helpButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buttonID = ID_HELP;
                onHelp();
            }
        });

        Dimension hgap = new Dimension(6, 0);
        buttonRow.add(Box.createHorizontalGlue());
        buttonRow.add(backButton);
        buttonRow.add(Box.createRigidArea(hgap));
        buttonRow.add(nextButton);
        buttonRow.add(Box.createRigidArea(hgap));
        buttonRow.add(finishButton);
        buttonRow.add(Box.createRigidArea(hgap));
        buttonRow.add(cancelButton);
        if (hasHelp) {
            buttonRow.add(Box.createRigidArea(hgap));
            buttonRow.add(helpButton);
        }

        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                buttonID = ID_CANCEL;
                onCancel();
            }
        });
    }

    public JFrame getFrame() {
        return frame;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public int getButtonID() {
        return buttonID;
    }

    public int getCardCount() {
        return cardPanel.getComponentCount();
    }

    public Component getCard(int index) {
        return cardPanel.getComponent(index);
    }

    public Component getCurrentCard() {
        return cardIndex < 0 ? null : getCard(cardIndex);
    }

    public String getCardName(int index) {
        return (String) names.elementAt(index);
    }

    public void addCard(String name, Component card) {
        cardPanel.add(card, name);
        names.addElement(name);
    }

    public int show() {
        if (getCardCount() > 0) {
            showCard(0);
        }
        dialog.pack();
        center();
        dialog.setVisible(true);
        return getButtonID();
    }

    public void hide() {
        dialog.setVisible(false);
    }

    public void center() {
        UIUtils.centerComponent(dialog, frame);
    }

    protected void onBack() {
        showCard(getPreviousCardIndex());
    }

    protected void onNext() {
        if (verifyUserInput()) {
            showCard(getNextCardIndex());
        }
    }

    protected void onFinish() {
        if (verifyUserInput()) {
            hide();
        }
    }

    protected void onCancel() {
        hide();
    }

    protected void onHelp() {
    }

    protected int getCurrentCardIndex() {
        return cardIndex;
    }

    protected int getPreviousCardIndex() {
        int cardIndexMin = 0;
        return (cardIndex > cardIndexMin) ? (cardIndex - 1) : cardIndexMin;
    }

    protected int getNextCardIndex() {
        int cardIndexMax = getCardCount() - 1;
        return (cardIndex < cardIndexMax) ? (cardIndex + 1) : cardIndexMax;
    }

    protected void showCard(int index) {
        cardIndex = index;
        cardLayout.show(cardPanel, getCardName(index));
        dialog.setTitle(titleBase
                        + " - Step "
                        + (cardIndex + 1)
                        + " of "
                        + getCardCount());
        updateButtonStates();
    }

    protected void updateButtonStates() {
        backButton.setEnabled(isBackPossible());
        nextButton.setEnabled(isNextPossible());
        finishButton.setEnabled(isFinishPossible());
    }

    protected boolean isBackPossible() {
        return getCurrentCardIndex() > 0;
    }

    protected boolean isNextPossible() {
        return getCurrentCardIndex() < getCardCount() - 1;
    }

    protected boolean isFinishPossible() {
        return getCurrentCardIndex() == getCardCount() - 1;
    }

    protected boolean verifyUserInput() {
        return true;
    }

    static class HorizontalLine extends Canvas {

        Dimension prefSize;

        HorizontalLine(int w, int h) {
            prefSize = new Dimension(w, h);
        }

        @Override
        public Dimension getMinimumSize() {
            return prefSize;
        }

        @Override
        public Dimension getPreferredSize() {
            return prefSize;
        }

        @Override
        public void paint(Graphics g) {
            Dimension size = this.getSize();
            int x1 = 0;
            int x2 = size.width - 1;
            int y1 = size.height / 2;
            int y2 = y1 + 1;
            g.setColor(this.getBackground().brighter());
            g.drawLine(x1, y1, x2, y1);
            g.setColor(this.getBackground().darker());
            g.drawLine(x1, y2, x2, y2);
        }
    }
}
