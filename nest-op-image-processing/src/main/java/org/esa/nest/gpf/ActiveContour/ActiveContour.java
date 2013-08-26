/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.ActiveContour;

import ij.IJ;
import ij.Prefs;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.esa.beam.framework.datamodel.PixelPos;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Active Contour finds a contour that best approximates the perimeter of an
 * object
 *
 * @author thomas.boudier@snv.jussieu.fr
 * @created August 26th 2003
 * @author Emanuela Boros
 * @updated September 2012
 *
 */
public class ActiveContour {

    private PixelPos points[];
    private PixelPos normal[];
    private PixelPos displacements[];
    private double dataDistance;
    private double lambda[];
    private int state[];
    private int NR_POINTS;
    private int NMAX = 50000;
    private int OFF;
    private int block;
    private int elimination;
    private boolean isClosed;
    private ActiveContourConfiguration activeContourConfiguration;
    private ImageProcessor gradientImage;
    private ImageProcessor originalImage;

    public ActiveContour() {
    }

    /**
     * Description of the Method
     */
    public void stop() {
        points = null;
        normal = null;
        displacements = null;
        lambda = null;
        state = null;
        System.gc();
    }

    /**
     * Sets the Configuration attribute of the ActiveContour object
     *
     * @param activeContourConfiguration The new Configuration value
     */
    public void setConfiguration(ActiveContourConfiguration activeContourConfiguration) {
        this.activeContourConfiguration = activeContourConfiguration;
    }

    /**
     * Get number of points
     *
     * @return The NR_POINTS value
     */
    public int getNumberOfPoints() {
        return NR_POINTS;
    }

    /**
     * Gets the point attribute of the ActiveContour object
     *
     * @param i Description of the Parameter
     * @return The point value
     */
    public PixelPos getPoint(int i) {
        return points[i];
    }

    /**
     * Gets the points attribute of the ActiveContour object
     *
     * @return The points value
     */
    public PixelPos[] getPoints() {
        return points;
    }

    /**
     * Gets the configuration attribute of the ActiveContour object
     *
     * @return The configuration value
     */
    public ActiveContourConfiguration getConfiguration() {
        return activeContourConfiguration;
    }

    /**
     * Gets the lambda attribute of the ActiveContour object
     *
     * @return The lambda value
     */
    public double[] getLambda() {
        return lambda;
    }

    /**
     * Gets the displacement attribute of the ActiveContour object
     *
     * @return The displacement value
     */
    public PixelPos[] getDisplacements() {
        return displacements;
    }

    /**
     * Is the contour is closed
     *
     * @return the value of boolean isClosed
     */
    public boolean isClosed() {
        return isClosed;
    }

    public void setOriginalImage(ImageProcessor originalImage) {
        this.originalImage = originalImage;
    }

    /**
     * Draw the active contour
     *
     * @param imageProcessor Description of the Parameter
     * @param color Description of the Parameter
     * @param lineWidth Description of the Parameter
     */
    public ImageProcessor drawContour(ImageProcessor imageProcessor,
            Color color, int lineWidth) {

        int x, y;
        int xx, yy;

        imageProcessor.setColor(color);
        imageProcessor.setLineWidth(lineWidth);

        for (int i = 0; i < NR_POINTS - 1; i++) {
            x = (int) (points[i].x);
            y = (int) (points[i].y);
            xx = (int) (points[i + 1].x);
            yy = (int) (points[i + 1].y);
            imageProcessor.drawLine(x, y, xx, yy);
        }
        if (isClosed()) {
            x = (int) (points[NR_POINTS - 1].x);
            y = (int) (points[NR_POINTS - 1].y);
            xx = (int) (points[0].x);
            yy = (int) (points[0].y);
            imageProcessor.drawLine(x, y, xx, yy);
        }
        return imageProcessor;
    }

    /**
     * write output in a text format
     *
     * @param nom file name
     * @param nb slice number
     */
    public void writeCoordinates(String nom, int nb, double resXY) {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(3);
        try {
            File fichier = new File(nom + nb + ".txt");
            FileWriter fw = new FileWriter(fichier);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("nb\tX\tY\tZ\tXcal\tYcal\n");
            for (int i = 0; i < NR_POINTS; i++) {
                bw.write(i + "\t" + nf.format(points[i].x) + "\t"
                        + nf.format(points[i].y) + "\t" + nb + "\t" + nf.format(points[i].x * resXY) + "\t" + nf.format(points[i].y) + "\n");
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
        }
    }

    /**
     * Creation of a polygon ROI
     *
     * @return ROI
     */
    PolygonRoi createROI() {
        int x[] = new int[NR_POINTS];
        int y[] = new int[NR_POINTS];
        for (int i = 0; i < NR_POINTS; i++) {
            x[i] = (int) (points[i].x);
            y[i] = (int) (points[i].y);
        }
        return new PolygonRoi(x, y, NR_POINTS - 1, Roi.FREEROI);
    }

    /**
     * Initialization of the active contour points
     *
     * @param roi ROI
     */
    public void initActiveContour(Roi roi) {

        double dRx, dRy;
        int i = 1;
        double a;
        NR_POINTS = 0;

        points = new PixelPos[NMAX];
        normal = new PixelPos[NMAX];
        displacements = new PixelPos[NMAX];
        dataDistance = 0.0;
        state = new int[NMAX];
        lambda = new double[NMAX];

        for (i = 0; i < NMAX; i++) {
            points[i] = new PixelPos();
            normal[i] = new PixelPos();
            displacements[i] = new PixelPos();
        }


        //computes the points of the ROI
        if ((roi.getType() == Roi.OVAL) || (roi.getType() == Roi.RECTANGLE)) {
            isClosed = true;
            Rectangle Rect = roi.getBounds();
            int xc = Rect.x + Rect.width / 2;
            int yc = Rect.y + Rect.height / 2;
            dRx = ((double) Rect.width) / 2;
            dRy = ((double) Rect.height) / 2;
            double theta = 4.0 / (dRx + dRy);
            i = 0;
            for (a = 2 * Math.PI; a > 0; a -= theta) {
                points[i].x = (int) (xc + dRx * Math.cos(a));
                points[i].y = (int) (yc + dRy * Math.sin(a));
                state[i] = 0;
                i++;
            }
            NR_POINTS = i;
        } else if (roi.getType() == Roi.LINE) {
            isClosed = false;
            Line l = (Line) (roi);
            dRx = (l.x2 - l.x1);
            dRy = (l.y2 - l.y1);
            a = Math.sqrt(dRx * dRx + dRy * dRy);
            dRx /= a;
            dRy /= a;
            int ind = 0;
            for (i = 0; i <= l.getLength(); i++) {
                points[ind].x = (float) (l.x1 + dRx * i);
                points[ind].y = (float) (l.y1 + dRy * i);
                state[ind] = 0;
                ind++;
            }
            NR_POINTS = ind;
        } else if ((roi.getType() == Roi.FREEROI) || (roi.getType() == Roi.POLYGON)) {
            isClosed = true;
            PolygonRoi p = (PolygonRoi) (roi);
            Rectangle rectBound = p.getBounds();
            int NBPT = p.getNCoordinates();
            int pointsX[] = p.getXCoordinates();
            int pointsY[] = p.getYCoordinates();
            for (i = 0; i < NBPT; i++) {
                points[i].x = pointsX[i] + rectBound.x;
                points[i].y = pointsY[i] + rectBound.y;

            }
            NR_POINTS = NBPT;
            if (roi.getType() == Roi.POLYGON) {
                this.resample(true);
            }
        } else {
            IJ.showStatus("Selection type not supported");
        }
        block = 0;
        elimination = 0;
        OFF = 0;
    }

    /**
     * Compute regularization of distance between points
     *
     * @param init
     */
    void resample(boolean init) {

        PixelPos temp[] = new PixelPos[NMAX];
        PixelPos tanPixel = new PixelPos();

        int i, j, k;
        int ii, aj;
        double dAvg, dAvgg, dI;
        double DD, D, D1, normTan;
        double dMin = 1000.0;
        double dMax = 0.0;
        double dTot = 0.0;

        for (i = 1; i < NR_POINTS; i++) {
            dI = getDistance(i, i - 1);
            dTot += dI;
            if (dI < dMin) {
                dMin = dI;
            }
            if (dI > dMax) {
                dMax = dI;
            }
        }
        if (((dMax / dMin) > 3.0) || init) {
            dAvgg = 1.0;
            temp[0] = new PixelPos();
            temp[0].x = points[0].x;
            temp[0].y = points[0].y;
            i = 1;
            ii = 1;
            temp[ii] = new PixelPos();
            while (i < NR_POINTS) {
                dAvg = dAvgg;
                DD = getDistance(i, i - 1);
                if (DD > dAvg) {
                    aj = (int) (DD / dAvg);
                    tanPixel.x = points[i].x - points[i - 1].x;
                    tanPixel.y = points[i].y - points[i - 1].y;
                    normTan = Math.sqrt(tanPixel.x * tanPixel.x + tanPixel.y * tanPixel.y);
                    tanPixel.x /= normTan;
                    tanPixel.y /= normTan;
                    for (k = 1; k <= aj; k++) {
                        temp[ii].x = points[i - 1].x + (float) (k * dAvg * tanPixel.x);
                        temp[ii].y = points[i - 1].y + (float) (k * dAvg * tanPixel.y);
                        ii++;
                        temp[ii] = new PixelPos();
                    }
                }
                i++;
                if ((DD <= dAvg) && (i < NR_POINTS - 1)) {
                    j = i - 1;
                    D = 0.0;
                    while ((D < dAvg) && (j < NR_POINTS - 1)) {
                        D += getDistance(j, j + 1);
                        j++;
                    }
                    temp[ii].x = points[j].x;
                    temp[ii].y = points[j].y;
                    ii++;
                    temp[ii] = new PixelPos();
                    i = j + 1;
                }
                if (i == NR_POINTS - 1) {
                    i = NR_POINTS;
                }
            }
            temp[ii].x = points[NR_POINTS - 1].x;
            temp[ii].y = points[NR_POINTS - 1].y;
            NR_POINTS = ii + 1;
            for (i = 0; i < NR_POINTS; i++) {
                points[i].x = (float) (temp[i].x);
                points[i].y = (float) (temp[i].y);
            }
        }
    }

    /**
     * Main calculus function (matrix inversion)
     *
     * @param iFirstRow first row
     * @param iLastRow last row
     */
    public void calculus(int iFirstRow, int iLastRow) {
        int i;
        PixelPos bi;
        PixelPos temp;
        PixelPos debtemp;
        double mi;
        double gi;
        double di;
        double omega;

        omega = 1.8;
        bi = new PixelPos();
        temp = new PixelPos();
        debtemp = new PixelPos();

        debtemp.x = points[iFirstRow].x;
        debtemp.y = points[iFirstRow].y;

        for (i = iFirstRow; i < iLastRow; i++) {
            bi.x = points[i].x + displacements[i].x;
            bi.y = points[i].y + displacements[i].y;
            gi = -lambda[i];
            di = -lambda[i + 1];
            mi = lambda[i] + lambda[i + 1] + 1.0;
            if (i > iFirstRow) {
                temp.x = (float) (mi * points[i].x + omega * (-gi * points[i - 1].x - mi * points[i].x - di * points[i + 1].x + bi.x));
                temp.y = (float) (mi * points[i].y + omega * (-gi * points[i - 1].y - mi * points[i].y - di * points[i + 1].y + bi.y));
            }
            if ((i == iFirstRow) && (isClosed)) {
                temp.x = (float) (mi * points[i].x + omega * (-gi * points[iLastRow].x - mi * points[i].x - di * points[i + 1].x + bi.x));
                temp.y = (float) (mi * points[i].y + omega * (-gi * points[iLastRow].y - mi * points[i].y - di * points[i + 1].y + bi.y));
            }
            if ((i == iFirstRow) && (!isClosed)) {
                temp.x = points[iFirstRow].x * (float) (mi);
                temp.y = points[iFirstRow].y * (float) (mi);
            }
            points[i].x = (float) (temp.x / mi);
            points[i].y = (float) (temp.y / mi);
        }
        // last point
        if (isClosed) {
            i = iLastRow;
            bi.x = points[i].x + displacements[i].x;
            bi.y = points[i].y + displacements[i].y;
            gi = -lambda[i];
            di = -lambda[iFirstRow];
            mi = lambda[i] + lambda[iFirstRow] + 1.0;
            temp.x = (float) (mi * points[i].x + omega * (-gi * points[i - 1].x - mi * points[i].x - di * debtemp.x + bi.x));
            temp.y = (float) (mi * points[i].y + omega * (-gi * points[i - 1].y - mi * points[i].y - di * debtemp.y + bi.y));
            points[i].x = (float) (temp.x / mi);
            points[i].y = (float) (temp.y / mi);
        }
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public double computeDisplacements() {

        double sum = 0d;
        double threshold = activeContourConfiguration.getGradThreshold();
        double DivForce = activeContourConfiguration.getMaxDisplacement();
        PixelPos displ = new PixelPos();
        double force;
        sum = 0;
        for (int i = 0; i < NR_POINTS; i++) {
            displ.x = 0f;
            displ.y = 0f;
            displ = searchTheClosestEdge(i, threshold, 1000, 1000, 0);

            force = Math.sqrt(displ.x * displ.x + displ.y * displ.y);
            if (force > DivForce) {
                displacements[i].x = (float) (DivForce * (displ.x / force));
                displacements[i].y = (float) (DivForce * (displ.y / force));
            } else {
                displacements[i].x = (float) (displ.x);
                displacements[i].y = (float) (displ.y);
            }
            force = Math.sqrt(displacements[i].x * displacements[i].x + displacements[i].y * displacements[i].y);

            sum += force;
        }
        return sum;
    }

    /**
     * Description of the Method
     *
     * @param image Description of the Parameter
     */
    public void computeGradient(ImageProcessor image) {
        gradientImage = computeDeriche(image, activeContourConfiguration.getAlpha());
    }

    /**
     * search for the closest edge along the normal direction
     *
     * @param iContourPoints number for the snake point
     * @param dEdgeThreshold threshold
     * @param directions directions to look for
     * @return the displacement vector towards the edges
     */
    PixelPos searchTheClosestEdge(int iContourPoints, double dEdgeThreshold,
            double dDistancePlus, double dDistanceMinus, int dir) {
        double dY, dX;
        double dMore;
        double dLess;
        double dMaxSearch = activeContourConfiguration.getMaxSearch();
        double dDistance;
        double crp = Double.NaN;
        double crm = Double.NaN;
        double bres;
        double ares;
        double bden;
        double bnum;
        PixelPos displacement;
        PixelPos pixel;
        PixelPos normalPixel;
        int scale = 10;
        double imageLine[] = new double[(int) (2 * scale * dMaxSearch + 1)];

        pixel = points[iContourPoints];
        normalPixel = normal[iContourPoints];

        displacement = new PixelPos();
        //search the points on the normal line of the contour
        int index = 0;
        double step = 1.0 / (double) scale;
        double deb = -dMaxSearch;
        for (double ii = deb; ii < dMaxSearch; ii += step) {
            dY = (pixel.y + normalPixel.y * ii);
            dX = (pixel.x + normalPixel.x * ii);
            if (dX < 0) {
                dX = 0;
            }
            if (dY < 0) {
                dY = 0;
            }
            if (dX >= gradientImage.getWidth()) {
                dX = gradientImage.getWidth() - 1;
            }
            if (dY >= gradientImage.getHeight()) {
                dY = gradientImage.getHeight() - 1;
            }
            imageLine[index] = gradientImage.getInterpolatedPixel(dX, dY);
            index++;
        }

        // polygon crossing, avoid self-intersecting contour
        for (int i = 0; i < NR_POINTS - 1; i++) {
            if ((i != iContourPoints) && (i != iContourPoints - 1)) {
                bden = (-normalPixel.x * points[i + 1].y + normalPixel.x * points[i].y
                        + normalPixel.y * points[i + 1].x - normalPixel.y * points[i].x);
                bnum = (-normalPixel.x * pixel.y + normalPixel.x * points[i].y
                        + normalPixel.y * pixel.x - normalPixel.y * points[i].x);
                if (bden != 0) {
                    bres = (bnum / bden);
                } else {
                    bres = 5.0;
                }
                if ((bres >= 0.0) && (bres <= 1.0)) {
                    ares = (float) (-(-points[i + 1].y * pixel.x + points[i + 1].y
                            * points[i].x + points[i].y * pixel.x + pixel.y * points[i + 1].x - pixel.y * points[i].x - points[i].y * points[i + 1].x) / (-normalPixel.x * points[i + 1].y + normalPixel.x * points[i].y + normalPixel.y * points[i + 1].x - normalPixel.y * points[i].x));
                    if ((ares > 0.0) && (ares < crp)) {
                        crp = ares;
                    }
                    if ((ares < 0.0) && (ares > crm)) {
                        crm = ares;
                    }
                }
            }
        }
        double coeff_crossing = 0.9d;
        crp = crp * coeff_crossing;
        crm = crm * coeff_crossing;

        dMore = Double.POSITIVE_INFINITY;
        dLess = Double.NEGATIVE_INFINITY;

        boolean edge_found = false;
        for (index = 1; index < 2 * scale * dMaxSearch - 1; index++) {
            // check edge threshold
            // local maximum
            if ((imageLine[index] >= dEdgeThreshold) && (imageLine[index] >= imageLine[index - 1]) && (imageLine[index] >= imageLine[index + 1])) {
                dDistance = index * step + deb;
                if ((dDistance < 0) && (dDistance > dLess)) {
                    dLess = dDistance;
                    edge_found = true;
                }
                if ((dDistance >= 0) && (dDistance < dMore)) {
                    dMore = dDistance;
                    edge_found = true;
                }
            }
        }
        state[iContourPoints] = 0;

        // no edge found
        if (!edge_found) {
            displacement.x = 0f;
            displacement.y = 0f;

            return displacement;
        }

        // check edges found against threshold distances plus and minus
        if (dMore > dDistancePlus) {
            dMore = 2 * dMaxSearch;
        }
        if (dLess < -dDistanceMinus) {
            dLess = -2 * dMaxSearch;
        }
        if (Double.isInfinite(dMore) && Double.isInfinite(dLess)) {
            displacement.x = 0f;
            displacement.y = 0f;

            return displacement;
        }

        int direction = 0;
        // go to closest edge
        if (-dLess < dMore) {
            displacement.x = (float) (normalPixel.x * dLess);
            displacement.y = (float) (normalPixel.y * dLess);
            direction = -1;
        } else {
            displacement.x = (float) (normalPixel.x * dMore);
            displacement.y = (float) (normalPixel.y * dMore);
            direction = 1;
        }
        return displacement;
    }

    /**
     * Description of the Method
     */
    public void compute_normales() {
        for (int i = 0; i < NR_POINTS; i++) {
            normal[i] = computeNormal(i);
        }
    }

    /**
     * The lambda computation
     */
    public void computeLambdas() {
        double force;
        double maxforce = 0.0;
        double minR = activeContourConfiguration.getRegMin();
        double maxR = activeContourConfiguration.getRegMax();

        for (int i = 0; i < NR_POINTS; i++) {
            force = Math.sqrt(displacements[i].x * displacements[i].x
                    + displacements[i].y * displacements[i].y);
            if (force > maxforce) {
                maxforce = force;
            }
        }

        for (int i = 0; i < NR_POINTS; i++) {
            force = Math.sqrt(displacements[i].x * displacements[i].x + displacements[i].y * displacements[i].y);
            lambda[i] = maxR / (1.0 + ((maxR - minR) / minR) * (force / maxforce));
        }
    }

    /**
     * compute normale
     *
     * @param np number for the snake point
     * @return normal vector
     */
    PixelPos computeNormal(int np) {
        PixelPos norma;
        PixelPos tan;
        double normtan;

        tan = new PixelPos();
        norma = new PixelPos();

        if (np == 0) {
            if (isClosed) {
                tan.x = points[1].x - points[NR_POINTS - 1].x;
                tan.y = points[1].y - points[NR_POINTS - 1].y;
            } else {
                tan.x = points[1].x - points[0].x;
                tan.y = points[1].y - points[0].y;
            }
        }
        if (np == NR_POINTS - 1) {
            if (isClosed) {
                tan.x = points[0].x - points[NR_POINTS - 2].x;
                tan.y = points[0].y - points[NR_POINTS - 2].y;
            } else {
                tan.x = points[NR_POINTS - 1].x - points[NR_POINTS - 2].x;
                tan.y = points[NR_POINTS - 1].y - points[NR_POINTS - 2].y;
            }
        }
        if ((np > 0) && (np < NR_POINTS - 1)) {
            tan.x = points[np + 1].x - points[np - 1].x;
            tan.y = points[np + 1].y - points[np - 1].y;
        }
        normtan = Math.sqrt(tan.x * tan.x + tan.y * tan.y);
        if (normtan > 0.0) {
            tan.x /= normtan;
            tan.y /= normtan;
            norma.x = -tan.y;
            norma.y = tan.x;
        }
        return (norma);
    }

    /**
     * destruction
     */
    void destroysnake() {
        PixelPos temp[];
        PixelPos fo[];
        double lan[];
        int state[];
        int i;
        int j;

        temp = new PixelPos[NR_POINTS];
        fo = new PixelPos[NR_POINTS];
        lan = new double[NR_POINTS];
        state = new int[NR_POINTS];

        j = 0;
        for (i = 0; i < NR_POINTS; i++) {
            if (state[i] != 1) {
                temp[j] = new PixelPos();
                temp[j].x = points[i].x;
                temp[j].y = points[i].y;
                state[j] = state[i];
                fo[j] = new PixelPos();
                fo[j].x = displacements[i].x;
                fo[j].y = displacements[i].y;
                lan[j] = lambda[i];
                j++;
            }
        }
        NR_POINTS = j;

        for (i = 0; i < NR_POINTS; i++) {
            points[i].x = temp[i].x;
            points[i].y = temp[i].y;
//            state[i] = state[i];
            displacements[i].x = fo[i].x;
            displacements[i].y = fo[i].y;
            lambda[i] = lan[i];
        }
    }

    /**
     * distance between two points of the snake
     *
     * @param a number of first point
     * @param b number of second point
     * @return distance
     */
    double getDistance(int a, int b) {
        return (Math.sqrt(Math.pow(points[a].x - points[b].x, 2.0)
                + Math.pow(points[a].y - points[b].y, 2.0)));
    }

    /**
     * compute new positions of the snake
     */
    void computeNewPositions() {
        calculus(0, NR_POINTS - 1);
    }

    /**
     * Deriche filtering
     *
     * @param iDep image
     * @param alphaD Description of the Parameter
     * @return Description of the Return Value
     */
    private ImageProcessor computeDeriche(ImageProcessor iDep, double alphaD) {

        ByteProcessor iGrad = (ByteProcessor) iDep.duplicate().convertToByte(true);

        int lines = iDep.getHeight();
        int columns = iDep.getWidth();
        int lenght = lines * columns;
        float[] nf_gry = new float[lenght];

        float[] a1 = new float[lenght];
        float[] a2 = new float[lenght];
        float[] a3 = new float[lenght];
        float[] a4 = new float[lenght];
        int iColumns;
        int line1, line2, line3;
        int col1, col2, col3;
        int icol_1, icol_2;
        int i, j;
        float ad1, ad2;
        float an1, an2, an3, an4, an11;

        line1 = lines - 1;
        line2 = lines - 2;
        line3 = lines - 3;
        col1 = columns - 1;
        col2 = columns - 2;
        col3 = columns - 3;

        ad1 = (float) -Math.exp(-alphaD);
        ad2 = 0;
        an1 = 1;
        an2 = 0;
        an3 = (float) Math.exp(-alphaD);
        an4 = 0;
        an11 = 1;

        /*
         * FIRST STEP: Y GRADIENT
         */
        /*
         * x-smoothing
         */
        for (i = 0; i < lines; i++) {
            for (j = 0; j < columns; j++) {
                a1[i * columns + j] = iDep.getPixelValue(j, i);
            }
        }

        for (i = 0; i < lines; ++i) {
            iColumns = i * columns;
            icol_1 = iColumns - 1;
            icol_2 = iColumns - 2;
            a2[iColumns] = an1 * a1[iColumns];
            a2[iColumns + 1] = an1 * a1[iColumns + 1]
                    + an2 * a1[iColumns] - ad1 * a2[iColumns];
            for (j = 2; j < columns; ++j) {
                a2[iColumns + j] = an1 * a1[iColumns + j] + an2 * a1[icol_1 + j]
                        - ad1 * a2[icol_1 + j] - ad2 * a2[icol_2 + j];
            }
        }

        for (i = 0; i < lines; ++i) {
            iColumns = i * columns;
            icol_1 = iColumns + 1;
            icol_2 = iColumns + 2;
            a3[iColumns + col1] = 0;
            a3[iColumns + col2] = an3 * a1[iColumns + col1];
            for (j = col3; j >= 0; --j) {
                a3[iColumns + j] = an3 * a1[icol_1 + j] + an4 * a1[icol_2 + j]
                        - ad1 * a3[icol_1 + j] - ad2 * a3[icol_2 + j];
            }
        }

        icol_1 = lines * columns;

        for (i = 0; i < icol_1; ++i) {
            a2[i] += a3[i];
        }

        /*
         * FIRST STEP Y-GRADIENT : y-derivative
         */
        /*
         * columns top - downn
         */
        for (j = 0; j < columns; ++j) {
            a3[j] = 0;
            a3[columns + j] = an11 * a2[j] - ad1 * a3[j];
            for (i = 2; i < lines; ++i) {
                a3[i * columns + j] = an11 * a2[(i - 1) * columns + j]
                        - ad1 * a3[(i - 1) * columns + j] - ad2 * a3[(i - 2) * columns + j];
            }
        }

        /*
         * columns down top
         */
        for (j = 0; j < columns; ++j) {
            a4[line1 * columns + j] = 0;
            a4[(line2 * columns) + j] = -an11 * a2[line1 * columns + j]
                    - ad1 * a4[line1 * columns + j];
            for (i = line3; i >= 0; --i) {
                a4[i * columns + j] = -an11 * a2[(i + 1) * columns + j]
                        - ad1 * a4[(i + 1) * columns + j] - ad2 * a4[(i + 2) * columns + j];
            }
        }

        icol_1 = columns * lines;
        for (i = 0; i < icol_1; ++i) {
            a3[i] += a4[i];
        }

        for (i = 0; i < lines; ++i) {
            for (j = 0; j < columns; ++j) {
                nf_gry[i * columns + j] = a3[i * columns + j];
            }
        }

        /*
         * SECOND STEP X-GRADIENT
         */
        for (i = 0; i < lines; ++i) {
            for (j = 0; j < columns; ++j) {
                a1[i * columns + j] = (int) (iDep.getPixel(j, i));
            }
        }

        for (i = 0; i < lines; ++i) {
            iColumns = i * columns;
            icol_1 = iColumns - 1;
            icol_2 = iColumns - 2;
            a2[iColumns] = 0;
            a2[iColumns + 1] = an11 * a1[iColumns];
            for (j = 2; j < columns; ++j) {
                a2[iColumns + j] = an11 * a1[icol_1 + j]
                        - ad1 * a2[icol_1 + j] - ad2 * a2[icol_2 + j];
            }
        }

        for (i = 0; i < lines; ++i) {
            iColumns = i * columns;
            icol_1 = iColumns + 1;
            icol_2 = iColumns + 2;
            a3[iColumns + col1] = 0;
            a3[iColumns + col2] = -an11 * a1[iColumns + col1];
            for (j = col3; j >= 0; --j) {
                a3[iColumns + j] = -an11 * a1[icol_1 + j]
                        - ad1 * a3[icol_1 + j] - ad2 * a3[icol_2 + j];
            }
        }
        icol_1 = lines * columns;
        for (i = 0; i < icol_1; ++i) {
            a2[i] += a3[i];
        }

        /*
         * on the columns
         */
        /*
         * columns top down
         */
        for (j = 0; j < columns; ++j) {
            a3[j] = an1 * a2[j];
            a3[columns + j] = an1 * a2[columns + j] + an2 * a2[j]
                    - ad1 * a3[j];
            for (i = 2; i < lines; ++i) {
                a3[i * columns + j] = an1 * a2[i * columns + j] + an2 * a2[(i - 1) * columns + j]
                        - ad1 * a3[(i - 1) * columns + j] - ad2 * a3[(i - 2) * columns + j];
            }
        }

        /*
         * columns down top
         */
        for (j = 0; j < columns; ++j) {
            a4[line1 * columns + j] = 0;
            a4[line2 * columns + j] = an3 * a2[line1 * columns + j] - ad1 * a4[line1 * columns + j];
            for (i = line3; i >= 0; --i) {
                a4[i * columns + j] = an3 * a2[(i + 1) * columns + j] + an4 * a2[(i + 2) * columns + j]
                        - ad1 * a4[(i + 1) * columns + j] - ad2 * a4[(i + 2) * columns + j];
            }
        }

        icol_1 = columns * lines;
        for (i = 0; i < icol_1; ++i) {
            a3[i] += a4[i];
        }

        float[] nf_grx = new float[lenght];

        for (i = 0; i < lines; i++) {
            for (j = 0; j < columns; j++) {
                nf_grx[i * columns + j] = a3[i * columns + j];
            }
        }

        /*
         * SECOND STEP X-GRADIENT : the x-gradient is done
         */
        /*
         * THIRD STEP : NORM
         */
        /*
         * computatopn of the magnitude
         */
        for (i = 0; i < lines; i++) {
            for (j = 0; j < columns; j++) {
                a2[i * columns + j] = nf_gry[i * columns + j];
            }
        }
        icol_1 = columns * lines;
        for (i = 0; i < icol_1; ++i) {
            a2[i] = (float) Math.sqrt((a2[i] * a2[i]) + (a3[i] * a3[i]));
        }
        /*
         * THIRD STEP : the norm is done
         */
        byte[] result_array = new byte[lenght];

        //Recherche des niveaux min et max du gradiant
        double min = a2[0];
        double max = a2[0];
        for (i = 1; i < lenght; i++) {
            if (min > a2[i]) {
                min = a2[i];
            }
            if (max < a2[i]) {
                max = a2[i];
            }
        }


        //Normalisation de gradient de 0 a 255
        for (i = 0; i < lenght; ++i) {
            result_array[i] = (byte) (255 * (a2[i] / (max - min)));
        }

        //sauvegarde de la norme du gradiant
        iGrad.setPixels(result_array);

        return iGrad;
    }

    /**
     * main function for the snake
     *
     * @return Description of the Return Value
     */
    public double process() {
        int i;
        double force;
        PixelPos displ = new PixelPos();
        double maxforce = 0.0;
        double som = 0.0;
        double seuil = activeContourConfiguration.getGradThreshold();
        double DivForce = activeContourConfiguration.getMaxDisplacement();
        double minr = activeContourConfiguration.getRegMin();
        double maxr = activeContourConfiguration.getRegMax();
        double alpha = activeContourConfiguration.getAlpha();

        double dist_plus = Prefs.get("ABSnake_ThreshDistPos.double", 100);
        double dist_minus = Prefs.get("ABSnake_ThreshDistNeg.double", 100);


        for (i = 0; i < NR_POINTS; i++) {
            normal[i] = computeNormal(i);
        }
        block = 0;
        elimination = 0;
        for (i = 0; i < NR_POINTS; i++) {
            displ.x = 0f;
            displ.y = 0f;
            displ = searchTheClosestEdge(i, seuil, dist_plus, dist_minus, -1);

            force = Math.sqrt(Math.pow(displ.x, 2.0) + Math.pow(displ.y, 2.0));
            if (force > DivForce) {
                displacements[i].x = (float) (DivForce * (displ.x / force));
                displacements[i].y = (float) (DivForce * (displ.y / force));
            } else {
                displacements[i].x = displ.x;
                displacements[i].y = displ.y;
            }
            force = Math.sqrt(displacements[i].x * displacements[i].x + displacements[i].y * displacements[i].y);
            if (force > maxforce) {
                maxforce = force;
            }
            som += force;
        }
        dataDistance = som / NR_POINTS;

        for (i = 0; i < NR_POINTS; i++) {
            force = Math.sqrt(Math.pow(displacements[i].x, 2.0) + Math.pow(displacements[i].y, 2.0));
            lambda[i] = maxr / (1.0 + ((maxr - minr) / minr) * (force / maxforce));
        }
        if (elimination == 1) {
            destroysnake();
        }

        computeNewPositions();
        resample(false);

        return dataDistance;
    }

    /**
     * SEGMENTATION : inside/outside the snake
     *
     * @param width Description of the Parameter
     * @param height Description of the Parameter
     * @return binarised image (black=object inside snake)
     */
    public ByteProcessor segment(int width, int height, int column) {

        PixelPos pos = new PixelPos();
        PixelPos norm = new PixelPos();
        PixelPos ref = new PixelPos();
        int top, left, right, bottom;
        int i, j;
        int x, y;
        int count;
        double bden, bnum, bres;
        double lnorm;
        double ares;

        ByteProcessor res = new ByteProcessor(width, height);
        //res.invert();

        //Calcul des valeur permettant de definir le rectangle englobant du snake
        top = 0;
        bottom = 100000;
        left = 100000;
        right = 0;
        for (i = 0; i < NR_POINTS; i++) {
            if (points[i].y > top) {
                top = (int) points[i].y;
            }
            if (points[i].y < bottom) {
                bottom = (int) points[i].y;
            }
            if (points[i].x > right) {
                right = (int) points[i].x;
            }
            if (points[i].x < left) {
                left = (int) points[i].x;
            }
        }

        //On dessine l'interieur du snake a 255
        ref.x = 0;
        ref.y = 0;
        for (x = left; x < right; x++) {
            for (y = bottom; y < top; y++) {
                pos.x = x;
                pos.y = y;
                //norm.x = ref.x - pos.x;
                //norm.y = ref.y - pos.y;
                //lnorm = Math.sqrt(norm.x * norm.x + norm.y * norm.y);
                //norm.x /= lnorm;
                //norm.y /= lnorm;

                if (isInsideTheContour(pos)) {
                    res.putPixel(x, y, column);
                } else {
                    res.putPixel(x, y, 0);
                }
            }
        }
        return res;
    }

    /**
     * Check if the point is inside the active contour
     *
     * @param pixel the point
     * @return boolean is or not inside
     */
    boolean isInsideTheContour(PixelPos pixel) {

        int iCount;
        double bden, bnum, bres, ares;
        double lnorm;

        PixelPos norm = new PixelPos();
        PixelPos ref = new PixelPos();

        ref.x = 0f;
        ref.y = 0f;
        norm.x = ref.x - pixel.x;
        norm.y = ref.y - pixel.y;
        lnorm = Math.sqrt(norm.x * norm.x + norm.y * norm.y);
        norm.x /= lnorm;
        norm.y /= lnorm;

        iCount = 0;
        for (int i = 0; i < NR_POINTS - 1; i++) {
            bden = (-norm.x * points[i + 1].y + norm.x * points[i].y + norm.y * points[i + 1].x - norm.y * points[i].x);
            bnum = (-norm.x * pixel.y + norm.x * points[i].y + norm.y * pixel.x - norm.y * points[i].x);
            if (bden != 0) {
                bres = (bnum / bden);
            } else {
                bres = 5.0;
            }
            if ((bres >= 0.0) && (bres <= 1.0)) {
                ares = -(-points[i + 1].y * pixel.x + points[i + 1].y * points[i].x
                        + points[i].y * pixel.x + pixel.y * points[i + 1].x - pixel.y * points[i].x
                        - points[i].y * points[i + 1].x) / (-norm.x * points[i + 1].y
                        + norm.x * points[i].y + norm.y * points[i + 1].x - norm.y * points[i].x);
                if ((ares >= 0.0) && (ares <= lnorm)) {
                    iCount++;
                }
            }
        }
        // last point
        int i = NR_POINTS - 1;
        bden = (-norm.x * points[0].y + norm.x * points[i].y + norm.y * points[0].x - norm.y * points[i].x);
        bnum = (-norm.x * pixel.y + norm.x * points[i].y + norm.y * pixel.x - norm.y * points[i].x);
        if (bden != 0) {
            bres = (bnum / bden);
        } else {
            bres = 5.0;
        }
        if ((bres >= 0.0) && (bres <= 1.0)) {
            ares = -(-points[0].y * pixel.x + points[0].y * points[i].x
                    + points[i].y * pixel.x + pixel.y * points[0].x - pixel.y * points[i].x
                    - points[i].y * points[0].x) / (-norm.x * points[0].y
                    + norm.x * points[i].y + norm.y * points[0].x - norm.y * points[i].x);
            if ((ares >= 0.0) && (ares <= lnorm)) {
                iCount++;
            }
        }
        return (iCount % 2 == 1);
    }
}
