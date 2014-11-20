/**
 * JWave - Java implementation of wavelet transform algorithms
 *
 * Copyright 2008-2014 Christian Scheiblich
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * This file is part of JWave.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 23.05.2008 17:42:23
 *
 */
package org.esa.nest.math.transforms.wavelets.daubechies;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 32 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:10:57
 */
public class Daubechies16 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db16/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:10:57
   */
  public Daubechies16( ) {

    _name = "Daubechies 16"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 32; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = -2.1093396300980412e-08;
    _scalingDeCom[ 1 ] = 2.3087840868545578e-07;
    _scalingDeCom[ 2 ] = -7.363656785441815e-07;
    _scalingDeCom[ 3 ] = -1.0435713423102517e-06;
    _scalingDeCom[ 4 ] = 1.133660866126152e-05;
    _scalingDeCom[ 5 ] = -1.394566898819319e-05;
    _scalingDeCom[ 6 ] = -6.103596621404321e-05;
    _scalingDeCom[ 7 ] = 0.00017478724522506327;
    _scalingDeCom[ 8 ] = 0.00011424152003843815;
    _scalingDeCom[ 9 ] = -0.0009410217493585433;
    _scalingDeCom[ 10 ] = 0.00040789698084934395;
    _scalingDeCom[ 11 ] = 0.00312802338120381;
    _scalingDeCom[ 12 ] = -0.0036442796214883506;
    _scalingDeCom[ 13 ] = -0.006990014563390751;
    _scalingDeCom[ 14 ] = 0.013993768859843242;
    _scalingDeCom[ 15 ] = 0.010297659641009963;
    _scalingDeCom[ 16 ] = -0.036888397691556774;
    _scalingDeCom[ 17 ] = -0.007588974368642594;
    _scalingDeCom[ 18 ] = 0.07592423604445779;
    _scalingDeCom[ 19 ] = -0.006239722752156254;
    _scalingDeCom[ 20 ] = -0.13238830556335474;
    _scalingDeCom[ 21 ] = 0.027340263752899923;
    _scalingDeCom[ 22 ] = 0.21119069394696974;
    _scalingDeCom[ 23 ] = -0.02791820813292813;
    _scalingDeCom[ 24 ] = -0.3270633105274758;
    _scalingDeCom[ 25 ] = -0.08975108940236352;
    _scalingDeCom[ 26 ] = 0.44029025688580486;
    _scalingDeCom[ 27 ] = 0.6373563320829833;
    _scalingDeCom[ 28 ] = 0.43031272284545874;
    _scalingDeCom[ 29 ] = 0.1650642834886438;
    _scalingDeCom[ 30 ] = 0.03490771432362905;
    _scalingDeCom[ 31 ] = 0.0031892209253436892;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies16

} // Daubechies16