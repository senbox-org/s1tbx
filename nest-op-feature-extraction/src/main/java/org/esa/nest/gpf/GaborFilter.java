/*
Copyright (C) 2010

This file is part of the Gabor applet
written by Max Bügler
http://www.maxbuegler.eu/

Gabor applet is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2, or (at your option) any
later version.

Gabor applet is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.esa.nest.gpf;

/**
 * Date: May 7, 2010
 * Time: 3:25:34 PM
 * <p/>
 * Copyright 2010 by Max Buegler.
 * Licensed under General Public License Version 3
 */

public class GaborFilter {

    public static double[][] createGarborFilter(double lambda, double theta, double psi, double sigma, double gamma){
        double sigma_x = sigma;
        double sigma_y = sigma/gamma;

        // Bounding box
        int nstds = 3;
        int xmax = (int)Math.ceil(Math.max(1,Math.max(Math.abs(nstds*sigma_x*Math.cos(theta)),Math.abs(nstds*sigma_y*Math.sin(theta)))));
        int ymax = (int)Math.ceil(Math.max(1,Math.max(Math.abs(nstds*sigma_x*Math.sin(theta)),Math.abs(nstds*sigma_y*Math.cos(theta)))));

        double[][] out=new double[2*xmax+1][2*ymax+1];

        double sum=0;
        for (int x=-xmax;x<=xmax;x++){
            for (int y=-ymax;y<=ymax;y++){
                double x_theta=x*Math.cos(theta)+y*Math.sin(theta);
                double y_theta=-x*Math.sin(theta)+y*Math.cos(theta);
                out[x+xmax][y+ymax]=Math.exp(-(Math.pow(x_theta,2)+Math.pow(gamma,2)*Math.pow(y_theta,2))/(2*Math.pow(sigma,2)))*Math.cos(2*Math.PI*x_theta/lambda+psi);
                sum+=out[x+xmax][y+ymax];
                //out[x+xmax][y+ymax]= 1/(2*Math.PI*sigma_x *sigma_y) * Math.exp(-0.5*(Math.pow(x_theta,2)/Math.pow(sigma_x,2)+(Math.pow(y_theta,2)/(Math.pow(sigma_y,2)))*Math.cos(2*Math.PI/lambda*x_theta+psi);
            }
        }
        for (int x=-xmax;x<=xmax;x++){
            for (int y=-ymax;y<=ymax;y++){
                out[x+xmax][y+ymax]/=sum;
            }
        }
        return out;
    }


    public static int[][] applyGarborFilter(int[][] in,double[][] filter){
        int xmax=(int)Math.floor(filter.length/2.0);
        int ymax=(int)Math.floor(filter[0].length/2.0);
        int[][] out=new int[in.length][in[0].length];
        for (int x=0;x<in.length;x++){
            for (int y=0;y<in.length;y++){
                double sum=0;
                for (int xf=-xmax;xf<=xmax;xf++){
                    for (int yf=-ymax;yf<=ymax;yf++){
                        if (x-xf>=0&&x-xf<in.length&&y-yf>=0&&y-yf<in[0].length)
                        sum+=filter[xf+xmax][yf+ymax]*in[x-xf][y-yf];
                    }
                }
                out[x][y]=(int)Math.round(sum);
            }
        }
        return out;
    }
}
