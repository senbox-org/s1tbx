/**
 * Cohen Daubechies Feauveau (CDF) 9/7 Wavelet
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 13:52:41 
 *
 * CDF97.java
 */
package org.esa.nest.math.transforms.wavelets.other;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Cohen Daubechies Feauveau (CDF) 9/7 Wavelet
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 17.08.2014 13:52:41
 */
public class CDF97 extends Wavelet {

  /**
   * Cohen Daubechies Feauveau (CDF) 9/7 Wavelet. THIS WAVELET IS NOT WORKING -
   * DUE TO ODD NUMBER COEFFICIENTS!!!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 13:52:41
   */
  public CDF97( ) {

    _name = "Cohen Daubechies Feauveau (CDF) 9/7"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 9; // wavelength of mother wavelet

    //    double sqrt2 = Math.sqrt( 2. );

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.026748757411; //
    _scalingDeCom[ 1 ] = -0.016864118443; //
    _scalingDeCom[ 2 ] = -0.078223266529; //
    _scalingDeCom[ 3 ] = 0.266864118443; //
    _scalingDeCom[ 4 ] = 0.602949018236; //
    _scalingDeCom[ 5 ] = 0.266864118443; //
    _scalingDeCom[ 6 ] = -0.078223266529; //
    _scalingDeCom[ 7 ] = -0.016864118443; //
    _scalingDeCom[ 8 ] = 0.026748757411; //

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.; // 
    _waveletDeCom[ 1 ] = 0.091271763114; // 
    _waveletDeCom[ 2 ] = -0.057543526229; // 
    _waveletDeCom[ 3 ] = -0.591271763114; // 
    _waveletDeCom[ 4 ] = 1.11508705; // 
    _waveletDeCom[ 5 ] = -0.591271763114; // 
    _waveletDeCom[ 6 ] = -0.057543526229; // 
    _waveletDeCom[ 7 ] = 0.091271763114; // 
    _waveletDeCom[ 8 ] = 0.; // 

    // Copy to reconstruction filters due to orthogonality!
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {
      _scalingReCon[ i ] = _scalingDeCom[ i ];
      _waveletReCon[ i ] = _waveletDeCom[ i ];
    } // i

  } // CDF97

} // CDF97
