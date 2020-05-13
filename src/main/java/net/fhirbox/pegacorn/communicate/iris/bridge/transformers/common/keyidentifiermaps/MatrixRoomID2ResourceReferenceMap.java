/* 
 * The MIT License
 *
 * Copyright 2020 ACT Health.
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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps;

import net.fhirbox.pegacorn.communicate.iris.utilities.IrisSharedCacheAccessorBean;
import net.fhirbox.pegacorn.communicate.iris.utilities.IrisCacheMapNameSet;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.manager.CacheContainer;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.FHIRIdentifier2StringUtility;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.FHIRReference2StringUtility;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACT Health (Mark A. Hunter)
 *
 */
@Singleton
public class MatrixRoomID2ResourceReferenceMap {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomID2ResourceReferenceMap.class);

    @Inject
    private IrisSharedCacheAccessorBean theIrisCacheSetManager;

    private IrisCacheMapNameSet cacheName = new IrisCacheMapNameSet();
    
    // My actual Replicated Cache (UserName, UserToken) 
    private Cache<String /* RoomID */, String /* FHIR Resource */> theRoomID2FHIRResourceMap;
    private Cache<String /* FHIR Resource */, String /* RoomID */> theFHIRResource2RoomIDMap;
    
        FhirContext r4FHIRContext; 
    IParser r4Parser;
    FHIRReference2StringUtility mySimpleReferenceConverter;

    public MatrixRoomID2ResourceReferenceMap(){
        r4FHIRContext = FhirContext.forR4();
        r4Parser = r4FHIRContext.newJsonParser();
        this.mySimpleReferenceConverter = new FHIRReference2StringUtility();
    }

    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        this.theRoomID2FHIRResourceMap = this.theIrisCacheSetManager.getIrisSharedCache(cacheName.getMatrixRoomID2FHIRResourceReferenceMap());
        this.theFHIRResource2RoomIDMap = this.theIrisCacheSetManager.getIrisSharedCache(cacheName.getFHIRResourceReference2MatrixRoomIDMap());
        LOG.debug("start(): Exit, Got Cache -> {}, and --> {}", this.theRoomID2FHIRResourceMap.getName(), this.theFHIRResource2RoomIDMap.getName());
    }

    /**
     *
     * @param roomID The name on the associated Room within the RoomoServer
     * @return A FHIR::Reference resource (see
     * https://www.hl7.org/fhir/datatypes.html#Resource)
     *
     */
    public Reference getFHIRResourceReferenceFromRoomID(String roomID) {
        LOG.debug("getFHIRResourceReferenceFromRoomID(): Entry");
        if (roomID == null) {
            LOG.debug("getFHIRResourceReferenceFromRoomID(): Exit, {}", roomID);
            return (null);
        }
        LOG.trace("getFHIRResourceReferenceFromRoomID(): getting Resource Reference for Room with Name : {}", roomID);
        String resourceReferenceString = this.theRoomID2FHIRResourceMap.get(roomID);
        if (resourceReferenceString != null) {
            Reference resourceReference = this.mySimpleReferenceConverter.fromString2Reference(resourceReferenceString);
            LOG.debug("getFHIRResourceReferenceFromRoomID(): Got Resource Reference {} for Room Name {}", resourceReferenceString, roomID);
            return (resourceReference);
        }
        LOG.debug("getFHIRResourceReferenceFromRoomID(): Could not find Resource Reference");
        return (null);
    }

    /**
     *
     * @param resourceReference A FHIR::Reference resource (see
     * https://www.hl7.org/fhir/datatypes.html#Resource)
     * @return String The name on the associated Room within the RoomoServer
     *
     */
    public String getRoomIDFromResourceReference(Reference resourceReference) {
        LOG.debug("getRoomIDFromResourceReference(): Entry");
        if (resourceReference == null) {
            LOG.debug("getRoomIDFromResourceReference(): Exit, {}", resourceReference);
            return (null);
        }
        String resourceReferenceString = this.mySimpleReferenceConverter.fromReference2String(resourceReference);
        if(resourceReferenceString == null ){
            LOG.debug("getRoomIDFromResourceReference(): Exit, couldn't convert the reference to a JSON string --> {}", resourceReference);
            return(null);
        }
        LOG.trace("getRoomIDFromResourceReference(): getting Room ID for Resource Reference {}", resourceReference);
        String roomID = this.theFHIRResource2RoomIDMap.get(resourceReferenceString);
        if (roomID != null) {
            LOG.debug("getRoomIDFromResourceReference(): Got Room Name {} for Resource Reference {}", roomID, resourceReferenceString);
            return (roomID);
        }
        LOG.debug("getRoomIDFromResourceReference(): Could not find Room ID");
        return (null);
    }

    public void setResourceReferenceForRoomID(String roomID, Reference resourceReference) {
        LOG.debug("setResourceReferenceForRoomID(): Entry");
        if (roomID == null) {
            LOG.debug("setResourceReferenceForRoomID(): Exit, roomID == null");
            return;
        }
        if (resourceReference == null) {
            LOG.debug("setResourceReferenceForRoomID(): Exit, resourceReference == null");
            return;
        }
        String resourceReferenceString = this.mySimpleReferenceConverter.fromReference2String(resourceReference);
        if( resourceReferenceString == null){
            return;
        }
        LOG.trace("setResourceReferenceForRoomID(): adding roomID = {} and resourceReference = {} to the RoomID2ResourceReferenceMap", roomID, resourceReferenceString);
        this.theRoomID2FHIRResourceMap.put(roomID, resourceReferenceString);
        LOG.trace("setResourceReferenceForRoomID(): adding resourceReference = {} and roomID = {} to the ResourceReference2RoomIDMap", resourceReferenceString, roomID);
        this.theFHIRResource2RoomIDMap.put(resourceReferenceString, roomID);
    }
    
    public void setRoomIDForResourceReference(Reference resourceReference, String roomID){
        LOG.debug("setRoomIDForResourceReference(), Entry");
        if (roomID == null) {
            LOG.debug("setRoomIDForResourceReference(): Exit, roomID == null");
            return;
        }
        if (resourceReference == null) {
            LOG.debug("setRoomIDForResourceReference(): Exit, resourceReference == null");
            return;
        }     
        setResourceReferenceForRoomID(roomID, resourceReference);
        LOG.debug("setRoomIDForResourceReference(): Exit");
    }
}
