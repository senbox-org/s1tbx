package com.bc.ceres.core.runtime;

import junit.framework.TestCase;

public class ProxyConfigTest extends TestCase {

    public void testDefaultValues() {
        ProxyConfig proxyConfig = new ProxyConfig();
        assertEquals("", proxyConfig.getHost());
        assertEquals(0, proxyConfig.getPort());
        assertEquals(false, proxyConfig.isAuthorizationUsed());
        assertEquals("", proxyConfig.getUsername());
        assertNotNull(proxyConfig.getPassword());
        assertEquals(0, proxyConfig.getPassword().length);    
    }

    public void testCryptDecrypt() {

        String s = ProxyConfig.scramble("An4nas?");
        assertNotNull(s);
        assertFalse(s.equals("An4nas?"));
        assertFalse(s.contains("An4nas?"));

        String t = ProxyConfig.descramble(s);
        assertTrue(t.equals("An4nas?"));
    }
}
