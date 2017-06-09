/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.datamodel;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Test Credentials Class
 */
public class TestCredentials {

    private static final String host = "https://some.website.com";

    @Test
    public void testAdd() {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(host);
        if (credentialInfo == null) {
            //add
            Credentials.instance().put(host, "testuser", "testpassword");
            credentialInfo = Credentials.instance().get(host);
        }

        assertNotNull(credentialInfo);
        assertEquals("testuser", credentialInfo.getUser());
        assertEquals("testpassword", credentialInfo.getPassword());
    }
}
