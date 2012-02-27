/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementXStreamConverter;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import junit.framework.TestCase;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public class SessionIOTest extends TestCase {

    private interface LayerIO {

        void write(Layer layer, Writer writer, DomConverter domConverter);

        LayerMemento read(Reader reader);
    }

    public void testGetInstance() {
        assertNotNull(SessionIO.getInstance());
        assertSame(SessionIO.getInstance(), SessionIO.getInstance());
    }

    public void testIO() throws Exception {
        ExtensionManager.getInstance().register(LayerType.class, new GraticuleLayerIOFactory());

        final Session session1 = SessionTest.createTestSession();

        testSession(session1);
        final StringWriter writer = new StringWriter();
        SessionIO.getInstance().writeSession(session1, writer);
        final String xml = writer.toString();
        System.out.println("Session XML:\n" + xml);
        final StringReader reader = new StringReader(xml);
        final Session session2 = SessionIO.getInstance().readSession(reader);
        testSession(session2);
    }

    private void testSession(Session session) {
        assertEquals(Session.CURRENT_MODEL_VERSION, session.getModelVersion());

        assertEquals(2, session.getProductCount());
        testProductRef(session.getProductRef(0), 11, "out/DIMAP/X.dim");
        testProductRef(session.getProductRef(1), 15, "out/DIMAP/Y.dim");

        assertEquals(4, session.getViewCount());
        testViewRef(session.getViewRef(0), 0, ProductSceneView.class.getName(), new Rectangle(0, 0, 200, 100), 11, "A");
        testViewRef(session.getViewRef(1), 1, ProductSceneView.class.getName(), new Rectangle(200, 0, 200, 100), 15,
                    "C");
        testViewRef(session.getViewRef(2), 2, ProductSceneView.class.getName(), new Rectangle(0, 100, 200, 100), 11,
                    "B");
        testViewRef(session.getViewRef(3), 3, ProductSceneView.class.getName(), new Rectangle(200, 100, 200, 100), 15,
                    "D");

        assertEquals(3, session.getViewRef(3).getLayerCount());
        assertEquals("[15] D", session.getViewRef(3).getLayerRef(0).name);
        final Session.LayerRef graticuleLayerRef = session.getViewRef(3).getLayerRef(1);
        assertEquals("Graticule", graticuleLayerRef.name);
        assertNotNull(graticuleLayerRef.configuration);
        assertEquals(13, graticuleLayerRef.configuration.getChildCount());
        final Session.LayerRef maskCollectionLayerRef = session.getViewRef(3).getLayerRef(2);
        assertEquals("Mask Collection", maskCollectionLayerRef.name);
        assertNotNull(maskCollectionLayerRef.configuration);
        assertEquals(0, maskCollectionLayerRef.configuration.getChildCount());
        assertEquals(2, maskCollectionLayerRef.children.length);
        assertEquals("M1", maskCollectionLayerRef.children[0].name);
        assertEquals("M2", maskCollectionLayerRef.children[1].name);
    }

    private void testProductRef(Session.ProductRef productRef, int expectedId, String expectedRelFile) {
        assertEquals(expectedId, productRef.refNo);
        assertEquals(expectedRelFile, productRef.uri.toString());
    }

    private void testViewRef(Session.ViewRef viewRef, int expectedId, String expectedType, Rectangle expectedBounds,
                             int expectedProductId, String expectedProductNodeName) {
        assertEquals(expectedId, viewRef.id);
        assertEquals(expectedType, viewRef.type);
        assertEquals(expectedBounds, viewRef.bounds);
        assertEquals(expectedProductId, viewRef.productRefNo);
        assertEquals(expectedProductNodeName, viewRef.productNodeName);
    }

    static class GraticuleLayerIOFactory extends SingleTypeExtensionFactory<LayerType, LayerIO> {

        private GraticuleLayerIOFactory() {
            super(LayerIO.class, GraticuleLayerIO.class);
        }

        @Override
        protected LayerIO getExtensionImpl(LayerType layerType, Class<LayerIO> extensionType) throws Throwable {
            return new GraticuleLayerIO();
        }
    }

    static class GraticuleLayerIO implements LayerIO {

        private volatile XStream xs;

        @Override
        public void write(Layer layer, Writer writer, DomConverter domConverter) {
            initIO();

            final XppDomElement configuration = new XppDomElement("configuration");
            try {
                domConverter.convertValueToDom(layer, configuration);
            } catch (com.bc.ceres.binding.ConversionException e) {
                e.printStackTrace();
            }

            final LayerMemento memento = new LayerMemento(layer.getLayerType().getClass().getSimpleName(), configuration);
            xs.toXML(memento, writer);
        }

        @Override
        public LayerMemento read(Reader reader) {
            initIO();

            final Object obj = xs.fromXML(reader);
            assertTrue(obj instanceof LayerMemento);

            return (LayerMemento) obj;
        }

        private void initIO() {
            if (xs == null) {
                synchronized (this) {
                    if (xs == null) {
                        xs = new XStream();
                        xs.processAnnotations(LayerMemento.class);
                        xs.alias("configuration", DomElement.class, XppDomElement.class);
                        xs.useAttributeFor(LayerMemento.class, "typeName");
                    }
                }
            }
        }
    }

    @XStreamAlias("layer")
    static class LayerMemento {

        @XStreamAlias("type")
        private String typeName;

        @XStreamConverter(DomElementXStreamConverter.class)
        private DomElement configuration;

        LayerMemento(String typeName, DomElement configuration) {
            this.typeName = typeName;
            this.configuration = configuration;
        }

        DomElement getConfiguration() {
            return configuration;
        }
    }
}
