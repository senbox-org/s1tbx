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
package org.esa.nest.math.exceptions;

/**
 * Exception for not found objects in JWave
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 25.02.2014 20:51:39
 */
public class JWaveNotFound extends JWaveFailure {

  /**
	 * 
	 */
  private static final long serialVersionUID = -7215817371882941856L;

  /**
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 25.02.2014 20:52:08
   * @param message
   *          reason for the instance.
   */
  public JWaveNotFound( String message ) {
    super( message );
    _message = "JWave"; // overwrite
    _message += ": "; // separator
    _message += "Not found"; // Exception type
    _message += ": "; // separator
    _message += message; // add message
    _message += "\n"; // break line
  } // JWaveNotFound

} // JWaveNotFound