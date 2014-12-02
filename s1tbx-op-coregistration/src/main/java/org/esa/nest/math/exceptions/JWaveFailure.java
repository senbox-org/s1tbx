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
 * Marking failures for this package; failures that are recoverable
 * 
 * @date 19.05.2009 09:26:22
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class JWaveFailure extends JWaveException {
  
  /**
   * Generated serial ID for this failure
   * 
   * @date 19.05.2009 09:27:18
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  private static final long serialVersionUID = 5471588833755939370L;
  
  /**
   * Constructor taking a failure message
   * 
   * @date 19.05.2009 09:26:22
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param message
   *          the stored failure message for this exception
   */
  public JWaveFailure( String message ) {
    super( message );
	_message = "JWave"; // overwrite
	_message += ": "; // separator
	_message += "Failure"; // Exception type
	_message += ": "; // separator
	_message += message; // add message
	_message += "\n"; // break line
  } // TransformFailure
  
} // class
