package org.esa.snap.smart.configurator;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Nicolas Ducoin
 */
public class VmParametersTest {
    @Test
    public void testBlankSpaces() {
        String stringWithBlanks = "a blank S\"tr ing\" with \"  's p a c e s'";
        List<String> vmParamsList = VMParameters.toParamList(stringWithBlanks);

        assertEquals(5, vmParamsList.size());
    }

    /**
     * Test we don't change file content
     */
    @Test
    public void testSaveVMParameters() throws URISyntaxException {
        URI confFileURL = getClass().getResource("snap.conf").toURI();
        Path confFileToSave = Paths.get(confFileURL);
        VMParameters.setSnapConfigPath(confFileToSave);

        String vmParametersString = "-Xms24m --locale en_GB  -Dnetbeans.mainclass=org.esa.snap.main.Main -Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false";
        VMParameters vmParameters = new VMParameters(vmParametersString);

        try {
            vmParameters.save();

            // compare the two files
            URI refConfFileURL = getClass().getResource("snap.conf").toURI();
            Path refConfigFilePath = Paths.get(refConfFileURL);
            List<String> refConfigLines = Files.readAllLines(refConfigFilePath);
            List<String> savedConfigLines = Files.readAllLines(confFileToSave);

            assertEquals(refConfigLines.size(), savedConfigLines.size());

            for(int i=0 ; i<refConfigLines.size() ; i++) {
                assertEquals(refConfigLines.get(i), savedConfigLines.get(i));
            }

        } catch (IOException e) {
            Assert.fail("Could not save or read config file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
