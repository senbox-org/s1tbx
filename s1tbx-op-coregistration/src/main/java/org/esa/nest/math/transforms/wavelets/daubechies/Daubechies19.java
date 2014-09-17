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
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 38 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:11:00
 */
public class Daubechies19 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db19/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:11:00
   */
  public Daubechies19( ) {

    _name = "Daubechies 19"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 38; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 8.666848839034483e-10;
    _scalingDeCom[ 1 ] = -1.1164020670405678e-08;
    _scalingDeCom[ 2 ] = 4.636937775802368e-08;
    _scalingDeCom[ 3 ] = 1.447088298804088e-08;
    _scalingDeCom[ 4 ] = -6.86275565779811e-07;
    _scalingDeCom[ 5 ] = 1.531931476697877e-06;
    _scalingDeCom[ 6 ] = 3.0109643163099385e-06;
    _scalingDeCom[ 7 ] = -1.664017629722462e-05;
    _scalingDeCom[ 8 ] = 5.105950487090694e-06;
    _scalingDeCom[ 9 ] = 8.711270467250443e-05;
    _scalingDeCom[ 10 ] = -0.00012460079173506306;
    _scalingDeCom[ 11 ] = -0.0002606761356811995;
    _scalingDeCom[ 12 ] = 0.0007358025205041731;
    _scalingDeCom[ 13 ] = 0.00034180865344939543;
    _scalingDeCom[ 14 ] = -0.002687551800734441;
    _scalingDeCom[ 15 ] = 0.0007689543592242488;
    _scalingDeCom[ 16 ] = 0.007040747367080495;
    _scalingDeCom[ 17 ] = -0.005866922281112195;
    _scalingDeCom[ 18 ] = -0.013988388678695632;
    _scalingDeCom[ 19 ] = 0.019375549889114482;
    _scalingDeCom[ 20 ] = 0.021623767409452484;
    _scalingDeCom[ 21 ] = -0.04567422627778492;
    _scalingDeCom[ 22 ] = -0.026501236250778635;
    _scalingDeCom[ 23 ] = 0.0869067555554507;
    _scalingDeCom[ 24 ] = 0.02758435062488713;
    _scalingDeCom[ 25 ] = -0.14278569504021468;
    _scalingDeCom[ 26 ] = -0.03351854190320226;
    _scalingDeCom[ 27 ] = 0.21234974330662043;
    _scalingDeCom[ 28 ] = 0.07465226970806647;
    _scalingDeCom[ 29 ] = -0.28583863175723145;
    _scalingDeCom[ 30 ] = -0.22809139421653665;
    _scalingDeCom[ 31 ] = 0.2608949526521201;
    _scalingDeCom[ 32 ] = 0.6017045491300916;
    _scalingDeCom[ 33 ] = 0.5244363774668862;
    _scalingDeCom[ 34 ] = 0.26438843174202237;
    _scalingDeCom[ 35 ] = 0.08127811326580564;
    _scalingDeCom[ 36 ] = 0.01428109845082521;
    _scalingDeCom[ 37 ] = 0.0011086697631864314;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies19

} // Daubechies19