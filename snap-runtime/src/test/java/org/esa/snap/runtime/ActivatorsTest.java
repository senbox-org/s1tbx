package org.esa.snap.runtime;

import org.junit.Assert;
import org.junit.Test;

public class ActivatorsTest {

    @Test
    public void testActivatorsAreCalledInCorrectOrder() throws Exception {

        trace = "";
        Engine engine = Engine.start(false);
        Assert.assertEquals("started A3;started A1;started A2;", trace);

        trace = "";
        engine.stop();
        Assert.assertEquals("stopped A2;stopped A1;stopped A3;", trace);
    }

    public static String trace = "";

    public static class Base implements Activator {
        @Override
        public void start() {
            trace += "started " + getClass().getSimpleName() + ";";
        }

        @Override
        public void stop() {
            trace += "stopped " + getClass().getSimpleName() + ";";
        }
    }

    public static class A1 extends Base {
    }

    public static class A2 extends Base {
        @Override
        public int getStartLevel() {
            return 100;
        }
    }

    public static class A3 extends Base {
        @Override
        public int getStartLevel() {
            return -100;
        }
    }
}
