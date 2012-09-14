/* ========================================================================
 * JCommon : a free general purpose class library for the Java(tm) platform
 * ========================================================================
 *
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 * 
 * Project Info:  http://www.jfree.org/jcommon/index.html
 *
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by 
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, 
 * USA.  
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 * ----------------
 * WizardPanel.java
 * ----------------
 * (C) Copyright 2000-2004, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * $Id: WizardPanel.java,v 1.5 2007/11/02 17:50:36 taqua Exp $
 *
 * Changes (from 26-Oct-2001)
 * --------------------------
 * 26-Oct-2001 : Changed package to com.jrefinery.ui.*;
 * 14-Oct-2002 : Fixed errors reported by Checkstyle (DG);
 *
 */
package org.esa.nest.dat.wizards;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.BatchGraphDialog;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * A panel that provides the user interface for a single step in a WizardDialog.
 *
 * @author David Gilbert
 */
public abstract class WizardPanel extends JPanel {

    /** The owner. */
    private WizardDialog owner;
    private final String panelTitle;

    protected final static File wizardGraphPath = new File(GraphBuilderDialog.getInternalGraphFolder(), "wizards");
    protected boolean finishing = false;

    private File[] targetFileList = new File[0];

    /**
     * Creates a new panel.
     *
     * @param name  the panel name.
     */
    protected WizardPanel(final String name) {
        super(new BorderLayout(1,1));
        this.panelTitle = name;

        this.setPreferredSize(new Dimension(500,500));
    }

    public String getPanelTitle() {
        return panelTitle;
    }

    /**
     * Returns a reference to the dialog that owns the panel.
     *
     * @return the owner.
     */
    public WizardDialog getOwner() {
        return this.owner;
    }

    /**
     * Sets the reference to the dialog that owns the panel (this is called automatically by
     * the dialog when the panel is added to the dialog).
     *
     * @param owner  the owner.
     */
    public void setOwner(final WizardDialog owner) {
        this.owner = owner;
    }

    public void finish() {
    }

    protected static void showErrorMsg(String msg) {
        VisatApp.getApp().showErrorDialog("Oops!", msg);
    }

    protected static JPanel createTextPanel(final String title, final String text) {
        final JPanel textPanel = new JPanel(new BorderLayout(2, 2));
        textPanel.setBorder(BorderFactory.createTitledBorder(title));
        final JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPanel.add(textPane, BorderLayout.NORTH);
        return textPanel;
    }

    public abstract boolean validateInput();

    /**
     * This method is called when the dialog redisplays this panel as a result of the user clicking
     * the "Previous" button.  Inside this method, subclasses should make a note of their current
     * state, so that they can decide what to do when the user hits "Next".
     */
    public abstract void returnFromLaterStep();

    /**
     * Returns true if it is OK to redisplay the last version of the next panel, or false if a new
     * version is required.
     *
     * @return boolean.
     */
    public abstract boolean canRedisplayNextPanel();

    /**
     * Returns true if there is a next panel.
     *
     * @return boolean.
     */
    public abstract boolean hasNextPanel();

    /**
     * Returns true if it is possible to finish from this panel.
     *
     * @return boolean.
     */
    public abstract boolean canFinish();

    /**
     * Returns the next panel in the sequence, given the current user input.  Returns null if this
     * panel is the last one in the sequence.
     *
     * @return the next panel in the sequence.
     */
    public abstract WizardPanel getNextPanel();

    public File[] getTargetFileList() {
        return targetFileList;
    }

    public class GraphProcessListener implements GraphBuilderDialog.ProcessingListener {
        public void notifyMSG(final MSG msg, final String text) {
            if(msg.equals(MSG.DONE)) {
                getOwner().updateState();
                
                if(finishing)  {
                    getOwner().dispose();
                }
            }
        }
        public void notifyMSG(final MSG msg, final File[] fileList) {
            if(msg.equals(MSG.DONE)) {
                targetFileList = fileList;
                getOwner().updateState();

                if(finishing)  {
                    getOwner().dispose();
                }
            }
        }
    }

    public class MyBatchProcessListener implements BatchGraphDialog.BatchProcessListener {
        public void notifyMSG(final BatchMSG msg, final File[] inputFileList, final File[] outputFileList) {
            if(msg.equals(BatchMSG.DONE)) {
                targetFileList = outputFileList;
                getOwner().updateState();
            }
        }
        public void notifyMSG(final BatchMSG msg, final String text) {
            if(msg.equals(BatchMSG.UPDATE)) {
            }
        }
    }
}
