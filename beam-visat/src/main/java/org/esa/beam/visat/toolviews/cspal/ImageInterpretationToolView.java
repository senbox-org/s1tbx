/*
 * $Id: ContrastStretchToolView.java,v 1.2 2007/04/20 08:49:14 marcop Exp $
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
package org.esa.beam.visat.toolviews.cspal;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The contrast stretch window.
 */
public class ImageInterpretationToolView extends AbstractToolView {

    public static final String ID = ContrastStretchToolView.class.getName();
    private ImageInterpretationForm imageInterpretationForm;

    public ImageInterpretationToolView() {
    }

    @Override
    protected JComponent createControl() {
        imageInterpretationForm = new ImageInterpretationForm(this);
        return imageInterpretationForm.getMainPanel();
    }
}