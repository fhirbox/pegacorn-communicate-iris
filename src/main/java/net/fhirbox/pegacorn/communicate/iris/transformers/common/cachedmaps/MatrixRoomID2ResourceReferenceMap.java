/* 
 * Copyright 2020 ACT Health.
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
package net.fhirbox.pegacorn.communicate.iris.transformers.common.cachedmaps;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.manager.CacheContainer;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import net.fhirbox.pegacorn.communicate.iris.utilities.SharedCache;
import net.fhirbox.pegacorn.communicate.iris.utilities.SharedCacheBean;

import org.hl7.fhir.r4.model.Reference;
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
    DefaultCacheManager theCommunicateCacheContainer;

    // My actual Replicated Cache (UserName, UserToken) 
    private Cache<String /* RoomID */, Reference /* FHIR Resource */> theRoomID2FHIRResourceMap;
    private Cache<Reference /* FHIR Resource */, String /* RoomID */> theFHIRResource2RoomIDMap;

    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        this.theRoomID2FHIRResourceMap = this.theCommunicateCacheContainer.getCache("RoomID2ResourceReferenceMap", true);
        this.theFHIRResource2RoomIDMap = this.theCommunicateCacheContainer.getCache("ResourceReference2RoomIDMap", true);
        LOG.debug("start(): Exit, Got Cache -> {}, and --> {}", this.theRoomID2FHIRResourceMap.getName(), this.theCommunicateCacheContainer.getName());
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
        Reference resourceReference = this.theRoomID2FHIRResourceMap.get(roomID);
        if (resourceReference != null) {
            LOG.debug("getFHIRResourceReferenceFromRoomID(): Got Resource Reference {} for Room Name {}", resourceReference, roomID);
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
        LOG.trace("getRoomIDFromResourceReference(): getting Room ID for Resource Reference {}", resourceReference);
        String roomID = this.theFHIRResource2RoomIDMap.get(resourceReference);
        if (roomID != null) {
            LOG.debug("getRoomIDFromResourceReference(): Got Room Name {} for Resource Reference {}", roomID, resourceReference);
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
        LOG.trace("setResourceReferenceForRoomID(): adding roomID = {} and resourceReference = {} to the RoomID2ResourceReferenceMap", roomID, resourceReference);
        this.theRoomID2FHIRResourceMap.put(roomID, resourceReference);
        LOG.trace("setResourceReferenceForRoomID(): adding resourceReference = {} and roomID = {} to the ResourceReference2RoomIDMap", resourceReference, roomID);
        this.theFHIRResource2RoomIDMap.put(resourceReference, roomID);
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
