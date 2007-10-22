package org.esa.beam.framework.gpf.internal;

import com.thoughtworks.xstream.XStream;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OperatorContextTest extends TestCase {

    public void testXStreamArrayConversion() {

        final XStream xStream = new XStream();
        xStream.aliasType("bibo", Bibo.class);
        xStream.aliasField("bibos", Pojo.class, "hugos");

//        xStream.addImplicitCollection(Pojo.class, "aList", "Samson", Bibo.class);


        final Pojo pojo = new Pojo();
        pojo.hugos = new Bibo[]{new Bibo("Ernie"), new Bibo("Bert")};
        pojo.aList = new ArrayList<Bibo>(Arrays.asList(new Bibo("Ernie"), new Bibo("Bert")));

        System.out.println(xStream.toXML(pojo));


    }


    private static class Pojo {
        Bibo[] hugos;
        String[] bandNames = new String[]{"a", "b"};
        double[] wl = new double[]{3, 4, 5};
        List<Bibo> aList;
    }

    private static class Bibo {
        String name;

        private Bibo(String name) {
            this.name = name;
        }
    }
}
