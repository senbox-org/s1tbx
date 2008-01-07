package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;

import javax.swing.JTree;

public class DefaultSelectionProviderTest extends TestCase {
    public void testTree() {
        final JTree tree = new JTree();
        final TreeSelectionProvider selectionProvider = new TreeSelectionProvider(tree);

        assertEquals(DefaultSelection.EMPTY, selectionProvider.getSelection());
    }
}
