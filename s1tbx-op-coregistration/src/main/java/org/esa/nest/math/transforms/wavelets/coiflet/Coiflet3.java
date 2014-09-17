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
package org.esa.nest.math.transforms.wavelets.coiflet;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal Coiflet wavelet of 18 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 15.02.2014 22:58:59
 */
public class Coiflet3 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/coif3/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:58:59
   */
  public Coiflet3( ) {

    _name = "Coiflet 3"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 18; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -3.459977283621256e-05;
    _scalingDeCom[ 1 ] = -7.098330313814125e-05;
    _scalingDeCom[ 2 ] = 0.0004662169601128863;
    _scalingDeCom[ 3 ] = 0.0011175187708906016;
    _scalingDeCom[ 4 ] = -0.0025745176887502236;
    _scalingDeCom[ 5 ] = -0.00900797613666158;
    _scalingDeCom[ 6 ] = 0.015880544863615904;
    _scalingDeCom[ 7 ] = 0.03455502757306163;
    _scalingDeCom[ 8 ] = -0.08230192710688598;
    _scalingDeCom[ 9 ] = -0.07179982161931202;
    _scalingDeCom[ 10 ] = 0.42848347637761874;
    _scalingDeCom[ 11 ] = 0.7937772226256206;
    _scalingDeCom[ 12 ] = 0.4051769024096169;
    _scalingDeCom[ 13 ] = -0.06112339000267287;
    _scalingDeCom[ 14 ] = -0.0657719112818555;
    _scalingDeCom[ 15 ] = 0.023452696141836267;
    _scalingDeCom[ 16 ] = 0.007782596427325418;
    _scalingDeCom[ 17 ] = -0.003793512864491014;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Coiflet3

} // Coiflet3