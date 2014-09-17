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
 * Symlet7 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:24:20
 */
public class Symlet7 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym7/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:24:20
   */
  public Symlet7( ) {

    _name = "Symlet 7"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 14; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.002681814568257878;
    _scalingDeCom[ 1 ] = -0.0010473848886829163;
    _scalingDeCom[ 2 ] = -0.01263630340325193;
    _scalingDeCom[ 3 ] = 0.03051551316596357;
    _scalingDeCom[ 4 ] = 0.0678926935013727;
    _scalingDeCom[ 5 ] = -0.049552834937127255;
    _scalingDeCom[ 6 ] = 0.017441255086855827;
    _scalingDeCom[ 7 ] = 0.5361019170917628;
    _scalingDeCom[ 8 ] = 0.767764317003164;
    _scalingDeCom[ 9 ] = 0.2886296317515146;
    _scalingDeCom[ 10 ] = -0.14004724044296152;
    _scalingDeCom[ 11 ] = -0.10780823770381774;
    _scalingDeCom[ 12 ] = 0.004010244871533663;
    _scalingDeCom[ 13 ] = 0.010268176708511255;

    _buildOrthonormalSpace( );

  } // Symlet7

} // Symlet7
