package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.UserInputHistory;
import org.esa.beam.util.PropertyMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class HistoryComboBoxModelTest {

    @Test
    public void testAddElement() {
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
        assertEquals(0, model.getSize());

        model.setSelectedItem("one");
        assertEquals(1, model.getSize());
        model.setSelectedItem("two");
        model.setSelectedItem("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("one", model.getElementAt(2));

        model.setSelectedItem("four");
        assertEquals(3, model.getSize());
        assertEquals("four", model.getElementAt(0));
        assertEquals("three", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("five");
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
        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(map);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.setSelectedItem("three");
        assertEquals(3, model.getSize());
        assertEquals("three", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));
        assertEquals("two", model.getElementAt(2));

        model.setSelectedItem("four");
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

        final UserInputHistory history = new UserInputHistory(3, "historyItem") {
            @Override
            protected boolean isValidItem(String item) {
                return "two".equals(item);

            }
        };
        history.initBy(map);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());
        assertEquals("two", model.getElementAt(0));
    }

    @Test
    public void testSetSelected() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        map.setPropertyString("historyItem.1", "two");

        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(map);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(2, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));

        model.setSelectedItem("two");
        assertEquals(2, model.getSize());
        assertEquals("two", model.getElementAt(0));
        assertEquals("one", model.getElementAt(1));

        model.setSelectedItem("three");
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
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
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

        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(map);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());

        map.setPropertyString("historyItem.1", "two");
        map.setPropertyString("historyItem.2", "three");

        model.getHistory().initBy(map);
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two", model.getElementAt(1));
        assertEquals("three", model.getElementAt(2));
    }

    @Test
    public void testLoadHistoryOverwritesCurrentModel() {
        final PropertyMap map = new PropertyMap();
        map.setPropertyString("historyItem.0", "one");
        final UserInputHistory history = new UserInputHistory(3, "historyItem");
        history.initBy(map);
        final HistoryComboBoxModel model = new HistoryComboBoxModel(history);
        assertEquals(1, model.getSize());
        model.setSelectedItem("two");
        model.setSelectedItem("three");
        assertEquals(3, model.getSize());

        map.setPropertyString("historyItem.1", "two2");
        map.setPropertyString("historyItem.2", "three3");

        model.getHistory().initBy(map);
        assertEquals(3, model.getSize());
        assertEquals("one", model.getElementAt(0));
        assertEquals("two2", model.getElementAt(1));
        assertEquals("three3", model.getElementAt(2));
    }

    @Test
    public void testSaveHistory() {
        final HistoryComboBoxModel model = new HistoryComboBoxModel(new UserInputHistory(3, "historyItem"));
        model.setSelectedItem("one");
        model.setSelectedItem("two");

        final PropertyMap map = new PropertyMap();
        model.getHistory().copyInto(map);
        assertEquals("two", map.getPropertyString("historyItem.0"));
        assertEquals("one", map.getPropertyString("historyItem.1"));
        assertEquals("", map.getPropertyString("historyItem.2"));

        model.setSelectedItem("three");
        model.getHistory().copyInto(map);
        assertEquals("three", map.getPropertyString("historyItem.0"));
        assertEquals("two", map.getPropertyString("historyItem.1"));
        assertEquals("one", map.getPropertyString("historyItem.2"));

    }
}
