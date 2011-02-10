package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;

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

    private  class LoadParametersAction extends AbstractAction {
        public LoadParametersAction() {
            super("Load Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(createParameterFileFilter());
            fileChooser.setDialogTitle("Load Parameters");
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            int response = fileChooser.showDialog(null, // todo
                                                  "Load");
        }

    }

    private class StoreParametersAction extends AbstractAction {
        public StoreParametersAction() {
            super("Store Parameters...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(createParameterFileFilter());
            fileChooser.setDialogTitle("Store Parameters");
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int response = fileChooser.showDialog(null, // todo
                                                  "Store");
            try {
                System.out.println("Storing parameters...");
                String s = toDomElement().toXml();
                System.out.println(s);
            } catch (ValidationException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ConversionException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
    private FileNameExtensionFilter createParameterFileFilter() {
          return new FileNameExtensionFilter("BEAM GPF Parameter Files (XML)", "xml");
      }
  }
