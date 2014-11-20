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
 * @date 19.08.2014 18:23:16
 */
public class Symlet16 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym16/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:16
   */
  public Symlet16( ) {

    _name = "Symlet 16"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 32; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 6.230006701220761e-06;
    _scalingDeCom[ 1 ] = -3.113556407621969e-06;
    _scalingDeCom[ 2 ] = -0.00010943147929529757;
    _scalingDeCom[ 3 ] = 2.8078582128442894e-05;
    _scalingDeCom[ 4 ] = 0.0008523547108047095;
    _scalingDeCom[ 5 ] = -0.0001084456223089688;
    _scalingDeCom[ 6 ] = -0.0038809122526038786;
    _scalingDeCom[ 7 ] = 0.0007182119788317892;
    _scalingDeCom[ 8 ] = 0.012666731659857348;
    _scalingDeCom[ 9 ] = -0.0031265171722710075;
    _scalingDeCom[ 10 ] = -0.031051202843553064;
    _scalingDeCom[ 11 ] = 0.004869274404904607;
    _scalingDeCom[ 12 ] = 0.032333091610663785;
    _scalingDeCom[ 13 ] = -0.06698304907021778;
    _scalingDeCom[ 14 ] = -0.034574228416972504;
    _scalingDeCom[ 15 ] = 0.39712293362064416;
    _scalingDeCom[ 16 ] = 0.7565249878756971;
    _scalingDeCom[ 17 ] = 0.47534280601152273;
    _scalingDeCom[ 18 ] = -0.054040601387606135;
    _scalingDeCom[ 19 ] = -0.15959219218520598;
    _scalingDeCom[ 20 ] = 0.03072113906330156;
    _scalingDeCom[ 21 ] = 0.07803785290341991;
    _scalingDeCom[ 22 ] = -0.003510275068374009;
    _scalingDeCom[ 23 ] = -0.024952758046290123;
    _scalingDeCom[ 24 ] = 0.001359844742484172;
    _scalingDeCom[ 25 ] = 0.0069377611308027096;
    _scalingDeCom[ 26 ] = -0.00022211647621176323;
    _scalingDeCom[ 27 ] = -0.0013387206066921965;
    _scalingDeCom[ 28 ] = 3.656592483348223e-05;
    _scalingDeCom[ 29 ] = 0.00016545679579108483;
    _scalingDeCom[ 30 ] = -5.396483179315242e-06;
    _scalingDeCom[ 31 ] = -1.0797982104319795e-05;

    _buildOrthonormalSpace( );

  } // Symlet16

} // Symlet16