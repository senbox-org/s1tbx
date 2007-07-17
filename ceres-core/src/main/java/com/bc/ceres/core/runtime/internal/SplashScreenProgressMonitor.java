package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;

import javax.imageio.ImageIO;
import java.awt.*;
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

    private Splash splashScreen;
    private String taskName;
    private String subTaskName;
    private double totalWork;
    private double currentWork;
    private int lastPixelsWorked;
    private Graphics2D graphics;
    private Rectangle splashArea;
    private int progressBarHeight;
    private Font font;
    private Color fontColor;
    private Color progressBarColor;
    private Rectangle progressBarArea;
    private int pixelsWorked;
    private String message;

    /**
     * Creates a {@link SplashScreenProgressMonitor} only if a splash screen exists.
     *
     * @return an instance of {@link SplashScreenProgressMonitor} or {@link NullProgressMonitor}
     */
    public static ProgressMonitor createProgressMonitor() {
        try {
            SplashScreen splashScreen = SplashScreen.getSplashScreen();
            if (splashScreen != null) {
                return new SplashScreenProgressMonitor(new AwtSplash(splashScreen));
            }
        } catch (Throwable t) {
            // ok, AWT splashscreen are not supported
        }
        String imageFilePath = getConfigurationValue(CONFIG_KEY_SPLASH_IMAGE);
        if (imageFilePath != null) {
            BufferedImage bufferedImage = loadImage(imageFilePath);
            if (bufferedImage != null) {
                return new SplashScreenProgressMonitor(new CeresSplash(bufferedImage));
            }
        }


        return ProgressMonitor.NULL;
    }

    public SplashScreenProgressMonitor(Splash splashScreen) {
        Assert.notNull(splashScreen, "splashScreen");
        this.splashScreen = splashScreen;

        font = new Font("Verdana", Font.ITALIC, 10);
        fontColor = Color.WHITE;

        graphics = this.splashScreen.createGraphics();
        graphics.setFont(font);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Dimension size = this.splashScreen.getSize();
        splashArea = new Rectangle(size);
        message = "";

        progressBarColor = getConfiguredSplashProgressBarColor();
        if (progressBarColor == null) {
            progressBarColor = Color.GREEN;
        }

        progressBarArea = getConfiguredSplashProgressBarArea();
        if (progressBarArea != null) {
            progressBarHeight = progressBarArea.height;
        } else {
            progressBarHeight = 5;
            progressBarArea = new Rectangle(0,
                                            splashArea.height - progressBarHeight - 4,
                                            splashArea.width,
                                            progressBarHeight);
        }
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
            message = taskName + ": " + subTaskName;
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
        // paintMessage();
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

    private void paintMessage() {
        graphics.setColor(fontColor);
        graphics.setFont(font);
        graphics.drawString(message, 4, progressBarArea.y - 4);
    }

    private static BufferedImage loadImage(String imageFilePath) {
        try {
            return ImageIO.read(new File(imageFilePath));
        } catch (IOException e) {
            return null;
        }
    }

    private static Color getConfiguredSplashProgressBarColor() {
        String areaStr = getConfigurationValue(CONFIG_KEY_SPLASH_PROGRESS_BAR_COLOR);
        StringTokenizer st = new StringTokenizer(areaStr, ",");
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
            }
        }
        return null;
    }

    private static Rectangle getConfiguredSplashProgressBarArea() {
        String areaStr = getConfigurationValue(CONFIG_KEY_SPLASH_PROGRESS_BAR_AREA);
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
            }
        }
        return null;
    }

    private static String getConfigurationValue(String key) {
        String contextId = System.getProperty(DefaultRuntimeConfig.CONFIG_KEY_CERES_CONTEXT,
                                              DefaultRuntimeConfig.DEFAULT_CERES_CONTEXT);
        return System.getProperty(contextId + '.' + key);
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
        private BufferedImage image0;
        private BufferedImage image;

        public CeresSplash(BufferedImage image) {
            super(null);
            image0 = image;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle bounds = new Rectangle((screenSize.width - image.getWidth()) / 2,
                                        (screenSize.height - image.getHeight()) / 2,
                                        image.getWidth(),
                                        image.getHeight());
            if (image.getRaster().getNumBands() == 4) {
                try {
                    Robot robot = new Robot(getGraphicsConfiguration().getDevice());
                    BufferedImage screen = robot.createScreenCapture(bounds);
                    Graphics2D graphics = screen.createGraphics();
                    graphics.drawImage(image, null, 0, 0);
                    graphics.dispose();
                    this.image0 = screen;
                } catch (AWTException e) {
                    // ok, no transparency
                }
            }
            setBounds(bounds);
            setVisible(true);
        }

        public void close() {
            dispose();
            image0 = null;
            image = null;
        }

        public void update() {
            repaint();
        }

        public Graphics2D createGraphics() throws IllegalStateException {
            if (image == null) {
                Dimension dim = getSize();
                image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            }
            return image.createGraphics();
        }

        @Override
        public void update(Graphics g) {
            g.drawImage(image0, 0, 0, null);
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}
