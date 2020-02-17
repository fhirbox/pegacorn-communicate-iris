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
package net.fhirbox.pegacorn.communicate.iris.transformers.cachedmaps;

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
 * <b> Note: </b> the following configuration details need to be loaded into the
 * Wildfly Application Server configuration file (i.e. standalone-ha.xml)  * {@code 
            <cache-container name="pegacorn-communicate" default-cache="general" module="org.wildfly.clustering.server">
 * <transport lock-timeout="15000" />
 * <replicated-cache name="general">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>
 * <replicated-cache name="room2resource_id_map">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>
 * <replicated-cache name="user2practitioner_id_map">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>>
 * </cache-container>}
 *
 * @author ACT Health (Mark A. Hunter)
 *
 */
@Singleton
public class RoomID2ResourceReferenceMap {

    private static final Logger LOG = LoggerFactory.getLogger(RoomID2ResourceReferenceMap.class);
    
    @Inject
    DefaultCacheManager theCommunicateCacheContainer;

    private Cache<String, Reference> theRoomIdMap;

    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        theRoomIdMap = theCommunicateCacheContainer.getCache("RoomID2ResourceReferenceMap", true);
        LOG.debug("start(): Exit, Got Cache -> " + theRoomIdMap.getName());
    }

    public Reference getFHIRResourceReference(String pRoomId) {
        if (pRoomId == null) {
            return (null);
        }
        Reference localResourceReference = theRoomIdMap.get(pRoomId);
        if (localResourceReference != null) {
            return (localResourceReference);
        }
        return (null);
    }

    public void setFHIRResourceReference(String pRoomId, Reference pResourceReference) {
        if (pResourceReference == null) {
            return;
        }
        if (pRoomId == null) {
            return;
        }
        this.theRoomIdMap.put(pRoomId, pResourceReference);
    }
}
