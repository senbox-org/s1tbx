package org.esa.snap.watermask.util;

import java.io.*;

public class ImageDescriptorBuilder {

    private int imageWidth;
    private int imageHeight;
    private int tileWidth;
    private int tileHeight;
    private File auxdataDir;
    private String zipFileName;


    public ImageDescriptorBuilder width(int imageWidth) {
        this.imageWidth = imageWidth;
        return this;
    }

    public ImageDescriptorBuilder height(int imageHeight) {
        this.imageHeight = imageHeight;
        return this;
    }

    public ImageDescriptorBuilder tileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
        return this;
    }

    public ImageDescriptorBuilder tileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
        return this;
    }

    public ImageDescriptorBuilder auxdataDir(File auxdataDir) {
        this.auxdataDir = auxdataDir;
        return this;
    }

    public ImageDescriptorBuilder zipFileName(String fileName) {
        this.zipFileName = fileName;
        return this;
    }

    public ImageDescriptor build() {
        final ImageDescriptorImpl imageDescriptor = new ImageDescriptorImpl();
        imageDescriptor.imageWidth = imageWidth;
        imageDescriptor.imageHeight = imageHeight;
        imageDescriptor.tileWidth = tileWidth;
        imageDescriptor.tileHeight = tileHeight;
        imageDescriptor.auxdataDir = auxdataDir;
        imageDescriptor.zipFileName = zipFileName;
        return imageDescriptor;
    }


    private class ImageDescriptorImpl implements ImageDescriptor {

        private int imageWidth;
        private int imageHeight;
        private int tileWidth;
        private int tileHeight;
        private File auxdataDir;
        private String zipFileName;

        @Override
        public int getImageWidth() {
            return imageWidth;
        }

        @Override
        public int getImageHeight() {
            return imageHeight;
        }

        @Override
        public int getTileWidth() {
            return tileWidth;
        }

        @Override
        public int getTileHeight() {
            return tileHeight;
        }

        @Override
        public File getAuxdataDir() {
            return auxdataDir;
        }

        @Override
        public String getZipFileName() {
            return zipFileName;
        }
    }
}
