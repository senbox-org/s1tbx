package org.esa.beam.framework;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class TesstX extends TestCase {


    public void testX() {
        final XStream xStream = new XStream();
        xStream.aliasType("descriptor", Heinz.class);
        xStream.autodetectAnnotations(true);
        final Heinz heinz = new Heinz();
        xStream.fromXML("<descriptor>" +
                        "<list>" +
                        "<id>" + "id1" + "</id>" +
                        "<id>" + "id2" + "</id>" +
                        "<id>" + "id3" + "</id>" +
                        "</list>" +
                        "</descriptor>", heinz);

        assertNotNull(heinz.elements);
        assertNotNull(heinz.elements.elems);
        assertEquals(3, heinz.elements.elems.size());
    }


    @XStreamAlias("descriptor")
    public static class Heinz {
        @XStreamAlias("list")
        MyList elements;
    }

    public static class MyList {
        @XStreamImplicit(itemFieldName = "id")
        ArrayList<String> elems;

    }
}
