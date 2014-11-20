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
 * BiOrthogonal Wavelet of type 3.9 - Three vanishing moments in wavelet
 * function and nine vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 17:25:01
 */
public class BiOrthogonal39 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior3.9/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:25:01
   */
  public BiOrthogonal39( ) {

    _name = "BiOrthogonal 3/9"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 20; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.000679744372783699;
    _scalingDeCom[ 1 ] = 0.002039233118351097;
    _scalingDeCom[ 2 ] = 0.005060319219611981;
    _scalingDeCom[ 3 ] = -0.020618912641105536;
    _scalingDeCom[ 4 ] = -0.014112787930175846;
    _scalingDeCom[ 5 ] = 0.09913478249423216;
    _scalingDeCom[ 6 ] = 0.012300136269419315;
    _scalingDeCom[ 7 ] = -0.32019196836077857;
    _scalingDeCom[ 8 ] = 0.0020500227115698858;
    _scalingDeCom[ 9 ] = 0.9421257006782068;
    _scalingDeCom[ 10 ] = 0.9421257006782068;
    _scalingDeCom[ 11 ] = 0.0020500227115698858;
    _scalingDeCom[ 12 ] = -0.32019196836077857;
    _scalingDeCom[ 13 ] = 0.012300136269419315;
    _scalingDeCom[ 14 ] = 0.09913478249423216;
    _scalingDeCom[ 15 ] = -0.014112787930175846;
    _scalingDeCom[ 16 ] = -0.020618912641105536;
    _scalingDeCom[ 17 ] = 0.005060319219611981;
    _scalingDeCom[ 18 ] = 0.002039233118351097;
    _scalingDeCom[ 19 ] = -0.000679744372783699;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = 0.;
    _waveletDeCom[ 3 ] = 0.;
    _waveletDeCom[ 4 ] = 0.;
    _waveletDeCom[ 5 ] = 0.;
    _waveletDeCom[ 6 ] = 0.;
    _waveletDeCom[ 7 ] = 0.;
    _waveletDeCom[ 8 ] = -0.1767766952966369;
    _waveletDeCom[ 9 ] = 0.5303300858899107;
    _waveletDeCom[ 10 ] = -0.5303300858899107;
    _waveletDeCom[ 11 ] = 0.1767766952966369;
    _waveletDeCom[ 12 ] = 0.;
    _waveletDeCom[ 13 ] = 0.;
    _waveletDeCom[ 14 ] = 0.;
    _waveletDeCom[ 15 ] = 0.;
    _waveletDeCom[ 16 ] = 0.;
    _waveletDeCom[ 17 ] = 0.;
    _waveletDeCom[ 18 ] = 0.;
    _waveletDeCom[ 19 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal39

} // BiOrthogonal39