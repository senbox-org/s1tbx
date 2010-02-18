package org.esa.beam.processor.radtorefl;

import java.awt.GridBagConstraints;

import javax.swing.JPanel;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamEditor;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.ListEditor;
import org.esa.beam.framework.processor.ui.IOParameterPage;
import org.esa.beam.framework.processor.ui.MultiPageProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessingParameterPage;
import org.esa.beam.framework.processor.ui.ProcessorUI;

public class RadToReflUIFactory {
    public static ProcessorUI createUI() {
    	final IOParameterPage ioParameterPage = new IOParameterPage(
    			new IOParameterPage.InputProductValidator() {
    				
    				@Override
                    public boolean validate(Product product) {
    					final boolean valid = RadToReflProcessor.isValidInputProduct(product);
    					
    					if (!valid) {
    						setErrorMessage("Please select a MERIS Level 1b product.");
    					}
    					
    					return valid;
    				}
    			});

    	ioParameterPage.setDefaultOutputProductFileName(RadToReflConstants.DEFAULT_OUTPUT_PRODUCT_FILE_NAME);
    	ioParameterPage.setDefaultLogPrefix(RadToReflConstants.DEFAULT_LOG_PREFIX);
        ioParameterPage.setDefaultLogToOutputParameter(RadToReflConstants.DEFAULT_LOG_TO_OUTPUT);

        final RadToReflRequestElementFactory elementFactory = RadToReflRequestElementFactory.getInstance();
        final ParamGroup paramGroup = new ParamGroup();

        paramGroup.addParameter(elementFactory.createParameter(RadToReflConstants.INPUT_BANDS_PARAM_NAME));
        paramGroup.addParameter(elementFactory.createParameter(RadToReflConstants.COPY_INPUT_BANDS_PARAM_NAME));
        
        final MultiPageProcessorUI ui = new MultiPageProcessorUI(RadToReflConstants.REQUEST_TYPE);
        ui.addPage(ioParameterPage);
        ui.addPage(new ProcessingParameterPage(paramGroup) {
        	
        	@Override
            public void addParameterToPanel(final Parameter parameter, final JPanel panel, final GridBagConstraints gbc) {
        		final ParamEditor editor = parameter.getEditor();

        		if (editor instanceof ListEditor) {
        			gbc.gridwidth = 1;
        			gbc.gridx = 0;
        			gbc.weightx = 1.0;	
        			gbc.insets.right = 2;
        			panel.add(editor.getLabelComponent(), gbc);  			
        			gbc.gridy++;
        			panel.add(editor.getEditorComponent(), gbc);
        			gbc.gridy++;                	
        		} else {
        			super.addParameterToPanel(parameter, panel, gbc);
        		} 			           
        	}
        });
        
        return ui;
    }
}
