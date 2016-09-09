package org.esa.snap.dataio.hdf5;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class Hdf5ProductWriterTest {

    @Test
    public void testEnumerateChildrenWithSameName() throws Exception {
        MetadataElement childOne = new MetadataElement("child");
        childOne.addAttribute(new MetadataAttribute("1", ProductData.TYPE_INT8));
        MetadataElement childTwo = new MetadataElement("child");
        childTwo.addAttribute(new MetadataAttribute("2", ProductData.TYPE_INT8));
        MetadataElement childThree = new MetadataElement("child");
        childThree.addAttribute(new MetadataAttribute("3", ProductData.TYPE_INT8));

        MetadataElement parent = new MetadataElement("parent");
        parent.addElement(new MetadataElement("one"));
        parent.addElement(childOne);
        parent.addElement(childTwo);
        parent.addElement(childThree);
        parent.addElement(new MetadataElement("sister"));
        parent.addElement(new MetadataElement("brother"));

        List<MetadataElement> metadataElements = Hdf5ProductWriter.enumerateChildrenWithSameName(parent);

        assertEquals("one", metadataElements.get(0).getName());
        assertEquals("child.1", metadataElements.get(1).getName());
        assertEquals("1", metadataElements.get(1).getAttributeAt(0).getName());
        assertEquals("child.2", metadataElements.get(2).getName());
        assertEquals("2", metadataElements.get(2).getAttributeAt(0).getName());
        assertEquals("child.3", metadataElements.get(3).getName());
        assertEquals("3", metadataElements.get(3).getAttributeAt(0).getName());
        assertEquals("sister", metadataElements.get(4).getName());
        assertEquals("brother", metadataElements.get(5).getName());

    }

    @Test
    public void testEnumerateChildrenWithSameName_AllNamesAreDifferent() throws Exception {
        MetadataElement parent = new MetadataElement("parent");
        parent.addElement(new MetadataElement("one"));
        parent.addElement(new MetadataElement("sister"));
        parent.addElement(new MetadataElement("brother"));

        List<MetadataElement> metadataElements = Hdf5ProductWriter.enumerateChildrenWithSameName(parent);

        assertEquals("one", metadataElements.get(0).getName());
        assertEquals("sister", metadataElements.get(1).getName());
        assertEquals("brother", metadataElements.get(2).getName());

    }
}