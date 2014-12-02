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
 * Symlet20 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 13:47:56
 */
public class Symlet20 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym20/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 13:47:56
   */
  public Symlet20( ) {

    _name = "Symlet 20"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 40; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 3.695537474835221e-07;
    _scalingDeCom[ 1 ] = -1.9015675890554106e-07;
    _scalingDeCom[ 2 ] = -7.919361411976999e-06;
    _scalingDeCom[ 3 ] = 3.025666062736966e-06;
    _scalingDeCom[ 4 ] = 7.992967835772481e-05;
    _scalingDeCom[ 5 ] = -1.928412300645204e-05;
    _scalingDeCom[ 6 ] = -0.0004947310915672655;
    _scalingDeCom[ 7 ] = 7.215991188074035e-05;
    _scalingDeCom[ 8 ] = 0.002088994708190198;
    _scalingDeCom[ 9 ] = -0.0003052628317957281;
    _scalingDeCom[ 10 ] = -0.006606585799088861;
    _scalingDeCom[ 11 ] = 0.0014230873594621453;
    _scalingDeCom[ 12 ] = 0.01700404902339034;
    _scalingDeCom[ 13 ] = -0.003313857383623359;
    _scalingDeCom[ 14 ] = -0.031629437144957966;
    _scalingDeCom[ 15 ] = 0.008123228356009682;
    _scalingDeCom[ 16 ] = 0.025579349509413946;
    _scalingDeCom[ 17 ] = -0.07899434492839816;
    _scalingDeCom[ 18 ] = -0.02981936888033373;
    _scalingDeCom[ 19 ] = 0.4058314443484506;
    _scalingDeCom[ 20 ] = 0.75116272842273;
    _scalingDeCom[ 21 ] = 0.47199147510148703;
    _scalingDeCom[ 22 ] = -0.0510883429210674;
    _scalingDeCom[ 23 ] = -0.16057829841525254;
    _scalingDeCom[ 24 ] = 0.03625095165393308;
    _scalingDeCom[ 25 ] = 0.08891966802819956;
    _scalingDeCom[ 26 ] = -0.0068437019650692274;
    _scalingDeCom[ 27 ] = -0.035373336756604236;
    _scalingDeCom[ 28 ] = 0.0019385970672402002;
    _scalingDeCom[ 29 ] = 0.012157040948785737;
    _scalingDeCom[ 30 ] = -0.0006111263857992088;
    _scalingDeCom[ 31 ] = -0.0034716478028440734;
    _scalingDeCom[ 32 ] = 0.0001254409172306726;
    _scalingDeCom[ 33 ] = 0.0007476108597820572;
    _scalingDeCom[ 34 ] = -2.6615550335516086e-05;
    _scalingDeCom[ 35 ] = -0.00011739133516291466;
    _scalingDeCom[ 36 ] = 4.525422209151636e-06;
    _scalingDeCom[ 37 ] = 1.22872527779612e-05;
    _scalingDeCom[ 38 ] = -3.2567026420174407e-07;
    _scalingDeCom[ 39 ] = -6.329129044776395e-07;

    _buildOrthonormalSpace( );

  } // Symlet20(

} // Symlet20(
