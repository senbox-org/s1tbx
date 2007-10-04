package org.esa.beam.collocation.visat;

import junit.framework.TestCase;
import com.bc.ceres.binding.ValidationException;

/**
 * Tests for class {@link TargetProductSelector}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class TargetProductSelectorTest extends TestCase {

    private TargetProductSelectorModel model;
    private TargetProductSelector selector;


    @Override
    protected void setUp() throws Exception {
        model = new TargetProductSelectorModel(true);
        selector = new TargetProductSelector(model);
    }

    public void testSetGetProductName() throws ValidationException {

        selector.getProductNameTextField().setText("Asterix");
        selector.getProductNameTextField().postActionEvent();
        assertEquals("Asterix", model.getProductName());

//        model.setProductName("Obelix");
//        assertEquals("Obelix", selector.getProductNameTextField().getText());
    }
}
