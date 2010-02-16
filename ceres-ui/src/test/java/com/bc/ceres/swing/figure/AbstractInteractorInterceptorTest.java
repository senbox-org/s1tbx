package com.bc.ceres.swing.figure;

import junit.framework.TestCase;

public class AbstractInteractorInterceptorTest extends TestCase {

    public void testInterceptorImpl() {
        InteractorInterceptor listener = new AbstractInteractorInterceptor() {
        };
        assertEquals(true, listener.interactorAboutToActivate(null));
        assertEquals(true, listener.interactionAboutToStart(null, null));
    }
}