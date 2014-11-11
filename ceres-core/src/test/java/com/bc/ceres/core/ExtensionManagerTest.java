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

package com.bc.ceres.core;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class ExtensionManagerTest {

    @Test
    public void testThatFactoriesAreStoredInOrderTheyAreDefined() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        ExtensionFactory[] ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(0, ml.length);

        DefaultModelGuiFactory df = new DefaultModelGuiFactory();
        em.register(Model.class, df);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(1, ml.length);
        assertSame(df, ml[0]);

        DefaultModelGuiFactory df2 = new DefaultModelGuiFactory();
        em.register(Model.class, df2);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(2, ml.length);
        assertSame(df, ml[0]);
        assertSame(df2, ml[1]);

        em.register(Model.class, df2);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(2, ml.length);
        assertSame(df, ml[0]);
        assertSame(df2, ml[1]);

        SlimModelGuiFactory sf = new SlimModelGuiFactory();
        em.register(SlimModel.class, sf);
        ExtensionFactory[] sl = em.getExtensionFactories(SlimModel.class);
        assertNotNull(sl);
        assertEquals(1, sl.length);
        assertSame(sf, sl[0]);

        RichModelGuiFactory rf = new RichModelGuiFactory();
        em.register(RichModel.class, rf);
        ExtensionFactory[] rl = em.getExtensionFactories(RichModel.class);
        assertNotNull(rl);
        assertEquals(1, rl.length);
        assertSame(rf, rl[0]);

        em.unregister(Model.class, df);
        em.unregister(Model.class, df2);
        em.unregister(SlimModel.class, sf);
        em.unregister(RichModel.class, rf);

        assertEquals(0, em.getExtensionFactories(Model.class).length);
        assertEquals(0, em.getExtensionFactories(SlimModel.class).length);
        assertEquals(0, em.getExtensionFactories(RichModel.class).length);
    }

    @Test
    public void testModelWithIndependentGui() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        // These models will be dynamically extended by special GUIs
        Model someModel = new SomeModel();
        Model slimModel = new SlimModel();
        Model richModel = new RichModel();

        // The GUI factories
        DefaultModelGuiFactory defaultModelGuiFactory = new DefaultModelGuiFactory();
        SlimModelGuiFactory slimModelGuiFactory = new SlimModelGuiFactory();
        RichModelGuiFactory richModelGuiFactory = new RichModelGuiFactory();

        /////////////////////////////////////////////////////////////////////
        // Model --> null
        // SomeModel --> null
        // SlimModel --> null
        // RichModel --> null

        testFail(someModel, ModelGui.class);
        testFail(slimModel, ModelGui.class);
        testFail(richModel, ModelGui.class);

        testFail(someModel, DefaultModelGui.class);
        testFail(slimModel, DefaultModelGui.class);
        testFail(richModel, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> DefaultModelGui
        // RichModel --> DefaultModelGui

        em.register(Model.class, defaultModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, ModelGui.class, DefaultModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> SlimModelGui
        // RichModel --> DefaultModelGui

        em.register(SlimModel.class, slimModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, SlimModelGui.class);
        testSuccess(richModel, ModelGui.class, DefaultModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testSuccess(slimModel, SlimModelGui.class, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Any Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> SlimModelGui
        // RichModel --> RichModelGui

        em.register(RichModel.class, richModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, SlimModelGui.class);
        testSuccess(richModel, ModelGui.class, RichModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testSuccess(slimModel, SlimModelGui.class, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testSuccess(richModel, RichModelGui.class, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> null
        // SomeModel --> null
        // SlimModel --> null
        // RichModel --> null

        em.unregister(Model.class, defaultModelGuiFactory);
        em.unregister(SlimModel.class, slimModelGuiFactory);
        em.unregister(RichModel.class, richModelGuiFactory);

        testFail(someModel, ModelGui.class);
        testFail(slimModel, ModelGui.class);
        testFail(richModel, ModelGui.class);

        testFail(someModel, DefaultModelGui.class);
        testFail(slimModel, DefaultModelGui.class);
        testFail(richModel, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);
    }

    private static void testSuccess(Model model,
                                    Class<? extends ModelGui> requestedType,
                                    Class<? extends ModelGui> expectedType) {
        ModelGui modelGui = model.getExtension(requestedType);
        assertNotNull(modelGui);
        assertEquals(expectedType, modelGui.getClass());
        assertSame(model, modelGui.getModel());
    }

    // Note: this is a dummy test, it is a show case for usage of the ExtensionManager API.
    @Test
    public void testCompiles() {
        ExtensionManager em = ExtensionManager.getInstance();

        JComponent component = new JButton();
        JComponent extension1 = em.getExtension("", component.getClass());
        JComponent extension2 = em.getExtension("", JComponent.class);

        JButton button = new JButton();
        JComponent extension3 = em.getExtension("", button.getClass());
        JComponent extension4 = em.getExtension("", JButton.class);
        JButton extension5 = em.getExtension("", button.getClass());
        JButton extension6 = em.getExtension("", JButton.class);

        assertNull(extension1);
        assertNull(extension2);
        assertNull(extension3);
        assertNull(extension4);
        assertNull(extension5);
        assertNull(extension6);
    }

    private static void testFail(Model model, Class<? extends ModelGui> extensionType) {
        ModelGui modelGui = model.getExtension(extensionType);
        assertNull(modelGui);
    }

    static interface Model extends Extensible {
    }

    static class SomeModel extends ExtensibleObject implements Model {
    }

    static class SlimModel extends ExtensibleObject implements Model {
    }

    static class RichModel extends ExtensibleObject implements Model {
    }

    static abstract class ModelGui {
        Model model;

        protected ModelGui(Model model) {
            this.model = model;
        }

        public Model getModel() {
            return model;
        }
    }

    static class DefaultModelGui extends ModelGui {
        DefaultModelGui(Model model) {
            super(model);
        }
    }

    static class SlimModelGui extends ModelGui {
        SlimModelGui(Model model) {
            super(model);
        }
    }

    static class RichModelGui extends ModelGui {
        RichModelGui(Model model) {
            super(model);
        }
    }

    class DefaultModelGuiFactory extends SingleTypeExtensionFactory<Model, ModelGui> {
        DefaultModelGuiFactory() {
            super(ModelGui.class, DefaultModelGui.class);
        }

        @Override
        protected ModelGui getExtensionImpl(Model model, Class<ModelGui> extensionType) throws Throwable {
            return new DefaultModelGui(model);
        }
    }

    class SlimModelGuiFactory extends SingleTypeExtensionFactory<SlimModel, ModelGui> {
        SlimModelGuiFactory() {
            super(ModelGui.class, SlimModelGui.class);
        }

        @Override
        protected ModelGui getExtensionImpl(SlimModel model, Class<ModelGui> extensionType) throws Throwable {
            return new SlimModelGui(model);
        }
    }

    class RichModelGuiFactory extends SingleTypeExtensionFactory<RichModel, ModelGui> {
        RichModelGuiFactory() {
            super(ModelGui.class, RichModelGui.class);
        }

        @Override
        protected ModelGui getExtensionImpl(RichModel model, Class<ModelGui> extensionType) throws Throwable {
            return new RichModelGui(model);
        }
    }
}
