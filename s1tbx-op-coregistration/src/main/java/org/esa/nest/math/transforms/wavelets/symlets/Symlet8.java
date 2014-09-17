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
 * Symlet8 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:28:42
 */
public class Symlet8 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym8/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:28:42
   */
  public Symlet8( ) {

    _name = "Symlet 8"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 16; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.0033824159510061256;
    _scalingDeCom[ 1 ] = -0.0005421323317911481;
    _scalingDeCom[ 2 ] = 0.03169508781149298;
    _scalingDeCom[ 3 ] = 0.007607487324917605;
    _scalingDeCom[ 4 ] = -0.1432942383508097;
    _scalingDeCom[ 5 ] = -0.061273359067658524;
    _scalingDeCom[ 6 ] = 0.4813596512583722;
    _scalingDeCom[ 7 ] = 0.7771857517005235;
    _scalingDeCom[ 8 ] = 0.3644418948353314;
    _scalingDeCom[ 9 ] = -0.05194583810770904;
    _scalingDeCom[ 10 ] = -0.027219029917056003;
    _scalingDeCom[ 11 ] = 0.049137179673607506;
    _scalingDeCom[ 12 ] = 0.003808752013890615;
    _scalingDeCom[ 13 ] = -0.01495225833704823;
    _scalingDeCom[ 14 ] = -0.0003029205147213668;
    _scalingDeCom[ 15 ] = 0.0018899503327594609;

    _buildOrthonormalSpace( );

  } // Symlet8

} // Symlet8
