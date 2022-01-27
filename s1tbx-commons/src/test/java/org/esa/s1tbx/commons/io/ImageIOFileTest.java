package org.esa.s1tbx.commons.io;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.junit.Assert;
import org.junit.Test;

public class ImageIOFileTest {

  @Test
  public void createImageInputStreamForLargeDimension() throws IOException {
    ImageInputStream imageInputStream = ImageIOFile.createImageInputStream(new ByteArrayInputStream(new byte[0]), new Dimension(60000, 60000));
    Assert.assertTrue("For large files we expect a FileCacheImageInputStream", imageInputStream instanceof FileCacheImageInputStream);
  }
}
