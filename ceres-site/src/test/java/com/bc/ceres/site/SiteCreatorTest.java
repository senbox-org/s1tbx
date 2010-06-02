package com.bc.ceres.site;

import org.junit.Test;

import java.net.MalformedURLException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 02.06.2010
 * Time: 13:13:10
 */
public class SiteCreatorTest {

    @Test
    public void testRetrieveVersion() throws MalformedURLException {
        String repoUrl = "http://www.brockmann-consult.de/beam/software/repositories/4.7/";
        assertEquals( "4.7", SiteCreator.retrieveVersion( repoUrl ) );

        repoUrl = "http://www.brockmann-consult.de/beam/software/repositories/4.6";
        assertEquals( "4.6", SiteCreator.retrieveVersion( repoUrl ) );

        repoUrl = "http://www.brockmann-consult.de/beam/software/repositories/4.6/";
        assertEquals( "4.6", SiteCreator.retrieveVersion( repoUrl ) );
    }

}
