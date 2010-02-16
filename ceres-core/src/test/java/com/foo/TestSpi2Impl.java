package com.foo;

import com.acme.TestSpi2;
import org.junit.Ignore;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
@Ignore
public class TestSpi2Impl implements TestSpi2 {

    public Object createAnotherService() {
        return new int[]{};
    }
}
