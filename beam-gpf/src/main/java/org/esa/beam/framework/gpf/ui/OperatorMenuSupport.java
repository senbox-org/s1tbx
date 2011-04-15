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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.jidesoft.action.CommandMenuBar;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Support for operator parameters input/output.
 */
public class OperatorMenuSupport {

    private Component parentComponent;
    private final Class<? extends Operator> opType;
    private final PropertySet parameters;
    private final String helpId;
    private final ParameterDescriptorFactory descriptorFactory;

    public OperatorMenuSupport(Component parentComponent,
                               Class<? extends Operator> opType,
                               PropertySet parameters,
                               String helpId) {
        this.parentComponent = parentComponent;
        this.opType = opType;
        this.parameters = parameters;
        this.helpId = helpId;
        this.descriptorFactory = new ParameterDescriptorFactory();
    }

    public PropertySet getParameters() {
        return parameters;
    }

    public Action createStoreParametersAction() {
        return new StoreParametersAction();
    }

    public Action createDisplayParametersAction() {
        return new DisplayParametersAction();
    }

    public Action createLoadParametersAction() {
        return new LoadParametersAction();
    }

    public Action createAboutOperatorAction() {
        return new AboutOperatorAction();
    }

    public JMenuItem createHelpMenuitem() {
        JMenuItem menuItem = new JMenuItem("Help");
        if (helpId != null && !helpId.isEmpty()) {
            HelpSys.enableHelpOnButton(menuItem, helpId);
        } else {
            menuItem.setEnabled(false);
        }
        return menuItem;
    }

    public JMenuBar createDefaultMenue() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(createLoadParametersAction());
        fileMenu.add(createStoreParametersAction());
        fileMenu.add(createDisplayParametersAction());

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createHelpMenuitem());
        helpMenu.add(createAboutOperatorAction());

        JMenuBar menuBar = new CommandMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    void fromDomElement(DomElement parametersElement) throws ValidationException, ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        domConverter.convertDomToValue(parametersElement, parameters);
    }

    DomElement toDomElement() throws ValidationException, ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        DefaultDomElement parametersElement = new DefaultDomElement("parameters");
        domConverter.convertValueToDom(parameters, parametersElement);
        return parametersElement;
    }

    private class LoadParametersAction extends AbstractAction {

        LoadParametersAction() {
            super("Load Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(createParameterFileFilter());
            String title = "Load Parameters";
            fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            int response = fileChooser.showDialog(parentComponent, "Load");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    readFromFile(selectedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parentComponent, "Could not load parameters.\n" + e.getMessage(),
                                                  title, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return parameters != null;
        }

        private void readFromFile(File selectedFile) throws ValidationException, ConversionException, IOException {
            FileReader reader = new FileReader(selectedFile);
            try {
                fromDomElement(readXml(reader));
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
                return new Xpp3DomElement(createDom(sb.toString()));
            } finally {
                br.close();
            }
        }

        private Xpp3Dom createDom(String xml) {
            XppDomWriter domWriter = new XppDomWriter();
            new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
            return domWriter.getConfiguration();
        }


    }

    private class StoreParametersAction extends AbstractAction {

        StoreParametersAction() {
            super("Store Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            final FileNameExtensionFilter parameterFileFilter = createParameterFileFilter();
            fileChooser.addChoosableFileFilter(parameterFileFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            String title = "Store Parameters";
            fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int response = fileChooser.showDialog(parentComponent, "Store");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    selectedFile = FileUtils.ensureExtension(selectedFile,
                                                             "." + parameterFileFilter.getExtensions()[0]);
                    String xmlString = toDomElement().toXml();
                    writeToFile(xmlString, selectedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parentComponent, "Could not store parameters.\n" + e.getMessage(),
                                                  title, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return parameters != null;
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
            super("Display Parameters");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                showMessageDialog("Parameters", new JTextArea(toDomElement().toXml()));
            } catch (Exception e) {
                Debug.trace(e);
                showMessageDialog("Parameters", new JLabel("Failed to convert parameters to XML."));
            }
        }

        @Override
        public boolean isEnabled() {
            return parameters != null;
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
