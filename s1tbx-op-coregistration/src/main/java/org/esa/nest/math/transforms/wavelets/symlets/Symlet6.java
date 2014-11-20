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
 * Symlet6 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:21:44
 */
public class Symlet6 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym6/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:21:44
   */
  public Symlet6( ) {

    _name = "Symlet 6"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 12; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.015404109327027373;
    _scalingDeCom[ 1 ] = 0.0034907120842174702;
    _scalingDeCom[ 2 ] = -0.11799011114819057;
    _scalingDeCom[ 3 ] = -0.048311742585633;
    _scalingDeCom[ 4 ] = 0.4910559419267466;
    _scalingDeCom[ 5 ] = 0.787641141030194;
    _scalingDeCom[ 6 ] = 0.3379294217276218;
    _scalingDeCom[ 7 ] = -0.07263752278646252;
    _scalingDeCom[ 8 ] = -0.021060292512300564;
    _scalingDeCom[ 9 ] = 0.04472490177066578;
    _scalingDeCom[ 10 ] = 0.0017677118642428036;
    _scalingDeCom[ 11 ] = -0.007800708325034148;

    _buildOrthonormalSpace( );
    
  } // Symlet6

} // Symlet6
