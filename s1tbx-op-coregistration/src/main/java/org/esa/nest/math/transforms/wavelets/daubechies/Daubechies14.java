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
package org.esa.nest.math.transforms.wavelets.daubechies;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 28 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:10:55
 */
public class Daubechies14 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db14/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:10:55
   */
  public Daubechies14( ) {

    _name = "Daubechies 14"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 28; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
	
    _scalingDeCom[ 0 ] = -1.7871399683109222e-07;
    _scalingDeCom[ 1 ] = 1.7249946753674012e-06;
    _scalingDeCom[ 2 ] = -4.389704901780418e-06;
    _scalingDeCom[ 3 ] = -1.0337209184568496e-05;
    _scalingDeCom[ 4 ] = 6.875504252695734e-05;
    _scalingDeCom[ 5 ] = -4.177724577037067e-05;
    _scalingDeCom[ 6 ] = -0.00038683194731287514;
    _scalingDeCom[ 7 ] = 0.0007080211542354048;
    _scalingDeCom[ 8 ] = 0.001061691085606874;
    _scalingDeCom[ 9 ] = -0.003849638868019787;
    _scalingDeCom[ 10 ] = -0.0007462189892638753;
    _scalingDeCom[ 11 ] = 0.01278949326634007;
    _scalingDeCom[ 12 ] = -0.0056150495303375755;
    _scalingDeCom[ 13 ] = -0.030185351540353976;
    _scalingDeCom[ 14 ] = 0.02698140830794797;
    _scalingDeCom[ 15 ] = 0.05523712625925082;
    _scalingDeCom[ 16 ] = -0.0715489555039835;
    _scalingDeCom[ 17 ] = -0.0867484115681106;
    _scalingDeCom[ 18 ] = 0.13998901658445695;
    _scalingDeCom[ 19 ] = 0.13839521386479153;
    _scalingDeCom[ 20 ] = -0.2180335299932165;
    _scalingDeCom[ 21 ] = -0.27168855227867705;
    _scalingDeCom[ 22 ] = 0.21867068775886594;
    _scalingDeCom[ 23 ] = 0.6311878491047198;
    _scalingDeCom[ 24 ] = 0.5543056179407709;
    _scalingDeCom[ 25 ] = 0.25485026779256437;
    _scalingDeCom[ 26 ] = 0.062364758849384874;
    _scalingDeCom[ 27 ] = 0.0064611534600864905;


    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies14

} // Daubechies14