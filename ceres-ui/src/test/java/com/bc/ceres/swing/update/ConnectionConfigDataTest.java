package com.bc.ceres.swing.update;

import junit.framework.TestCase;
import com.bc.ceres.core.runtime.ProxyConfig;

public class ConnectionConfigDataTest extends TestCase {

    public void testConnectionConfig() {

        ConnectionConfigData connectionConfigData = new ConnectionConfigData();
        assertNotNull(connectionConfigData.getProxyConfig());
    }
}
