package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;
import com.bc.ceres.swing.figure.support.FigureEditorPanel;
import junit.framework.TestCase;

import javax.swing.JMenu;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class AbstractInteractorListenerTest extends TestCase {

    public void testInterceptorImpl() {
        AbstractInteractorListener listener = new AbstractInteractorListener() {
        };
        assertEquals(true, listener.canActivateInteractor(null));
        assertEquals(true, listener.canStartInteraction(null, null));
    }
}