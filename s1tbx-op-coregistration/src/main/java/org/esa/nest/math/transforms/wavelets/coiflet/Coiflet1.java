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
 * Ingrid Daubechies' orthonormal Coiflet wavelet of six coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 15.02.2014 22:27:55
 */
public class Coiflet1 extends Wavelet {

  /**
   * Constructor calculating analytically the orthogonal Coiflet wavelet of 6
   * coefficients, orthonormalizes them (normed, due to ||*||2 - euclidean
   * norm), and spreads the scaling coefficients afterwards; .
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:27:55
   */
  public Coiflet1( ) {

    _name = "Coiflet 1"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 6; // wavelength of mother wavelet

    double sqrt02 = 1.4142135623730951;
    double sqrt15 = Math.sqrt( 15. );

    // these coefficients are already orthonormal
    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = sqrt02 * ( sqrt15 - 3. ) / 32.; //  -0.01565572813546454;
    _scalingDeCom[ 1 ] = sqrt02 * ( 1. - sqrt15 ) / 32.; // -0.0727326195128539;
    _scalingDeCom[ 2 ] = sqrt02 * ( 6. - 2 * sqrt15 ) / 32.; //  0.38486484686420286;
    _scalingDeCom[ 3 ] = sqrt02 * ( 2. * sqrt15 + 6. ) / 32.; // 0.8525720202122554;
    _scalingDeCom[ 4 ] = sqrt02 * ( sqrt15 + 13. ) / 32.; // 0.3378976624578092;
    _scalingDeCom[ 5 ] = sqrt02 * ( 9. - sqrt15 ) / 32.; //-0.0727326195128539;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Coiflet1

} // class
