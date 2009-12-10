/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.esa.beam.dataio.propertystore;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * DataStore factory that creates {@linkplain org.geotools.data.property.PropertyDataStore}s
 *
 * @author jgarnett
 * @source $URL: http://svn.osgeo.org/geotools/trunk/modules/plugin/property/src/main/java/org/geotools/data/property/PropertyDataStoreFactory.java $
 * @version $Id: PropertyDataStoreFactory.java 32332 2009-01-26 18:54:59Z aaime $
 */
class PropertyDataStoreFactory implements DataStoreFactorySpi {
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(PropertyDataStoreFactory.class.getPackage().getName());
	
	
	//    public DataSourceMetadataEnity createMetadata( Map params )
    //            throws IOException {
    //        if( !canProcess( params )){
    //            throw new IOException( "Provided params cannot be used to connect");
    //        }
    //        File dir = (File) DIRECTORY.lookUp( params );
    //        return new DataSourceMetadataEnity( dir, "Property file access for " + dir );        
    //    }    

    /** DOCUMENT ME!  */
    public static final Param DIRECTORY = new Param("directory", File.class,
            "Directory containting property files", true);

    public static final Param NAMESPACE = new Param("namespace", String.class, 
    		"namespace of datastore", false );
    
    /**
     * DOCUMENT ME!
     *
     * @param params DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public DataStore createDataStore(Map params) throws IOException {
    	File dir = directoryLookup(params);
        String namespaceURI = (String) NAMESPACE.lookUp( params );
        if (dir.exists() && dir.isDirectory()) {
            return new PropertyDataStore(dir,namespaceURI);
        } else {
            throw new IOException("Directory is required");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param params DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public DataStore createNewDataStore(Map params) throws IOException {
    	File dir = directoryLookup(params);

        if (dir.exists()) {
            throw new IOException(dir + " already exists");
        }

        boolean created;

        created = dir.mkdir();

        if (!created) {
            throw new IOException("Could not create the directory" + dir);
        }

        String namespaceURI = (String) NAMESPACE.lookUp(params);
        return new PropertyDataStore(dir,namespaceURI);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getDisplayName() {
        return "Properties";
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getDescription() {
        return "Allows access to Java Property files containing Feature information";
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Param[] getParametersInfo() {
        return new Param[] { DIRECTORY, NAMESPACE };
    }

    /**
     * Test to see if this datastore is available, if it has all the
     * appropriate libraries to construct a datastore.  This datastore just
     * returns true for now.  This method is used for gui apps, so as to not
     * advertise data store capabilities they don't actually have.
     *
     * @return <tt>true</tt> if and only if this factory is available to create
     *         DataStores.
     *
     * @task REVISIT: I'm just adding this method to compile, maintainer should
     *       revisit to check for any libraries that may be necessary for
     *       datastore creations. ch.
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param params DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean canProcess(Map params) {
        try {
        	directoryLookup(params);
            return true;
        } catch (Exception erp) {
            //can't process, just return false
            return false;
        }
    }
    
    /**
     */
    public Map getImplementationHints(){
        return java.util.Collections.EMPTY_MAP;
    }
    
    /**
     * Lookups the directory containing property files in the params argument, and
     * returns the corresponding <code>java.io.File</code>.
     * <p>
     * The file is first checked for existence as an absolute path in the filesystem. If
     * such a directory is not found, then it is treated as a relative path, taking Java
     * system property <code>"user.dir"</code> as the base.
     * </p>
     * @param params
     * @throws IllegalArgumentException if directory is not a directory.
     * @throws FileNotFoundException if directory does not exists
     * @throws IOException if {@linkplain #DIRECTORY} doesn't find parameter in <code>params</code>
     * file does not exists.
     */
    private File directoryLookup(Map params)throws IOException, FileNotFoundException,IllegalArgumentException{
    	File directory = (File)DIRECTORY.lookUp(params);
    	if(!directory.exists()){
    		File currentDir = new File(System.getProperty("user.dir"));
    		directory = new File(currentDir, (String)params.get(DIRECTORY.key));
    		if(!directory.exists()){
    			throw new FileNotFoundException(directory.getAbsolutePath());
    		}
    		if(!directory.isDirectory()){
    			throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory");
    		}
    	} else if(!directory.isDirectory()) {
    	        throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory");
    	}
    	return directory;
    }
}
