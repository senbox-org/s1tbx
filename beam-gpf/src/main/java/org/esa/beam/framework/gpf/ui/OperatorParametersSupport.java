package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.util.io.FileUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
public class OperatorParametersSupport {

    private Component parentComponent;
    private final Class<? extends Operator> opType;
    private final PropertySet parameters;
    private final ParameterDescriptorFactory descriptorFactory;

    public OperatorParametersSupport(Component parentComponent,
                                     Class<? extends Operator> opType, PropertySet parameters) {
        this.parentComponent = parentComponent;
        this.opType = opType;
        this.parameters = parameters;
        this.descriptorFactory = new ParameterDescriptorFactory();
    }

    public PropertySet getParameters() {
        return parameters;
    }

    public Action createStoreParametersAction() {
        return new StoreParametersAction();
    }

    public Action createLoadParametersAction() {
        return new LoadParametersAction();
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
}
