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
package org.esa.nest.math.transforms.wavelets.symlets;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Symlet10 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:23:13
 */
public class Symlet13 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym13/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:13
   */
  public Symlet13( ) {

    _name = "Symlet 13"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 26; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 6.820325263075319e-05;
    _scalingDeCom[ 1 ] = -3.573862364868901e-05;
    _scalingDeCom[ 2 ] = -0.0011360634389281183;
    _scalingDeCom[ 3 ] = -0.0001709428585302221;
    _scalingDeCom[ 4 ] = 0.0075262253899681;
    _scalingDeCom[ 5 ] = 0.005296359738725025;
    _scalingDeCom[ 6 ] = -0.02021676813338983;
    _scalingDeCom[ 7 ] = -0.017211642726299048;
    _scalingDeCom[ 8 ] = 0.013862497435849205;
    _scalingDeCom[ 9 ] = -0.0597506277179437;
    _scalingDeCom[ 10 ] = -0.12436246075153011;
    _scalingDeCom[ 11 ] = 0.19770481877117801;
    _scalingDeCom[ 12 ] = 0.6957391505614964;
    _scalingDeCom[ 13 ] = 0.6445643839011856;
    _scalingDeCom[ 14 ] = 0.11023022302137217;
    _scalingDeCom[ 15 ] = -0.14049009311363403;
    _scalingDeCom[ 16 ] = 0.008819757670420546;
    _scalingDeCom[ 17 ] = 0.09292603089913712;
    _scalingDeCom[ 18 ] = 0.017618296880653084;
    _scalingDeCom[ 19 ] = -0.020749686325515677;
    _scalingDeCom[ 20 ] = -0.0014924472742598532;
    _scalingDeCom[ 21 ] = 0.0056748537601224395;
    _scalingDeCom[ 22 ] = 0.00041326119884196064;
    _scalingDeCom[ 23 ] = -0.0007213643851362283;
    _scalingDeCom[ 24 ] = 3.690537342319624e-05;
    _scalingDeCom[ 25 ] = 7.042986690694402e-05;

    _buildOrthonormalSpace( );

  } // Symlet13

} // Symlet13