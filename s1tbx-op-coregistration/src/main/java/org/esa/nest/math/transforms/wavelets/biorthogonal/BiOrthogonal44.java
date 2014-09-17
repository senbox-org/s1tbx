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
package org.esa.nest.math.transforms.wavelets.biorthogonal;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * BiOrthogonal Wavelet of type 4.4 - Four vanishing moments in wavelet function
 * and four vanishing moments in scaling function - 44 muhahaha! ~8>
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 17:36:17
 */
public class BiOrthogonal44 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior4.4/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:36:17
   */
  public BiOrthogonal44( ) {

    _name = "BiOrthogonal 4/4"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 10; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = 0.03782845550726404;
    _scalingDeCom[ 2 ] = -0.023849465019556843;
    _scalingDeCom[ 3 ] = -0.11062440441843718;
    _scalingDeCom[ 4 ] = 0.37740285561283066;
    _scalingDeCom[ 5 ] = 0.8526986790088938;
    _scalingDeCom[ 6 ] = 0.37740285561283066;
    _scalingDeCom[ 7 ] = -0.11062440441843718;
    _scalingDeCom[ 8 ] = -0.023849465019556843;
    _scalingDeCom[ 9 ] = 0.03782845550726404;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = -0.06453888262869706;
    _waveletDeCom[ 2 ] = 0.04068941760916406;
    _waveletDeCom[ 3 ] = 0.41809227322161724;
    _waveletDeCom[ 4 ] = -0.7884856164055829;
    _waveletDeCom[ 5 ] = 0.41809227322161724;
    _waveletDeCom[ 6 ] = 0.04068941760916406;
    _waveletDeCom[ 7 ] = -0.06453888262869706;
    _waveletDeCom[ 8 ] = 0.;
    _waveletDeCom[ 9 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal44

} // BiOrthogonal44