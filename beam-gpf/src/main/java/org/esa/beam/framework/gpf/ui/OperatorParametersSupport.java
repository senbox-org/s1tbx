package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.util.io.FileUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Support for operator parameters input/output.
 */
public class OperatorParametersSupport {

    private final Class<? extends Operator> opType;
    private final PropertySet parameters;
    private final ParameterDescriptorFactory descriptorFactory;

    public OperatorParametersSupport(Class<? extends Operator> opType, PropertySet parameters) {
        this.opType = opType;
        this.descriptorFactory = new ParameterDescriptorFactory();
        this.parameters = parameters;
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
            fileChooser.setDialogTitle("Load Parameters");
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            int response = fileChooser.showDialog(null, // todo
                                                  "Load");
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
            fileChooser.setDialogTitle("Store Parameters");
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int response = fileChooser.showDialog(null, // todo
                                                  "Store");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    String xmlString = toDomElement().toXml();
                    File selectedFile = fileChooser.getSelectedFile();
                    selectedFile = FileUtils.ensureExtension(selectedFile, parameterFileFilter.getExtensions()[0]);
                    writeToFile(xmlString, selectedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, // todo
                                                  "Could not store parameters \n" + e.getMessage(),
                                                  "Store Parameters", JOptionPane.ERROR_MESSAGE);
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
