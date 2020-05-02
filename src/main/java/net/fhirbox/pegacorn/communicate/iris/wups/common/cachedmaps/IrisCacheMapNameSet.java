/*
 * The MIT License
 *
 * Copyright 2020 MAHun.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author MAHun
 */
public class IrisCacheMapNameSet {
    private static final String IRIS_MATRIX_ROOM_ID_2_ROOM_NAME_MAP = "Pegacorn.Communicate.Iris.MatrixRoomID2MatrixRoomNameMap";
    private static final String IRIS_MATRIX_ROOM_ID_2_FHIR_RESOURCE_REFERENCE_MAP = "Pegacorn.Communicate.Iris.MatrixRoomID2FHIRResourceReferenceMap";
    private static final String IRIS_FHIR_RESOURCE_REFERENCE_2_MATRIX_ROOM_ID_MAP = "Pegacorn.Communicate.Iris.FHIRResourceReference2MatrixRoomIDMap";
    private static final String IRIS_MATRIX_USER_ID_2_MATRIX_TOKEN_MAP = "Pegacorn.Communicate.Iris.MatrixUser2TokenMap";
    private static final String IRIS_MATRIX_USER_TOKEN_2_MATRIX_USER_ID_MAP = "Pegacorn.Communicate.Iris.MatrixToken2UserMap";
    private static final String IRIS_MATRIX_USER_NAME_2_FHIR_PRACTITIONER_ID_MAP = "Pegacorn.Communicate.Iris.UserName2PractitionerIdMap";
    private static final String IRIS_FHIR_PRACTITIONER_ID_2_MATRIX_USER_NAME_MAP = "Pegacorn.Communicate.Iris.PractitionerId2UserNameMap";

    public String getMatrixRoomID2MatrixRoomMapName(){
        return(IRIS_MATRIX_ROOM_ID_2_ROOM_NAME_MAP);
    }

    public String getMatrixRoomID2FHIRResourceReferenceMap(){
        return(IRIS_MATRIX_ROOM_ID_2_FHIR_RESOURCE_REFERENCE_MAP);
    }

    public String getFHIRResourceReference2MatrixRoomIDMap(){
        return(IRIS_FHIR_RESOURCE_REFERENCE_2_MATRIX_ROOM_ID_MAP);
    }
    
    public String getMatrixUser2TokenMap(){
        return(IRIS_MATRIX_USER_ID_2_MATRIX_TOKEN_MAP);
    }
    
    public String getMatrixToken2MatrixUserMap(){
        return(IRIS_MATRIX_USER_TOKEN_2_MATRIX_USER_ID_MAP);
    }
    
    public String getMatrixUserName2FHIRPractitionerIdMap(){
        return(IRIS_MATRIX_USER_NAME_2_FHIR_PRACTITIONER_ID_MAP);
    }
    
    public String getFHIRPractitionerId2MatrixUserNameMap(){
        return(IRIS_FHIR_PRACTITIONER_ID_2_MATRIX_USER_NAME_MAP);
    }
}
