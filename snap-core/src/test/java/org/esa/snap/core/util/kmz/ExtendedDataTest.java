package org.esa.snap.core.util.kmz;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ExtendedDataTest {

    @Test
    public void createKml() throws Exception {
        ExtendedData extendedData = new ExtendedData();
        extendedData.add("one", "1");
        extendedData.add("two", "specialTwo", "2");

        StringBuilder sb = new StringBuilder();
        extendedData.createKml(sb);
        String actual = sb.toString();
        assertEquals("<ExtendedData>" +
                     "<Data name=\"one\">" +
                     "<value>1</value>" +
                     "</Data>" +
                     "<Data name=\"two\">" +
                     "<displayName>specialTwo</displayName>" +
                     "<value>2</value>" +
                     "</Data>" +
                     "</ExtendedData>", actual);

    }

}