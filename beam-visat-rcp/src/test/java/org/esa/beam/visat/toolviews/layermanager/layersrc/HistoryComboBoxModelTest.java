package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.util.PropertyMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class HistoryComboBoxModelTest {

    @Test
    public void testAddElement() {
        final PropertyMap map = new PropertyMap();
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(0, model.getSize());

        model.addElement("one");
        assertEquals(1, model.getSize());
        model.addElement("two");
        model.addElement("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

        model.addElement("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.addElement("five");
        assertEquals(3, model.getSize());
        assertEquals("five", model.getElementAt(0));
        assertEquals("four", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));
    }

    @Test
    public void testAddElementWithInnitilaizedProperties() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        map.setPropertyString("historyItem.1", "two");
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.addElement("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.addElement("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

    }


    @Test
    public void testValidation() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        map.setPropertyString("historyItem.1", "two");
        map.setPropertyString("historyItem.2", "three");

        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3,
                                                                    new HistoryComboBoxModel.Validator() {
                                                                        @Override
                                                                        public boolean isValid(String entry) {
                                                                            return "two".equals(entry);
                                                                        }
                                                                    });
        assertEquals(1, model.getSize());
        assertEquals("two", model.getElementAt(0));
    }

    @Test
    public void testSetSelected() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        map.setPropertyString("historyItem.1", "two");
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.setSelectedItem("two");
        assertEquals(2, model.getSize());
        assertEquals("two", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));

        model.addElement("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

        model.setSelectedItem("one");
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));

    }

    @Test
    public void testSetSelectedOnEmptyHistory() {
        final PropertyMap map = new PropertyMap();
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(0, model.getSize());

        model.setSelectedItem("one");
        assertEquals(1, model.getSize());
        assertEquals("one", model.getElementAt(0));

        model.setSelectedItem("two");
        assertEquals(2, model.getSize());
        assertEquals("two", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
    }

    @Test
    public void testLoadHistory() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(1, model.getSize());

        map.setPropertyString("historyItem.1", "two");
        map.setPropertyString("historyItem.2", "three");

        model.loadHistory();
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));
    }

    @Test
    public void testLoadHistoryOverwritesCurrentModel() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        assertEquals(1, model.getSize());
        model.addElement("two");
        model.addElement("three");
        assertEquals(3, model.getSize());

        map.setPropertyString("historyItem.1", "two2");
        map.setPropertyString("historyItem.2", "three3");

        model.loadHistory();
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two2", model.getElementAt(1));
        assertEquals("three3", model.getElementAt(2));
    }

    @Test
    public void testSaveHistory() {
        final PropertyMap map = new PropertyMap();
        final HistoryComboBoxModel model = new HistoryComboBoxModel(map, "historyItem", 3);
        model.addElement("one");
        model.addElement("two");

        model.saveHistory();
        assertEquals("two", map.getPropertyString("historyItem.0"));
        assertEquals("one", map.getPropertyString("historyItem.1"));
        assertEquals("", map.getPropertyString("historyItem.2"));

        model.addElement("three");
        model.saveHistory();
        assertEquals("three", map.getPropertyString("historyItem.0"));
        assertEquals("two", map.getPropertyString("historyItem.1"));
        assertEquals("one", map.getPropertyString("historyItem.2"));

        model.removeElementAt(1);
        model.saveHistory();
        assertEquals("three", map.getPropertyString("historyItem.0"));
        assertEquals("one", map.getPropertyString("historyItem.1"));
        assertEquals("", map.getPropertyString("historyItem.2"));
    }
}
