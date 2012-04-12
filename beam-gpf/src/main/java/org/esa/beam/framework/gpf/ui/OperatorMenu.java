/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.action.DockableBar;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 * <p/>
 * Provides an operator menu with action for loading, saving and displaying the parameters of an operator
 * in the file menu section and actions for help and about in the help menu section.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class OperatorMenu {

    private final Component parentComponent;
    private final OperatorParameterSupport parameterSupport;
    private final Class<? extends Operator> opType;
    private final String helpId;
    private final Action openParametersAction;
    private final Action saveParametersAction;
    private final Action displayParametersAction;
    private final Action aboutAction;

    public OperatorMenu(Component parentComponent,
                        Class<? extends Operator> opType,
                        OperatorParameterSupport parameterSupport,
                        String helpId) {
        this.parentComponent = parentComponent;
        this.parameterSupport = parameterSupport;
        this.opType = opType;
        this.helpId = helpId;
        openParametersAction = new OpenParametersAction();
        saveParametersAction = new SaveParametersAction();
        displayParametersAction = new DisplayParametersAction();
        aboutAction = new AboutOperatorAction();
    }

    public Action getSaveParametersAction() {
        return saveParametersAction;
    }

    public Action getDisplayParametersAction() {
        return displayParametersAction;
    }

    public Action getOpenParametersAction() {
        return openParametersAction;
    }

    public Action getAboutOperatorAction() {
        return aboutAction;
    }

    /**
     * Creates the default menu.
     *
     * @return The menu
     */
    public JMenuBar createDefaultMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openParametersAction);
        fileMenu.add(saveParametersAction);
        fileMenu.addSeparator();
        fileMenu.add(displayParametersAction);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createHelpMenuItem());
        helpMenu.add(aboutAction);

        final JMenuBar menuBar;
        if (SystemUtils.isRunningOnMacOS()) {
            menuBar = new JMenuBar();
        } else {
            menuBar = new CommandMenuBar();
        }
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenuItem createHelpMenuItem() {
        JMenuItem menuItem = new JMenuItem("Help");
        if (helpId != null && !helpId.isEmpty()) {
            HelpSys.enableHelpOnButton(menuItem, helpId);
        } else {
            menuItem.setEnabled(false);
        }
        return menuItem;
    }

    private class OpenParametersAction extends AbstractAction {

        OpenParametersAction() {
            super("Open Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(createParameterFileFilter());
            String title = "Open Parameters";
            fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            int response = fileChooser.showDialog(parentComponent, "Open");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    readFromFile(selectedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parentComponent, "Could not open parameters.\n" + e.getMessage(),
                                                  title, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }

        private void readFromFile(File selectedFile) throws Exception {
            FileReader reader = new FileReader(selectedFile);
            try {
                DomElement domElement = readXml(reader);
                parameterSupport.fromDomElement(domElement);
            } finally {
                reader.close();
            }
        }

        private DomElement readXml(Reader reader) throws IOException {
            final BufferedReader br = new BufferedReader(reader);
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                return new XppDomElement(createDom(sb.toString()));
            } finally {
                br.close();
            }
        }

        private XppDom createDom(String xml) {
            XppDomWriter domWriter = new XppDomWriter();
            new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
            return domWriter.getConfiguration();
        }


    }

    private class SaveParametersAction extends AbstractAction {

        SaveParametersAction() {
            super("Save Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            final FileNameExtensionFilter parameterFileFilter = createParameterFileFilter();
            fileChooser.addChoosableFileFilter(parameterFileFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            String title = "Save Parameters";
            fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int response = fileChooser.showDialog(parentComponent, "Save");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    selectedFile = FileUtils.ensureExtension(selectedFile,
                                                             "." + parameterFileFilter.getExtensions()[0]);
                    String xmlString = parameterSupport.toDomElement().toXml();
                    writeToFile(xmlString, selectedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parentComponent, "Could not save parameters.\n" + e.getMessage(),
                                                  title, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }

        private void writeToFile(String s, File outputFile) throws IOException {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            try {
                bw.write(s);
            } finally {
                bw.close();
            }
        }
    }

    private FileNameExtensionFilter createParameterFileFilter() {
        return new FileNameExtensionFilter("BEAM GPF Parameter Files (XML)", "xml");
    }

    private class DisplayParametersAction extends AbstractAction {

        DisplayParametersAction() {
            super("Display Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                DomElement domElement = parameterSupport.toDomElement();
                showMessageDialog("Parameters", new JTextArea(domElement.toXml()));
            } catch (Exception e) {
                Debug.trace(e);
                showMessageDialog("Parameters", new JLabel("Failed to convert parameters to XML."));
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }

    }


    private class AboutOperatorAction extends AbstractAction {

        AboutOperatorAction() {
            super("About...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            showMessageDialog("About " + getOperatorName(), new JLabel(getOperatorDescription()));
        }

    }

    private void showMessageDialog(String title, Component component) {
        final ModalDialog modalDialog = new ModalDialog(UIUtils.getRootWindow(parentComponent),
                                                        title,
                                                        AbstractDialog.ID_OK,
                                                        null); /*I18N*/
        modalDialog.setContent(component);
        modalDialog.show();
    }

    String getOperatorName() {
        OperatorMetadata operatorMetadata = opType.getAnnotation(OperatorMetadata.class);
        if (operatorMetadata != null) {
            return operatorMetadata.alias();
        }
        return opType.getName();
    }

    String getOperatorDescription() {
        OperatorMetadata operatorMetadata = opType.getAnnotation(OperatorMetadata.class);
        if (operatorMetadata != null) {
            StringBuilder sb = new StringBuilder("<html>");
            sb.append("<h2>").append(operatorMetadata.alias()).append(" Operator</h2>");
            sb.append("<table>");
            sb.append("  <tr><td><b>Name:</b></td><td><code>").append(operatorMetadata.alias()).append(
                    "</code></td></tr>");
            sb.append("  <tr><td><b>Full name:</b></td><td><code>").append(opType.getName()).append(
                    "</code></td></tr>");
            sb.append("  <tr><td><b>Purpose:</b></td><td>").append(operatorMetadata.description()).append("</td></tr>");
            sb.append("  <tr><td><b>Authors:</b></td><td>").append(operatorMetadata.authors()).append("</td></tr>");
            sb.append("  <tr><td><b>Version:</b></td><td>").append(operatorMetadata.version()).append("</td></tr>");
            sb.append("  <tr><td><b>Copyright:</b></td><td>").append(operatorMetadata.copyright()).append("</td></tr>");
            sb.append("</table>");
            sb.append("</html>");
            return makeHtmlConform(sb.toString());
        }
        return "No operator metadata available.";
    }

    private static String makeHtmlConform(String text) {
        return text.replace("\n", "<br/>");
    }

}
