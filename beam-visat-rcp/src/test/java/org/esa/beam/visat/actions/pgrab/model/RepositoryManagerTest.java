/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.actions.pgrab.model;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.esa.beam.visat.actions.pgrab.model.dataprovider.DataProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.ProductPropertiesProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.WorldMapProvider;

public class RepositoryManagerTest extends TestCase {

    private RepositoryManager _repositoryManager;

    @Override
    protected void setUp() throws Exception {
        _repositoryManager = new RepositoryManager();
    }

    public void testAddingRepositories() {
        final CountingListener listener = new CountingListener();
        _repositoryManager.addListener(listener);

        _repositoryManager.addRepository(new Repository(new File("c:\\data\\dummy")));
        _repositoryManager.addRepository(new Repository(new File("c:\\data\\dummy2")));
        _repositoryManager.addRepository(new Repository(new File("c:\\data\\dummy3")));

        assertEquals(3, _repositoryManager.getNumRepositories());
        assertEquals(3, listener.countedEvents);
    }


    public void testAddingRepositoriesWithSameDir(){
        final File dir = new File("c:\\data\\dummy");
        final CountingListener listener = new CountingListener();
        _repositoryManager.addListener(listener);

        _repositoryManager.addRepository(new Repository(dir));
        _repositoryManager.addRepository(new Repository(dir));

        assertEquals(1, _repositoryManager.getNumRepositories());
        assertEquals(1, listener.countedEvents);
    }

    public void testRemovingRepositories() {
        final CountingListener listener = new CountingListener();
        _repositoryManager.addListener(listener);

        final File dir1 = new File("c:\\data\\dummy1");
        final File dir2 = new File("c:\\data\\dummy2");
        final File dir3 = new File("c:\\data\\dummy3");
        final Repository repository = new Repository(dir1);
        _repositoryManager.addRepository(repository);
        _repositoryManager.addRepository(new Repository(dir2));
        _repositoryManager.addRepository(new Repository(dir3));

        assertEquals(3, _repositoryManager.getNumRepositories());
        assertEquals(3, listener.countedEvents);

        _repositoryManager.removeRepository(repository);
        assertEquals(2, _repositoryManager.getNumRepositories());
        assertEquals(4, listener.countedEvents);

        _repositoryManager.removeRepository(new Repository(dir3));
        assertEquals(1, _repositoryManager.getNumRepositories());
        assertEquals(5, listener.countedEvents);
    }

    public void testGettingRepository(){
        final ArrayList repList = new ArrayList();
        final int numReporitories = 3;
        for(int i = 0; i < numReporitories; i++) {
            final Repository repository = new Repository(new File("c:\\data\\dummy" + i));
            repList.add(repository);
            _repositoryManager.addRepository(repository);
        }
        assertEquals(numReporitories, _repositoryManager.getNumRepositories());

        for(int i = 0; i < numReporitories; i++) {
            final Repository actualRep = _repositoryManager.getRepository(i);
            final Object expectedRep = repList.get(i);
            assertSame(expectedRep,  actualRep);
        }
    }

    public void testAdddingDataProvider() {
        final ProductPropertiesProvider propertiesProvider = new ProductPropertiesProvider();
        final WorldMapProvider wmProvider = new WorldMapProvider(false);
        _repositoryManager.addDataProvider(propertiesProvider);
        _repositoryManager.addDataProvider(wmProvider);

        assertEquals(4, _repositoryManager.getNumDataProviders());
        assertSame(propertiesProvider, _repositoryManager.getDataProvider(2));
        assertSame(wmProvider, _repositoryManager.getDataProvider(3));
    }

    public void testAdddingSameDataProvider2Times() {
        final ProductPropertiesProvider propertiesProvider = new ProductPropertiesProvider();
        _repositoryManager.addDataProvider(propertiesProvider);
        _repositoryManager.addDataProvider(propertiesProvider);

        assertEquals(3, _repositoryManager.getNumDataProviders());
    }

    public void testAddingListener() {
        final CountingListener listener = new CountingListener();
        _repositoryManager.addListener(listener);

        _repositoryManager.addRepository(new Repository(new File("")));

        assertEquals(1, listener.countedEvents);
    }

     public void testRemovingListener() {
        final CountingListener listener = new CountingListener();
        _repositoryManager.addListener(listener);

        _repositoryManager.removeListener(listener);

        _repositoryManager.addRepository(new Repository(new File("")));

        assertEquals(0, listener.countedEvents);

    }

    private static class CountingListener implements RepositoryManagerListener {
        public int countedEvents = 0;

        /**
         * Implementation should handle that a new <code>Repository<code> was added.
         *
         * @param repository the <code>Repository<code> that was added.
         */
        public void repositoryAdded(final Repository repository) {
            countedEvents++;
        }

        /**
         * Implementation should handle that a new <code>Repository<code> was removed.
         *
         * @param repository the <code>Repository<code> that was removed.
         */
        public void repositoryRemoved(final Repository repository) {
            countedEvents++;
        }


        /**
         * Implementation should handle that a new <code>DataProvider<code> was added.
         *
         * @param dataProvider <code>DataProvider<code> that was added.
         */
        public void dataProviderAdded(final DataProvider dataProvider) {
            countedEvents++;
        }
    }
}