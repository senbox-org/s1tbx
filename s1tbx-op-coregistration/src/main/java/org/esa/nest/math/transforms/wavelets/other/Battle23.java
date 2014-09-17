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
package org.esa.nest.math.transforms.wavelets.other;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * The Battle 23 Wavelet from Mallat's book: "A Theory for Multiresolution
 * Signal Decomposition: The Wavelet Representation", IEEE PAMI, v. 11, no. 7,
 * 674-693, Table 1
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 15.02.2014 23:19:07
 */
@Deprecated public class Battle23 extends Wavelet {

  /**
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 23:23:23
   */
  public Battle23( ) {

    _name = "Battle 23"; // name of the wavelet

    _transformWavelength = 8; // minimal wavelength of input signal

    _motherWavelength = 23; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.002;
    _scalingDeCom[ 1 ] = -0.003;
    _scalingDeCom[ 2 ] = 0.006;
    _scalingDeCom[ 3 ] = 0.006;
    _scalingDeCom[ 4 ] = -0.013;
    _scalingDeCom[ 5 ] = -0.012;
    _scalingDeCom[ 6 ] = 0.030;
    _scalingDeCom[ 7 ] = 0.023;
    _scalingDeCom[ 8 ] = -0.078;
    _scalingDeCom[ 9 ] = -0.035;
    _scalingDeCom[ 10 ] = 0.307;
    _scalingDeCom[ 11 ] = 0.542;
    _scalingDeCom[ 12 ] = 0.307;
    _scalingDeCom[ 13 ] = -0.035;
    _scalingDeCom[ 14 ] = -0.078;
    _scalingDeCom[ 15 ] = 0.023;
    _scalingDeCom[ 16 ] = 0.030;
    _scalingDeCom[ 17 ] = -0.012;
    _scalingDeCom[ 18 ] = -0.013;
    _scalingDeCom[ 19 ] = 0.006;
    _scalingDeCom[ 20 ] = 0.006;
    _scalingDeCom[ 21 ] = -0.003;
    _scalingDeCom[ 22 ] = -0.002;

    // building wavelet as orthogonal (orthonormal) space from
    // scaling coefficients (low pass filter). Have a look into
    // Alfred Haar's wavelet or the Daubechie Wavelet with 2
    // vanishing moments for understanding what is done here. ;-)
    _waveletDeCom = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ )
      if( i % 2 == 0 )
        _waveletDeCom[ i ] = _scalingDeCom[ ( _motherWavelength - 1 ) - i ];
      else
        _waveletDeCom[ i ] = -_scalingDeCom[ ( _motherWavelength - 1 ) - i ];

    // Copy to reconstruction filters due to orthogonality (orthonormality)!
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {

      _scalingReCon[ i ] = _scalingDeCom[ i ];
      _waveletReCon[ i ] = _waveletDeCom[ i ];

    } // i

  } // Battle23

} // Battle23
