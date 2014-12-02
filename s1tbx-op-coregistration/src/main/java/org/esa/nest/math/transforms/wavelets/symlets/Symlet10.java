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
 * Symlet10 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 14:35:09
 */
public class Symlet10 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym10/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 14:35:09
   */
  public Symlet10( ) {

    _name = "Symlet 10"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 20; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.0007701598091144901;
    _scalingDeCom[ 1 ] = 9.563267072289475e-05;
    _scalingDeCom[ 2 ] = -0.008641299277022422;
    _scalingDeCom[ 3 ] = -0.0014653825813050513;
    _scalingDeCom[ 4 ] = 0.0459272392310922;
    _scalingDeCom[ 5 ] = 0.011609893903711381;
    _scalingDeCom[ 6 ] = -0.15949427888491757;
    _scalingDeCom[ 7 ] = -0.07088053578324385;
    _scalingDeCom[ 8 ] = 0.47169066693843925;
    _scalingDeCom[ 9 ] = 0.7695100370211071;
    _scalingDeCom[ 10 ] = 0.38382676106708546;
    _scalingDeCom[ 11 ] = -0.03553674047381755;
    _scalingDeCom[ 12 ] = -0.0319900568824278;
    _scalingDeCom[ 13 ] = 0.04999497207737669;
    _scalingDeCom[ 14 ] = 0.005764912033581909;
    _scalingDeCom[ 15 ] = -0.02035493981231129;
    _scalingDeCom[ 16 ] = -0.0008043589320165449;
    _scalingDeCom[ 17 ] = 0.004593173585311828;
    _scalingDeCom[ 18 ] = 5.7036083618494284e-05;
    _scalingDeCom[ 19 ] = -0.0004593294210046588;

    _buildOrthonormalSpace( );

  } // Symlet10

} // Symlet10
