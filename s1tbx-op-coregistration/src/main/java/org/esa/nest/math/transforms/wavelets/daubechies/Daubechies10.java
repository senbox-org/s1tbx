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
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 20 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 00:41:08
 */
public class Daubechies10 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db10/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 00:41:08
   */
  public Daubechies10( ) {

    _name = "Daubechies 10"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 20; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -1.326420300235487e-05;
    _scalingDeCom[ 1 ] = 9.358867000108985e-05;
    _scalingDeCom[ 2 ] = -0.0001164668549943862;
    _scalingDeCom[ 3 ] = -0.0006858566950046825;
    _scalingDeCom[ 4 ] = 0.00199240529499085;
    _scalingDeCom[ 5 ] = 0.0013953517469940798;
    _scalingDeCom[ 6 ] = -0.010733175482979604;
    _scalingDeCom[ 7 ] = 0.0036065535669883944;
    _scalingDeCom[ 8 ] = 0.03321267405893324;
    _scalingDeCom[ 9 ] = -0.02945753682194567;
    _scalingDeCom[ 10 ] = -0.07139414716586077;
    _scalingDeCom[ 11 ] = 0.09305736460380659;
    _scalingDeCom[ 12 ] = 0.12736934033574265;
    _scalingDeCom[ 13 ] = -0.19594627437659665;
    _scalingDeCom[ 14 ] = -0.24984642432648865;
    _scalingDeCom[ 15 ] = 0.2811723436604265;
    _scalingDeCom[ 16 ] = 0.6884590394525921;
    _scalingDeCom[ 17 ] = 0.5272011889309198;
    _scalingDeCom[ 18 ] = 0.18817680007762133;
    _scalingDeCom[ 19 ] = 0.026670057900950818;

//    _scalingDeCom[ 0 ] = -1.326420300235487e-05;
//    _scalingDeCom[ 1 ] = -9.358867000108985e-05;
//    _scalingDeCom[ 2 ] = -0.0001164668549943862;
//    _scalingDeCom[ 3 ] = 0.0006858566950046825;
//    _scalingDeCom[ 4 ] = 0.00199240529499085;
//    _scalingDeCom[ 5 ] = -0.0013953517469940798;
//    _scalingDeCom[ 6 ] = -0.010733175482979604;
//    _scalingDeCom[ 7 ] = -0.0036065535669883944;
//    _scalingDeCom[ 8 ] = 0.03321267405893324;
//    _scalingDeCom[ 9 ] = 0.02945753682194567;
//    _scalingDeCom[ 10 ] = -0.07139414716586077;
//    _scalingDeCom[ 11 ] = -0.09305736460380659;
//    _scalingDeCom[ 12 ] = 0.12736934033574265;
//    _scalingDeCom[ 13 ] = 0.19594627437659665;
//    _scalingDeCom[ 14 ] = -0.24984642432648865;
//    _scalingDeCom[ 15 ] = -0.2811723436604265;
//    _scalingDeCom[ 16 ] = 0.6884590394525921;
//    _scalingDeCom[ 17 ] = -0.5272011889309198;
//    _scalingDeCom[ 18 ] = 0.18817680007762133;
//    _scalingDeCom[ 19 ] = -0.026670057900950818;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies10

} // Daubechies10
