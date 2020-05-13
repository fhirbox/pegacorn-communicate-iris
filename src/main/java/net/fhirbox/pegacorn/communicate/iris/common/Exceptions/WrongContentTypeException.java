/* 
 * Copyright 2020 Mark A. Hunter (ACT Health).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.fhirbox.pegacorn.communicate.iris.common.Exceptions;

/**
 *
 * @author mhunter
 */
public class WrongContentTypeException extends Exception {

    /**
     * Creates a new instance of <code>TransformError</code> without detail
     * message.
     */
    public WrongContentTypeException() {
    }
    
    /**
     * Constructs an instance of <code>TransformError</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public WrongContentTypeException(String msg) {
        super(msg);
    }    

    /**
     * Constructs an instance of <code>TransformError</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public WrongContentTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
