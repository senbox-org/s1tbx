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
 * Symlet9 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:31:46
 */
public class Symlet9 extends Wavelet {

  /**
   * TODO Comment me please! * Already orthonormal coefficients taken from Filip
   * Wasilewski's webpage http://wavelets.pybytes.com/wavelet/sym9/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:31:46
   */
  public Symlet9( ) {

    _name = "Symlet 9"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 18; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.0014009155259146807;
    _scalingDeCom[ 1 ] = 0.0006197808889855868;
    _scalingDeCom[ 2 ] = -0.013271967781817119;
    _scalingDeCom[ 3 ] = -0.01152821020767923;
    _scalingDeCom[ 4 ] = 0.03022487885827568;
    _scalingDeCom[ 5 ] = 0.0005834627461258068;
    _scalingDeCom[ 6 ] = -0.05456895843083407;
    _scalingDeCom[ 7 ] = 0.238760914607303;
    _scalingDeCom[ 8 ] = 0.717897082764412;
    _scalingDeCom[ 9 ] = 0.6173384491409358;
    _scalingDeCom[ 10 ] = 0.035272488035271894;
    _scalingDeCom[ 11 ] = -0.19155083129728512;
    _scalingDeCom[ 12 ] = -0.018233770779395985;
    _scalingDeCom[ 13 ] = 0.06207778930288603;
    _scalingDeCom[ 14 ] = 0.008859267493400484;
    _scalingDeCom[ 15 ] = -0.010264064027633142;
    _scalingDeCom[ 16 ] = -0.0004731544986800831;
    _scalingDeCom[ 17 ] = 0.0010694900329086053;

    _buildOrthonormalSpace( );

  } // Symlet9

} // Symlet9
