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
 * @date 19.08.2014 18:23:17
 */
public class Symlet17 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym17/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:17
   */
  public Symlet17( ) {

    _name = "Symlet 17"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 34; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 4.297343327345983e-06;
    _scalingDeCom[ 1 ] = 2.7801266938414138e-06;
    _scalingDeCom[ 2 ] = -6.293702597554192e-05;
    _scalingDeCom[ 3 ] = -1.3506383399901165e-05;
    _scalingDeCom[ 4 ] = 0.0004759963802638669;
    _scalingDeCom[ 5 ] = -0.000138642302680455;
    _scalingDeCom[ 6 ] = -0.0027416759756816018;
    _scalingDeCom[ 7 ] = 0.0008567700701915741;
    _scalingDeCom[ 8 ] = 0.010482366933031529;
    _scalingDeCom[ 9 ] = -0.004819212803176148;
    _scalingDeCom[ 10 ] = -0.03329138349235933;
    _scalingDeCom[ 11 ] = 0.01790395221434112;
    _scalingDeCom[ 12 ] = 0.10475461484223211;
    _scalingDeCom[ 13 ] = 0.0172711782105185;
    _scalingDeCom[ 14 ] = -0.11856693261143636;
    _scalingDeCom[ 15 ] = 0.1423983504146782;
    _scalingDeCom[ 16 ] = 0.6507166292045456;
    _scalingDeCom[ 17 ] = 0.681488995344925;
    _scalingDeCom[ 18 ] = 0.18053958458111286;
    _scalingDeCom[ 19 ] = -0.15507600534974825;
    _scalingDeCom[ 20 ] = -0.08607087472073338;
    _scalingDeCom[ 21 ] = 0.016158808725919346;
    _scalingDeCom[ 22 ] = -0.007261634750928767;
    _scalingDeCom[ 23 ] = -0.01803889724191924;
    _scalingDeCom[ 24 ] = 0.009952982523509598;
    _scalingDeCom[ 25 ] = 0.012396988366648726;
    _scalingDeCom[ 26 ] = -0.001905407689852666;
    _scalingDeCom[ 27 ] = -0.003932325279797902;
    _scalingDeCom[ 28 ] = 5.8400428694052584e-05;
    _scalingDeCom[ 29 ] = 0.0007198270642148971;
    _scalingDeCom[ 30 ] = 2.520793314082878e-05;
    _scalingDeCom[ 31 ] = -7.607124405605129e-05;
    _scalingDeCom[ 32 ] = -2.4527163425833e-06;
    _scalingDeCom[ 33 ] = 3.7912531943321266e-06;


    _buildOrthonormalSpace( );

  } // Symlet17

} // Symlet17