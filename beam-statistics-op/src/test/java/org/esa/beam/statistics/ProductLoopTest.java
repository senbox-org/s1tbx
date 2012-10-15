package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ProductLoopTest {

    private StatisticComputer statisticComputerMock;
    private ProductLoop productLoop;
    private ProductLoader productLoaderMock;
    private Logger loggerMock;

    @Before
    public void setUp() throws Exception {
        statisticComputerMock = mock(StatisticComputer.class);
        productLoaderMock = mock(ProductLoader.class);
        loggerMock = mock(Logger.class);
        productLoop = new ProductLoop(productLoaderMock, statisticComputerMock, null, null, loggerMock);
    }

    @Test
    public void testThatAlreadyLoadedProductsShouldNotBeDisposedAfterStatisticComputation() {
        // preparation
        final Product productMock1 = mock(Product.class);
        final Product productMock2 = mock(Product.class);
        final Product[] loadedProducts = {productMock1, productMock2};

        // execution
        productLoop.loop(loadedProducts, new File[0]);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock1));
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock2));
        verify(productMock1, never()).dispose();
        verify(productMock2, never()).dispose();
    }

    @Test
    public void testThatLoadedProductsShouldBeDisposedAfterStatisticComputation() throws IOException {
        // preparation
        final File file1 = new File("1");
        final File file2 = new File("2");
        final Product productMock1 = mock(Product.class);
        final Product productMock2 = mock(Product.class);
        when(productLoaderMock.loadProduct(file1)).thenReturn(productMock1);
        when(productLoaderMock.loadProduct(file2)).thenReturn(productMock2);
        final File[] productFilesToLoad = {file1, file2};

        // execution
        productLoop.loop(new Product[0], productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock1));
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock2));
        verify(productMock1, times(1)).dispose();
        verify(productMock2, times(1)).dispose();
    }

    @Test
    public void testThatFilesWhichPointsToAnAlreadyLoadedProductShouldNotBeOpenedTwice() throws IOException {
        // preparation
        final File file1 = new File("1");
        final File file2 = new File("2"); // pints To productMock2
        final Product productMock1 = mock(Product.class);
        final Product productMock2 = mock(Product.class);
        when(productLoaderMock.loadProduct(file1)).thenReturn(productMock1);
        when(productMock2.getFileLocation()).thenReturn(new File("2")); // file pointer
        final Product[] alreadyLoadedProducts = {productMock2};
        final File[] productFilesToLoad = {file1, file2};

        // execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock2));
        inOrder.verify(statisticComputerMock).computeStatistic(same(productMock1));
        verify(productLoaderMock).loadProduct(eq(new File("1")));
        verify(productMock1, times(1)).dispose();
        verify(productMock2, atLeastOnce()).getFileLocation();
    }

    @Test
    public void testThatOperatorExceptionOccuresIfNoProductsAreComputed() {
        try {
            productLoop.loop(new Product[0], new File[0]);
            fail("OperatorExceptionExpected");
        } catch (OperatorException expected) {
            assertEquals("No input products found.", expected.getMessage());
        }
    }

    @Test
    public void testThatFilesWhichCanNotBeOpenedAreLogged() throws IOException {
        //preparation
        final File file1 = new File("1");
        final File file2 = new File("No reader available"); // no reader found
        final Product productMock1 = mock(Product.class);
        when(productLoaderMock.loadProduct(file1)).thenReturn(productMock1);
        when(productLoaderMock.loadProduct(file2)).thenReturn(null); // no reader found
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(loggerMock).severe("Failed to read from 'No reader available' (not a data product or reader missing)");
        verify(statisticComputerMock, times(1)).computeStatistic(any(Product.class));
        verify(productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(statisticComputerMock, productLoaderMock, loggerMock);
    }

    @Test
    public void testThatIOExceptionsAreLogged() throws IOException {
        //preparation
        final File file1 = new File("1");
        final File file2 = new File("Causes IO Exception"); // IO Exception
        final Product productMock1 = mock(Product.class);
        when(productLoaderMock.loadProduct(file1)).thenReturn(productMock1);
        when(productLoaderMock.loadProduct(file2)).thenThrow(new IOException()); // IO Exception
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(loggerMock).severe("Failed to read from 'Causes IO Exception' (not a data product or reader missing)");
        verify(statisticComputerMock, times(1)).computeStatistic(any(Product.class));
        verify(productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(statisticComputerMock, productLoaderMock, loggerMock);
    }
}
