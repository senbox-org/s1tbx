package com.foo;

import com.acme.TestSpi2;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class TestSpi2Impl implements TestSpi2 {

    public Object createAnotherService() {
        return new int[]{};
    }
}
