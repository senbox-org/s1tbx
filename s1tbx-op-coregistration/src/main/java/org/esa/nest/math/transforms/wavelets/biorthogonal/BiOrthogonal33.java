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
 * BiOrthogonal Wavelet of type 3.3 - Three vanishing moments in wavelet
 * function and three vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 17:14:24
 */
public class BiOrthogonal33 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior3.3/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:14:24
   */
  public BiOrthogonal33( ) {

    _name = "BiOrthogonal 3/3"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 8; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.06629126073623884;
    _scalingDeCom[ 1 ] = -0.19887378220871652;
    _scalingDeCom[ 2 ] = -0.15467960838455727;
    _scalingDeCom[ 3 ] = 0.9943689110435825;
    _scalingDeCom[ 4 ] = 0.9943689110435825;
    _scalingDeCom[ 5 ] = -0.15467960838455727;
    _scalingDeCom[ 6 ] = -0.19887378220871652;
    _scalingDeCom[ 7 ] = 0.06629126073623884;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = -0.1767766952966369;
    _waveletDeCom[ 3 ] = 0.5303300858899107;
    _waveletDeCom[ 4 ] = -0.5303300858899107;
    _waveletDeCom[ 5 ] = 0.1767766952966369;
    _waveletDeCom[ 6 ] = 0.;
    _waveletDeCom[ 7 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal33

} // BiOrthogonal33