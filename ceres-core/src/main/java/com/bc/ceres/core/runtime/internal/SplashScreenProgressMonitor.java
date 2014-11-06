/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeConfig;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * A progress monitor which uses a splash screen.
 */
public class SplashScreenProgressMonitor extends NullProgressMonitor {
    private static final String CONFIG_KEY_SPLASH_IMAGE = "splash.image";
    private static final String CONFIG_KEY_SPLASH_PROGRESS_BAR_COLOR = "splash.progressBar.color";
    private static final String CONFIG_KEY_SPLASH_PROGRESS_BAR_AREA = "splash.progressBar.area";
    private static final String CONFIG_KEY_TASK_LABEL_ENABLED = "splash.taskLabel.enabled";
    private static final String CONFIG_KEY_TASK_LABEL_FONT = "splash.taskLabel.font";
    private static final String CONFIG_KEY_TASK_LABEL_COLOR = "splash.taskLabel.color";
    private static final String CONFIG_KEY_TASK_LABEL_POS = "splash.taskLabel.pos";
    private static final String CONFIG_KEY_SPLASH_VERSION_TEXT = "splash.version.text";
    private static final String CONFIG_KEY_VERSION_ENABLED = "splash.version.enabled";
    private static final String CONFIG_KEY_VERSION_FONT = "splash.version.font";
    private static final String CONFIG_KEY_VERSION_COLOR = "splash.version.color";
    private static final String CONFIG_KEY_VERSION_POS = "splash.version.pos";

    private Splash splashScreen;
    private String taskName;
    private String subTaskName;
    private double totalWork;
    private double currentWork;
    private int lastPixelsWorked;
    private Graphics2D graphics;
    private Rectangle splashArea;
    private int progressBarHeight;
    private boolean taskLabelEnabled;
    private Font taskLabelFont;
    private Color taskLabelColor;
    private Point taskLabelPos;
    private boolean versionLabelEnabled;
    private Font versionLabelFont;
    private Color versionLabelColor;
    private Point versionLabelPos;
    private Color progressBarColor;
    private Rectangle progressBarArea;
    private int pixelsWorked;
    private String message;
    private String versionText;

    /**
     * Creates a {@link SplashScreenProgressMonitor} only if a splash screen exists.
     *
     * @param config The runtime configuration
     * @return an instance of {@link SplashScreenProgressMonitor} or {@link NullProgressMonitor}
     */
    public static ProgressMonitor createProgressMonitor(RuntimeConfig config) {
        String imageFilePath = config.getContextProperty(CONFIG_KEY_SPLASH_IMAGE);
        if (imageFilePath != null) {
            BufferedImage bufferedImage = loadImage(imageFilePath);
            if (bufferedImage != null) {
                return new SplashScreenProgressMonitor(new CeresSplash(bufferedImage), config);
            }
        } else {
            SplashScreen splashScreen = null;
            try {
                splashScreen = SplashScreen.getSplashScreen();
            } catch (Throwable t) {
                //
            }
            if (splashScreen != null) {
                return new SplashScreenProgressMonitor(new AwtSplash(splashScreen), config);
            }
        }
        return ProgressMonitor.NULL;
    }

    public SplashScreenProgressMonitor(Splash splashScreen, RuntimeConfig config) {
        Assert.notNull(splashScreen, "splashScreen");
        this.splashScreen = splashScreen;

        Dimension size = this.splashScreen.getSize();
        splashArea = new Rectangle(size);

        progressBarColor = getConfiguredColor(config, CONFIG_KEY_SPLASH_PROGRESS_BAR_COLOR, Color.GREEN);
        progressBarArea = getConfiguredSplashProgressBarArea(config);
        if (progressBarArea != null) {
            progressBarHeight = progressBarArea.height;
        } else {
            progressBarHeight = 5;
            progressBarArea = new Rectangle(0,
                                            splashArea.height - progressBarHeight - 4,
                                            splashArea.width,
                                            progressBarHeight);
        }

        taskLabelColor = getConfiguredColor(config, CONFIG_KEY_TASK_LABEL_COLOR, Color.WHITE);
        taskLabelEnabled = getConfiguredOptionEnabled(config, CONFIG_KEY_TASK_LABEL_ENABLED);
        if (taskLabelEnabled) {
            taskLabelFont = getConfiguredFont(config, CONFIG_KEY_TASK_LABEL_FONT);
            taskLabelPos = getConfiguredPos(config, CONFIG_KEY_TASK_LABEL_POS);
            if (taskLabelPos == null) {
                taskLabelPos = new Point(progressBarArea.x, progressBarArea.y + progressBarArea.height + 10);
            }
        }

        versionLabelEnabled = getConfiguredOptionEnabled(config, CONFIG_KEY_VERSION_ENABLED);
        if (versionLabelEnabled) {
            versionText = config.getContextProperty(CONFIG_KEY_SPLASH_VERSION_TEXT);
            if (versionText == null) {
                versionText = config.getContextProperty("version");
            }
            versionLabelFont = getConfiguredFont(config, CONFIG_KEY_VERSION_FONT);
            versionLabelColor = getConfiguredColor(config, CONFIG_KEY_VERSION_COLOR, taskLabelColor);
            versionLabelPos = getConfiguredPos(config, CONFIG_KEY_VERSION_POS);
            if (versionLabelPos == null) {
                versionLabelPos = new Point(progressBarArea.x, progressBarArea.y + progressBarArea.height + 30);
            }
        }

        graphics = this.splashScreen.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        message = "";
    }

    public Splash getSplash() {
        return splashScreen;
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.totalWork = totalWork;
        this.currentWork = 0;
        this.lastPixelsWorked = 0;
        setTaskName(taskName);
    }

    @Override
    public void done() {
        internalWorked(totalWork);
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        if (splashScreen.isVisible()) {
            splashScreen.close();
        }
    }

    @Override
    public void worked(int work) {
        internalWorked(work);
    }

    @Override
    public void internalWorked(double work) {
        currentWork += work;
        double progress = currentWork / totalWork;
        pixelsWorked = (int) ((double) progressBarArea.width * progress);
        if (pixelsWorked > lastPixelsWorked) {
            lastPixelsWorked = pixelsWorked;
            update();
        }
    }

    @Override
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        this.message = createMessage();
        update();
    }

    @Override
    public void setSubTaskName(String subTaskName) {
        this.subTaskName = subTaskName;
        this.message = createMessage();
        update();
    }

    private String createMessage() {
        boolean validTaskName = taskName != null && taskName.length() > 0;
        boolean validSubTaskName = subTaskName != null && subTaskName.length() > 0;
        String message;
        if (validTaskName && validSubTaskName) {
            message = taskName + " - " + subTaskName;
        } else if (validTaskName) {
            message = taskName;
        } else if (validSubTaskName) {
            message = subTaskName;
        } else {
            message = "";
        }
        return message;
    }

    private void update() {
        if (graphics == null || !splashScreen.isVisible()) {
            return;
        }

        graphics.setComposite(AlphaComposite.Clear);
        graphics.fill(splashArea);

        graphics.setPaintMode();
        if (taskLabelEnabled) {
            paintTaskLabel();
        }
        if (versionLabelEnabled) {
            paintVersionLabel();
        }
        paintBar();

        splashScreen.update();
    }

    private void paintBar() {

//        graphics.setColor(Color.DARK_GRAY);
//        graphics.fillRect(progressBarArea.x,
//                          progressBarArea.y,
//                          progressBarArea.width,
//                          progressBarArea.height);

        graphics.setColor(progressBarColor);
        graphics.fillRect(progressBarArea.x,
                          progressBarArea.y,
                          pixelsWorked,
                          progressBarArea.height);

//        graphics.setColor(Color.BLACK);
//        graphics.drawLine(progressBarArea.x,
//                          progressBarArea.y,
//                          progressBarArea.x + progressBarArea.width,
//                          progressBarArea.y);
//        graphics.drawLine(progressBarArea.x,
//                          progressBarArea.y + progressBarHeight - 1,
//                          progressBarArea.x + progressBarArea.width,
//                          progressBarArea.y + progressBarHeight - 1);
    }

    private void paintTaskLabel() {
        graphics.setColor(taskLabelColor);
        graphics.setFont(taskLabelFont);
        graphics.drawString(message, taskLabelPos.x, taskLabelPos.y);
    }

    private void paintVersionLabel() {
        graphics.setColor(versionLabelColor);
        graphics.setFont(versionLabelFont);
        graphics.drawString(versionText, versionLabelPos.x, versionLabelPos.y);
    }

    private static BufferedImage loadImage(String imageFilePath) {
        try {
            return ImageIO.read(new File(imageFilePath));
        } catch (IOException e) {
            return null;
        }
    }

    private boolean getConfiguredOptionEnabled(RuntimeConfig config, String key) {
        return Boolean.parseBoolean(config.getContextProperty(key, "false"));
    }

    private static Color getConfiguredColor(RuntimeConfig config, String key, Color defaultValue) {
        String colorStr = config.getContextProperty(key);
        if (colorStr != null && !colorStr.isEmpty()) {
            if (colorStr.startsWith("#")) {
                return Color.decode(colorStr);
            }
            StringTokenizer st = new StringTokenizer(colorStr, ",");
            int n = st.countTokens();
            if (n == 3 || n == 4) {
                try {
                    int r = Integer.parseInt(st.nextToken());
                    int g = Integer.parseInt(st.nextToken());
                    int b = Integer.parseInt(st.nextToken());
                    int a = 255;
                    if (st.hasMoreTokens()) {
                        a = Integer.parseInt(st.nextToken());
                    }
                    return new Color(r, g, b, a);
                } catch (Exception e) {
                    // defaultValue is returned
                }
            }
        }
        return defaultValue;
    }

    private static Rectangle getConfiguredSplashProgressBarArea(RuntimeConfig config) {
        String areaStr = config.getContextProperty(CONFIG_KEY_SPLASH_PROGRESS_BAR_AREA);
        if (areaStr != null && !areaStr.isEmpty()) {
            StringTokenizer st = new StringTokenizer(areaStr, ",");
            int n = st.countTokens();
            if (n == 4) {
                try {
                    int x = Integer.parseInt(st.nextToken());
                    int y = Integer.parseInt(st.nextToken());
                    int w = Integer.parseInt(st.nextToken());
                    int h = Integer.parseInt(st.nextToken());
                    return new Rectangle(x, y, w, h);
                } catch (Exception e) {
                    // null is returned
                }
            }
        }
        return null;
    }

    private static Point getConfiguredPos(RuntimeConfig config, String key) {
        String posStr = config.getContextProperty(key);
        if (posStr != null && !posStr.isEmpty()) {
            StringTokenizer st = new StringTokenizer(posStr, ",");
            int n = st.countTokens();
            if (n == 2) {
                try {
                    int x = Integer.parseInt(st.nextToken());
                    int y = Integer.parseInt(st.nextToken());
                    return new Point(x, y);
                } catch (Exception e) {
                    // null is returned
                }
            }
        }
        return null;
    }

    private static Font getConfiguredFont(RuntimeConfig config, String key) {
        String fontDesc = config.getContextProperty(key);
        if (fontDesc == null || fontDesc.isEmpty()) {
            return new Font("Verdana", Font.ITALIC, 10);
        }
        return Font.decode(fontDesc.replace(",", "-"));
    }

    public static interface Splash {
        boolean isVisible();

        void close();

        void update();

        Graphics2D createGraphics();

        Dimension getSize();

        Rectangle getBounds();
    }

    private static class AwtSplash implements Splash {
        private final SplashScreen splashScreen;

        public AwtSplash(SplashScreen splashScreen) {
            this.splashScreen = splashScreen;
        }

        public void close() {
            splashScreen.close();
        }

        public boolean isVisible() {
            return splashScreen.isVisible();
        }

        public void update() {
            splashScreen.update();
        }


        public Graphics2D createGraphics() {
            return splashScreen.createGraphics();
        }

        public Dimension getSize() {
            return splashScreen.getSize();
        }

        public Rectangle getBounds() {
            return splashScreen.getBounds();
        }
    }

    private static class CeresSplash extends Window implements Splash {
        private BufferedImage bufferImage;
        private BufferedImage backgroundImage;
        private BufferedImage updateImage;
        private Graphics2D bufferGraphics;

        public CeresSplash(BufferedImage image) {
            super(null);
            backgroundImage = image;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle bounds = new Rectangle((screenSize.width - image.getWidth()) / 2,
                                             (screenSize.height - image.getHeight()) / 2,
                                             image.getWidth(),
                                             image.getHeight());
            bufferImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
            bufferGraphics = bufferImage.createGraphics();
            setBounds(bounds);
            setFocusable(false);
            setAlwaysOnTop(false);
            setVisible(true);
        }

        public void close() {
            if (bufferGraphics != null) {
                dispose();
                bufferGraphics.dispose();
                bufferGraphics = null;
                bufferImage = null;
                backgroundImage = null;
                updateImage = null;
            }
        }

        public void update() {
            repaint();
        }

        public Graphics2D createGraphics() throws IllegalStateException {
            if (updateImage == null) {
                Dimension dim = getSize();
                updateImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            }
            return updateImage.createGraphics();
        }

        @Override
        public void update(Graphics g) {
            bufferGraphics.drawImage(backgroundImage, null, 0, 0);
            if (updateImage != null) {
                bufferGraphics.drawImage(updateImage, null, 0, 0);
            }
            g.drawImage(bufferImage, 0, 0, null);
        }
    }
}
