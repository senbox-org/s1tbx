package com.foo;

import com.acme.TestSpi1;
import org.junit.Ignore;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
@Ignore
public class TestSpi1Impl implements TestSpi1 {

    public Object createService() {
        return "";
    }
}
