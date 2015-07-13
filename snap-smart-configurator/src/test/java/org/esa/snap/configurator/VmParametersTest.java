package org.esa.snap.configurator;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

/**
 * @author Nicolas Ducoin
 */
public class VmParametersTest extends TestCase {
    @Test
    public void testBlankSpaces() {
        String stringWithBlanks = "a blank S\"tr ing\" with \"  's p a c e s'";
        List<String> vmParamsList = VMParameters.toParamList(stringWithBlanks);

        assertEquals(5, vmParamsList.size());
    }
}
