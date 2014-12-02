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
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 26 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:10:54
 */
public class Daubechies13 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db13/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:10:54
   */
  public Daubechies13( ) {

    _name = "Daubechies 13"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 26; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
	
    _scalingDeCom[ 0 ] = 5.2200350984548e-07;
    _scalingDeCom[ 1 ] = -4.700416479360808e-06;
    _scalingDeCom[ 2 ] = 1.0441930571407941e-05;
    _scalingDeCom[ 3 ] = 3.067853757932436e-05;
    _scalingDeCom[ 4 ] = -0.0001651289885565057;
    _scalingDeCom[ 5 ] = 4.9251525126285676e-05;
    _scalingDeCom[ 6 ] = 0.000932326130867249;
    _scalingDeCom[ 7 ] = -0.0013156739118922766;
    _scalingDeCom[ 8 ] = -0.002761911234656831;
    _scalingDeCom[ 9 ] = 0.007255589401617119;
    _scalingDeCom[ 10 ] = 0.003923941448795577;
    _scalingDeCom[ 11 ] = -0.02383142071032781;
    _scalingDeCom[ 12 ] = 0.002379972254052227;
    _scalingDeCom[ 13 ] = 0.056139477100276156;
    _scalingDeCom[ 14 ] = -0.026488406475345658;
    _scalingDeCom[ 15 ] = -0.10580761818792761;
    _scalingDeCom[ 16 ] = 0.07294893365678874;
    _scalingDeCom[ 17 ] = 0.17947607942935084;
    _scalingDeCom[ 18 ] = -0.12457673075080665;
    _scalingDeCom[ 19 ] = -0.31497290771138414;
    _scalingDeCom[ 20 ] = 0.086985726179645;
    _scalingDeCom[ 21 ] = 0.5888895704312119;
    _scalingDeCom[ 22 ] = 0.6110558511587811;
    _scalingDeCom[ 23 ] = 0.3119963221604349;
    _scalingDeCom[ 24 ] = 0.08286124387290195;
    _scalingDeCom[ 25 ] = 0.009202133538962279;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies13

} // Daubechies13