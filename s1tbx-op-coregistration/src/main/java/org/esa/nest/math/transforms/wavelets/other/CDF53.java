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
 * @date 17.08.2014 08:41:55
 *
 */
package org.esa.nest.math.transforms.wavelets.other;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Cohen Daubechies Feauveau (CDF) 9/7 wavelet. THIS WAVELET IS NOT WORKING -
 * DUE TO ODD NUMBER COEFFICIENTS!!!
 * 
 * @date 17.08.2014 08:41:55
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class CDF53 extends Wavelet {

  /**
   * THIS WAVELET IS NOT WORKING - DUE TO ODD NUMBER COEFFICIENTS!!!
   * 
   * @date 17.08.2014 08:41:55
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  @Deprecated public CDF53( ) {

    _name = "Cohen Daubechies Feauveau (CDF) 9/7"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 5; // wavelength of mother wavelet

    //    double sqrt2 = Math.sqrt( 2. );

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -1. / 8.; // - 1/8
    _scalingDeCom[ 1 ] = 1. / 4.; // + 2/8
    _scalingDeCom[ 2 ] = 3. / 4.; // + 6/8
    _scalingDeCom[ 3 ] = 1. / 4.; // + 2/8
    _scalingDeCom[ 4 ] = -1. / 8.; // - 1/8

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0; // 
    _waveletDeCom[ 1 ] = 1. / 2.; // 
    _waveletDeCom[ 2 ] = 1.; // 
    _waveletDeCom[ 3 ] = 1. / 2.; // 
    _waveletDeCom[ 4 ] = 0; // 

    // Copy to reconstruction filters due to orthogonality!
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {
      _scalingReCon[ i ] = _scalingDeCom[ i ];
      _waveletReCon[ i ] = _waveletDeCom[ i ];
    } // i

  } // CDF53

} // class
