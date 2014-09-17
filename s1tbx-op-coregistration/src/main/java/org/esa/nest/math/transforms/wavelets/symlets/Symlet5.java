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
 * Symlet5 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:17:36
 */
public class Symlet5 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym5/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:17:36
   */
  public Symlet5( ) {

    _name = "Symlet 4"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 10; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.027333068345077982;
    _scalingDeCom[ 1 ] = 0.029519490925774643;
    _scalingDeCom[ 2 ] = -0.039134249302383094;
    _scalingDeCom[ 3 ] = 0.1993975339773936;
    _scalingDeCom[ 4 ] = 0.7234076904024206;
    _scalingDeCom[ 5 ] = 0.6339789634582119;
    _scalingDeCom[ 6 ] = 0.01660210576452232;
    _scalingDeCom[ 7 ] = -0.17532808990845047;
    _scalingDeCom[ 8 ] = -0.021101834024758855;
    _scalingDeCom[ 9 ] = 0.019538882735286728;

    _buildOrthonormalSpace( );

  } // Symlet5

} // Symlet5
