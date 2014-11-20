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
package org.esa.nest.math.transforms.wavelets;

/**
 * Basic class for one wavelet keeping coefficients of the wavelet function, the
 * scaling function, the base wavelength, the forward transform method, and the
 * reverse transform method.
 * 
 * @date 10.02.2010 08:54:48
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public abstract class Wavelet implements WaveletInterface {

  /**
   * The name of the wavelet.
   */
  protected String _name;

  /**
   * The wavelength of the base or so called mother wavelet and its matching
   * scaling function.
   */
  protected int _motherWavelength;

  /**
   * The minimal wavelength of a signal that can be transformed
   */
  protected int _transformWavelength;

  /**
   * The coefficients of the mother scaling (low pass filter) for decomposition.
   */
  protected double[ ] _scalingDeCom;

  /**
   * The coefficients of the mother wavelet (high pass filter) for
   * decomposition.
   */
  protected double[ ] _waveletDeCom;

  /**
   * The coefficients of the mother scaling (low pass filter) for
   * reconstruction.
   */
  protected double[ ] _scalingReCon;

  /**
   * The coefficients of the mother wavelet (high pass filter) for
   * reconstruction.
   */
  protected double[ ] _waveletReCon;

  /**
   * Constructor; predefine members to default values or null!
   * 
   * @date 15.02.2014 22:16:27
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public Wavelet( ) {

    _name = null;

    _motherWavelength = 0;

    _transformWavelength = 0;

    _scalingDeCom = null;

    _waveletDeCom = null;

    _scalingReCon = null;

    _waveletReCon = null;

  } // Wavelet

  /**
   * The method builds form the scaling (low pass) coefficients for
   * decomposition of a filter, the matching coefficients for the wavelet (high
   * pass) for decomposition, for the scaling (low pass) for reconstruction, and
   * for the wavelet (high pass) of reconstruction. This method should be called
   * in the constructor of an orthonormal filter directly after defining the
   * orthonormal coefficients of the scaling (low pass) for decomposition!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 13:19:27
   */
  protected void _buildOrthonormalSpace( ) {

    // building wavelet as orthogonal (orthonormal) space from
    // scaling coefficients (low pass filter). Have a look into
    // Alfred Haar's wavelet or the Daubechies Wavelet with 2
    // vanishing moments for understanding what is done here. ;-)
    _waveletDeCom = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ )
      if( i % 2 == 0 )
        _waveletDeCom[ i ] = _scalingDeCom[ ( _motherWavelength - 1 ) - i ];
      else
        _waveletDeCom[ i ] = -_scalingDeCom[ ( _motherWavelength - 1 ) - i ];

    // Copy to reconstruction filters due to orthogonality (orthonormality)!
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {
      _scalingReCon[ i ] = _scalingDeCom[ i ];
      _waveletReCon[ i ] = _waveletDeCom[ i ];
    } // i

  } // _buildOrthonormalSpace

  /**
   * The method builds form the scaling (low pass) coefficients for
   * decomposition and wavelet (high pass) coefficients for decomposition of a
   * filter, the matching coefficients for the scaling (low pass) for
   * reconstruction, and for the wavelet (high pass) of reconstruction. This
   * method should be called in the constructor of an biorthogonal
   * (biorthonormal) filter directly after defining the orthonormal coefficients
   * of the scaling (low pass) and wavelet (high pass) for decomposition!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:04:44
   */
  protected void _buildBiOrthonormalSpace( ) {

    // building wavelet and scaling function for reconstruction
    // as orthogonal (orthonormal) spaces from scaling and wavelet
    // of decomposition. ;-)
    _scalingReCon = new double[ _motherWavelength ];
    _waveletReCon = new double[ _motherWavelength ];
    for( int i = 0; i < _motherWavelength; i++ ) {
      if( i % 2 == 0 ) {
        _scalingReCon[ i ] = _waveletDeCom[ ( _motherWavelength - 1 ) - i ];
        _waveletReCon[ i ] = _scalingDeCom[ ( _motherWavelength - 1 ) - i ];
      } else {
        _scalingReCon[ i ] = -_waveletDeCom[ ( _motherWavelength - 1 ) - i ];
        _waveletReCon[ i ] = -_scalingDeCom[ ( _motherWavelength - 1 ) - i ];
      } // if
    } // i

  } // _buildBiOrthonormalSpace

  /*
   * Returns a String keeping the name of the current Wavelet.
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 11:02:31
   * @return String keeping the name of the wavelet
   */
  public String getName( ) {

    return _name;

  } // getName

  /**
   * Performs the forward transform for the given array from time domain to
   * Hilbert domain and returns a new array of the same size keeping
   * coefficients of Hilbert domain and should be of length 2 to the power of p
   * -- length = 2^p where p is a positive integer.
   * 
   * @date 10.02.2010 08:18:02
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrTime
   *          array keeping time domain coefficients
   * @param arrTimeLength
   *          is necessary, due to working only on a part of arrTime not on the
   *          full length of arrTime!
   * @return coefficients represented by frequency domain
   */
  public double[ ] forward( double[ ] arrTime, int arrTimeLength ) {

    double[ ] arrHilb = new double[ arrTimeLength ];

    int h = arrHilb.length >> 1; // .. -> 8 -> 4 -> 2 .. shrinks in each step by half wavelength

    for( int i = 0; i < h; i++ ) {

      arrHilb[ i ] = arrHilb[ i + h ] = 0.; // set to zero before sum up

      for( int j = 0; j < _motherWavelength; j++ ) {

        int k = ( i * 2 ) + j; // int k = ( i << 1 ) + j;

        while( k >= arrHilb.length )
          k -= arrHilb.length; // circulate over arrays if scaling and wavelet are are larger

        arrHilb[ i ] += arrTime[ k ] * _scalingDeCom[ j ]; // low pass filter for the energy (approximation)
        arrHilb[ i + h ] += arrTime[ k ] * _waveletDeCom[ j ]; // high pass filter for the details

      } // Sorting each step in patterns of: { scaling coefficients | wavelet coefficients }

    } // h = 2^(p-1) | p = { 1, 2, .., N } .. shrinks in each step by half wavelength 

    return arrHilb;

  } // forward

  /**
   * Performs the reverse transform for the given array from Hilbert domain to
   * time domain and returns a new array of the same size keeping coefficients
   * of time domain and should be of length 2 to the power of p -- length = 2^p
   * where p is a positive integer.
   * 
   * @date 10.02.2010 08:19:24
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrHilb
   *          array keeping frequency domain coefficients
   * @param arrHilbLength
   *          is necessary, due to working only on a part of arrHilb not on the
   *          full length of arrHilb!
   * @return coefficients represented by time domain
   */
  public double[ ] reverse( double[ ] arrHilb, int arrHilbLength ) {

    double[ ] arrTime = new double[ arrHilbLength ];

    for( int i = 0; i < arrTime.length; i++ )
      arrTime[ i ] = 0.;

    int h = arrTime.length >> 1; // .. -> 8 -> 4 -> 2 .. shrinks in each step by half wavelength

    for( int i = 0; i < h; i++ ) {

      for( int j = 0; j < _motherWavelength; j++ ) {

        int k = ( i * 2 ) + j; // int k = ( i << 1 ) + j;

        while( k >= arrTime.length )
          k -= arrTime.length; // circulate over arrays if scaling and wavelet are larger

        // adding up energy from low pass (approximation) and details from high pass filter
        arrTime[ k ] +=
            ( arrHilb[ i ] * _scalingReCon[ j ] )
                + ( arrHilb[ i + h ] * _waveletReCon[ j ] ); // looks better with brackets

      } // Reconstruction from patterns of: { scaling coefficients | wavelet coefficients }

    } // h = 2^(p-1) | p = { 1, 2, .., N } .. shrink in each step by half wavelength 

    return arrTime;

  } // reverse

  /**
   * Returns the wavelength of the so called mother wavelet or scaling function.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:06:12
   * @return the minimal wavelength for the mother wavelet
   */
  public int getMotherWavelength( ) {

    return _motherWavelength;

  } // getMotherWavelength

  /**
   * Returns the minimal necessary wavelength for a signal that can be
   * transformed by this wavelet.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:08:43
   * @return integer representing minimal wavelength of the input signal that
   *         should be transformed by this wavelet.
   */
  public int getTransformWavelength( ) {

    return _transformWavelength;

  } // getTransformWavelength

  /**
   * Returns a copy of the scaling (low pass filter) coefficients of
   * decomposition.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2010 22:11:42
   * @return array of length of the mother wavelet wavelength keeping the
   *         decomposition low pass filter coefficients
   */
  public double[ ] getScalingDeComposition( ) {

    double[ ] scalingDeCom = new double[ _scalingDeCom.length ];

    for( int i = 0; i < _scalingDeCom.length; i++ )
      scalingDeCom[ i ] = _scalingDeCom[ i ];

    return scalingDeCom;

  } // getScalingDeCom  

  /**
   * Returns a copy of the wavelet (high pass filter) coefficients of
   * decomposition.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:11:25
   * @return array of length of the mother wavelet wavelength keeping the
   *         decomposition high pass filter coefficients
   */
  public double[ ] getWaveletDeComposition( ) {

    double[ ] waveletDeCom = new double[ _waveletDeCom.length ];

    for( int i = 0; i < _waveletDeCom.length; i++ )
      waveletDeCom[ i ] = _waveletDeCom[ i ];

    return waveletDeCom;

  } // getWaveletDeCom

  /**
   * Returns a copy of the scaling (low pass filter) coefficients of
   * reconstruction.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 10:35:11
   * @return array of length of the mother wavelet wavelength keeping the
   *         reconstruction low pass filter coefficients
   */
  public double[ ] getScalingReConstruction( ) {

    double[ ] scalingReCon = new double[ _scalingReCon.length ];

    for( int i = 0; i < _scalingReCon.length; i++ )
      scalingReCon[ i ] = _scalingReCon[ i ];

    return scalingReCon;

  } // getScalingReCon

  /**
   * Returns a copy of the wavelet (high pass filter) coefficients of
   * reconstruction.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 10:35:09
   * @return array of length of the mother wavelet wavelength keeping the
   *         reconstruction high pass filter coefficients
   */
  public double[ ] getWaveletReConstruction( ) {

    double[ ] waveletReCon = new double[ _waveletReCon.length ];

    for( int i = 0; i < _waveletReCon.length; i++ )
      waveletReCon[ i ] = _waveletReCon[ i ];

    return waveletReCon;

  } // getWaveletReCon

} // Wavelet
