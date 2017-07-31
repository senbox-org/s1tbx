package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.internal.matchers.EndsWith;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProductLoopTest {

    private StatisticComputer statisticComputerMock;
    private ProductLoop productLoop;
    private ProductLoader productLoaderMock;
    private Logger loggerMock;
    private ProductData.UTC startDate;
    private ProductData.UTC endDate;
    private Product validProductMock1;
    private Product validProductMock2;
    private ProductValidator validatorMock;

    @Before
    public void setUp() throws Exception {
        statisticComputerMock = mock(StatisticComputer.class);
        productLoaderMock = mock(ProductLoader.class);
        loggerMock = mock(Logger.class);
        startDate = ProductData.UTC.parse("22-MAR-2008 00:00:00");
        endDate = ProductData.UTC.parse("15-SEP-2008 00:00:00");
        validProductMock1 = createTimeValidProductMock(4, 3);
        validProductMock2 = createTimeValidProductMock(7, 2);
        validatorMock = mock(ProductValidator.class);
        when(validatorMock.isValid(any(Product.class))).thenReturn(true);
        productLoop = new ProductLoop(productLoaderMock, validatorMock, statisticComputerMock, loggerMock);
    }

    @Test
    public void testThatAlreadyLoadedProductsShouldNotBeDisposedAfterStatisticComputation() {
        // preparation
        final Product[] loadedProducts = {validProductMock1, validProductMock2};

        // execution
        productLoop.loop(loadedProducts, new File[0]);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock1));
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock2));
        verify(validProductMock1, never()).dispose();
        verify(validProductMock2, never()).dispose();
    }

    @Test
    public void testThatLoadedProductsShouldBeDisposedAfterStatisticComputation() throws IOException {
        // preparation
        final File file1 = new File("1");
        final File file2 = new File("2");
        when(productLoaderMock.loadProduct(file1)).thenReturn(validProductMock1);
        when(productLoaderMock.loadProduct(file2)).thenReturn(validProductMock2);
        final File[] productFilesToLoad = {file1, file2};

        // execution
        productLoop.loop(new Product[0], productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock1));
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock2));
        verify(validProductMock1, times(1)).dispose();
        verify(validProductMock2, times(1)).dispose();
    }

    @Test
    public void testThatFilesWhichPointToAnAlreadyLoadedProductShouldNotBeOpenedTwice() throws IOException {
        // preparation
        final File file1 = validProductMock1.getFileLocation();
        final File file2 = validProductMock2.getFileLocation(); // points to productMock2
        when(productLoaderMock.loadProduct(file1)).thenReturn(validProductMock1);
        final Product[] alreadyLoadedProducts = {validProductMock2};
        final File[] productFilesToLoad = {file1, file2};

        // execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(statisticComputerMock);
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock2));
        inOrder.verify(statisticComputerMock).computeStatistic(same(validProductMock1));
        verify(productLoaderMock).loadProduct(eq(file1));
        verify(validProductMock1, times(1)).dispose();
        verify(validProductMock2, never()).dispose();
        verify(validProductMock2, atLeastOnce()).getFileLocation();
    }

    @Test
    public void testThatNoOperatorExceptionOccursIfNoProductsAreComputed() {
        try {
            productLoop.loop(new Product[0], new File[0]);
            assertEquals(0, productLoop.getProductNames().length);
        } catch (OperatorException expected) {
            assertEquals("No input products found.", expected.getMessage());
        }
    }

    @Test
    public void testThatFilesWhichCanNotBeOpenedAreLogged() throws IOException {
        //preparation
        final File file1 = validProductMock1.getFileLocation();
        final File file2 = new File("No reader available"); // no reader found
        when(productLoaderMock.loadProduct(file1)).thenReturn(validProductMock1);
        when(productLoaderMock.loadProduct(file2)).thenReturn(null); // no reader found
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(loggerMock).severe("Failed to read from 'No reader available' (not a data product or reader missing)");
        verify(statisticComputerMock, times(1)).computeStatistic(validProductMock1);
        verify(productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(statisticComputerMock, productLoaderMock);
    }

    @Test
    public void testThatIOExceptionsAreLogged() throws IOException {
        //preparation
        final File file1 = validProductMock1.getFileLocation();
        final File file2 = new File("Causes IO Exception"); // IO Exception
        when(productLoaderMock.loadProduct(file1)).thenReturn(validProductMock1);
        when(productLoaderMock.loadProduct(file2)).thenThrow(new IOException()); // IO Exception
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(loggerMock).severe("Failed to read from 'Causes IO Exception' (not a data product or reader missing)");
        verify(statisticComputerMock, times(1)).computeStatistic(validProductMock1);
        verify(productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(statisticComputerMock, productLoaderMock);
    }

    @Test
    public void testThatAlreadyLoadedProductsAreSkippedIfOutOfDateRange() {
        //preparation
        final Product productMockBefore = createProductMock(4, 2, true, false);
        final Product productMockAfter = createProductMock(4, 2, false, true);
        final Product[] alreadyLoadedProducts = {productMockBefore, productMockAfter, validProductMock1};
        when(validatorMock.isValid(productMockBefore)).thenReturn(false);
        when(validatorMock.isValid(productMockAfter)).thenReturn(false);

        //execution
        productLoop.loop(alreadyLoadedProducts, new File[0]);

        //verification
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verifyNoMoreInteractions(statisticComputerMock);
        verify(productMockBefore, never()).dispose();
        verify(productMockAfter, never()).dispose();
        verify(validProductMock1, never()).dispose();
    }

    @Test
    public void testThatProductsToBeLoadedAreSkippedIfOutOfDateRange() throws IOException {
        //preparation
        final File file1 = new File("before");
        final File file2 = new File("after");
        final File file3 = validProductMock1.getFileLocation();
        final Product productMockBefore = createProductMock(4, 2, true, false);
        final Product productMockAfter = createProductMock(4, 2, false, true);
        when(productLoaderMock.loadProduct(file1)).thenReturn(productMockBefore);
        when(productLoaderMock.loadProduct(file2)).thenReturn(productMockAfter);
        when(productLoaderMock.loadProduct(file3)).thenReturn(validProductMock1);
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = new File[]{file1, file2, file3};
        when(validatorMock.isValid(productMockBefore)).thenReturn(false);
        when(validatorMock.isValid(productMockAfter)).thenReturn(false);

        //execution
        productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verifyNoMoreInteractions(statisticComputerMock);
        verify(productMockBefore, times(1)).dispose();
        verify(productMockAfter, times(1)).dispose();
        verify(validProductMock1, times(1)).dispose();
    }

    @Test
    public void testThatLoopWorksIfAlreadyLoadedProductsIsNull() throws IOException {
        //preparation
        File file = validProductMock1.getFileLocation();
        when(productLoaderMock.loadProduct(file)).thenReturn(validProductMock1);

        //execution
        productLoop.loop(null, new File[]{file});

        //verification
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verify(validProductMock1, times(1)).dispose();
    }

    @Test
    public void testThatLoopWorksIfAlreadyLoadedProductsContainsNullValues() throws IOException {
        //preparation
        final Product[] alreadyLoadedProducts = {validProductMock1, null, validProductMock2};

        //execution
        productLoop.loop(alreadyLoadedProducts, new File[0]);

        //verification
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verify(statisticComputerMock).computeStatistic(validProductMock2);
        verify(validProductMock1, never()).dispose();
        verify(validProductMock2, never()).dispose();
        final String[] productNames = productLoop.getProductNames();
        assertEquals(2, productNames.length);
        assertThat(productNames[0], endsWith(validProductMock1.getFileLocation().getName()));
        assertThat(productNames[1], endsWith(validProductMock2.getFileLocation().getName()));
    }

    @Test
    public void testThatLoopWorksIfProductFilesToLoadContainsNullValues() throws IOException {
        //preparation
        final File file1 = validProductMock1.getFileLocation();
        final File file2 = validProductMock2.getFileLocation();
        final File[] productFilesToLoad = new File[]{file1, file2};
        when(productLoaderMock.loadProduct(file1)).thenReturn(validProductMock1);
        when(productLoaderMock.loadProduct(file2)).thenReturn(validProductMock2);

        //execution
        productLoop.loop(null, productFilesToLoad);

        //verification
        verify(productLoaderMock).loadProduct(file1);
        verify(productLoaderMock).loadProduct(file2);
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verify(statisticComputerMock).computeStatistic(validProductMock2);
        verifyNoMoreInteractions(statisticComputerMock, productLoaderMock);
        verify(validProductMock1, times(1)).dispose();
        verify(validProductMock2, times(1)).dispose();
        final String[] productNames = productLoop.getProductNames();
        assertEquals(2, productNames.length);
        assertThat(productNames[0], endsWith(file1.getName()));
        assertThat(productNames[1], endsWith(file2.getName()));
    }

    @Test
    public void testThatComputationIsPerformedWhenStartAndEndTimeOfProductAreNotSet() {
        //preparation
        final Product[] alreadyLoadedProducts = {validProductMock1, validProductMock2};
        when(validProductMock1.getStartTime()).thenReturn(null);
        when(validProductMock2.getStartTime()).thenReturn(null);
        when(validProductMock1.getEndTime()).thenReturn(null);
        when(validProductMock2.getEndTime()).thenReturn(null);

        //execution
        productLoop.loop(alreadyLoadedProducts, new File[0]);

        //verification
        verify(statisticComputerMock).computeStatistic(validProductMock1);
        verify(statisticComputerMock).computeStatistic(validProductMock2);
        verifyNoMoreInteractions(statisticComputerMock);
        verify(validProductMock1, never()).dispose();
        verify(validProductMock2, never()).dispose();
        final String[] productNames = productLoop.getProductNames();
        assertEquals(2, productNames.length);
        assertThat(productNames[0], endsWith(validProductMock1.getFileLocation().getName()));
        assertThat(productNames[1], endsWith(validProductMock2.getFileLocation().getName()));
    }

    private Product createTimeValidProductMock(int startOffset, int numObservationDays) {
        return createProductMock(startOffset, numObservationDays, false, false);
    }

    private Product createProductMock(int startOffset, int numObservationDays, boolean before_startDate, boolean after_endDate) {
        final ProductData.UTC startTime = getStartTime(startOffset, numObservationDays, before_startDate, after_endDate);
        final ProductData.UTC endTime = new ProductData.UTC(startTime.getMJD() + numObservationDays);
        final File fileLocation = getFileLocation(startOffset, numObservationDays, before_startDate, after_endDate);

        final Product mock = mock(Product.class);
        when(mock.getStartTime()).thenReturn(startTime);
        when(mock.getEndTime()).thenReturn(endTime);
        when(mock.getFileLocation()).thenReturn(fileLocation);
        when(mock.getName()).thenReturn(fileLocation.getName());
        return mock;
    }

    private ProductData.UTC getStartTime(int startOffset, int numObservationDays, boolean before_startDate, boolean after_endDate) {
        final ProductData.UTC startTime;
        if (before_startDate) {
            startTime = new ProductData.UTC(startDate.getMJD() - startOffset - numObservationDays);
        } else if (after_endDate) {
            startTime = new ProductData.UTC(endDate.getMJD() + startOffset);
        } else {
            startTime = new ProductData.UTC(startDate.getMJD() + startOffset);
        }
        return startTime;
    }

    private File getFileLocation(int startOffset, int numObservationDays, boolean before, boolean after) {
        final StringBuilder location = new StringBuilder();
        location.append("mock_loc_");
        if (before) {
            location.append("before_");
        } else if (after) {
            location.append("after_");
        }
        location.append(startOffset).append("_");
        location.append(numObservationDays);
        return new File(location.toString());
    }

    private EndsWith endsWith(final String suffix) {
        return new EndsWith(suffix);
    }
}
