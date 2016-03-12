package org.esa.snap.core.util;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.esa.snap.core.util.Debug.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class ServiceFinderTest {

    @Test
    public void testDefaults1() throws Exception {
        ServiceFinder serviceFinder = new ServiceFinder("MyService");
        assertEquals("META-INF/services/MyService", serviceFinder.getServicesPath());
        assertEquals(0, serviceFinder.getSearchPaths().size());
        assertEquals(false, serviceFinder.getUseClassPath());
        assertEquals(false, serviceFinder.getUseZipFiles());
    }

    @Test
    public void testDefaults2() throws Exception {
        ServiceFinder serviceFinder = new ServiceFinder(MyService.class);
        assertEquals("META-INF/services/org.esa.snap.core.util.ServiceFinderTest$MyService", serviceFinder.getServicesPath());
        assertEquals(0, serviceFinder.getSearchPaths().size());
        assertEquals(false, serviceFinder.getUseClassPath());
        assertEquals(false, serviceFinder.getUseZipFiles());
    }

    @Test
    public void testFindInDirsOnly() throws Exception {

        Path path = Paths.get(getClass().getResource("service_finder").toURI());

        ServiceFinder serviceFinder = new ServiceFinder("MyService");
        serviceFinder.addSearchPath(path);

        assertEquals(1, serviceFinder.getSearchPaths().size());
        assertEquals(path, serviceFinder.getSearchPaths().get(0));

        List<ServiceFinder.Module> services = serviceFinder.findServices();
        Collections.sort(services, (o1, o2) -> o1.getPath().compareTo(o2.getPath()));

        assertEquals(2, services.size());
        assertEquals(Paths.get(path.toString(), "module1"), services.get(0).getPath());
        assertEquals(Paths.get(path.toString(), "module2"), services.get(1).getPath());
    }

    @Test
    public void testFindInDirsAndZips() throws Exception {

        Path path = Paths.get(getClass().getResource("service_finder").toURI());

        ServiceFinder serviceFinder = new ServiceFinder("MyService");
        serviceFinder.addSearchPath(path);
        serviceFinder.setUseZipFiles(true);

        assertEquals(1, serviceFinder.getSearchPaths().size());
        assertEquals(path, serviceFinder.getSearchPaths().get(0));

        List<ServiceFinder.Module> services = serviceFinder.findServices();
        Collections.sort(services, (o1, o2) -> o1.getPath().toUri().toString().compareTo(o2.getPath().toUri().toString()));

        assertEquals(4, services.size());
        assertTrue(services.get(0).getPath().toUri().toString().endsWith("/org/esa/snap/core/util/service_finder/module1"));
        assertTrue(services.get(1).getPath().toUri().toString().endsWith("/org/esa/snap/core/util/service_finder/module2"));
        assertTrue(services.get(2).getPath().toUri().toString().endsWith("/org/esa/snap/core/util/service_finder/module3.zip!/"));
        assertTrue(services.get(3).getPath().toUri().toString().endsWith("/org/esa/snap/core/util/service_finder/module4.jar!/"));
    }

    public interface MyService {
        void run();
    }
}
