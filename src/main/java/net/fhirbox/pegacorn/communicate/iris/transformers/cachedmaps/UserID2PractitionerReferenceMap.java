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
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.infinispan.manager.DefaultCacheManager;

import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.infinispan.Cache;

import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1> Map Pegacorn.RoomServer.UserID to Pegacorn.Practitioner.Identifier </h1>
 * <p>
 * This class is used to perform the transformation of a the RoomServer User's 
 * Identifier (Matrix::User) to the associate Practitioner Identifier 
 * (FHIR::Identifier)
 * <p>
 * To do this, a replicated cache is maintained within the Wildfly Application
 * Server (an Infinispan Replicated Cache) that spans all the Communicate
 * Application nodes within a given Site. The "user2practitioner_id_map" is used
 * to maintain the map.
 * <p>
 * If there is any issues with the FHIR::Identifier - and empty one is returned. 
 * It is assumed that a new (non-canonical) FHIR::Identifier will be created by 
 * what-ever function is calling this class. 
 * <p>
 * <b> Note: </b> the following configuration details need to be loaded into 
 * the Wildfly Application Server configuration file (standalone-ha.xml)
 * {@code 
            <cache-container name="pegacorn-communicate" default-cache="general" module="org.wildfly.clustering.server">
                <transport lock-timeout="15000" />
                <replicated-cache name="general">
                    <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
                </replicated-cache>
                <replicated-cache name="room2resource_id_map">
                    <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
                </replicated-cache>
                <replicated-cache name="user2practitioner_id_map">
                    <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
                </replicated-cache>>
            </cache-container>}
 * 
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020.01.15
 * 
 */

@Singleton
public class UserID2PractitionerReferenceMap {
    private static final Logger LOG = LoggerFactory.getLogger(UserID2PractitionerReferenceMap.class);
    // The JNDI reference to lookup within Wildfly for the Replicate-Cache cotainer
    // My pointed to the Replicated Cache Container
    @Inject
    private DefaultCacheManager theCommunicateCacheContainer;
    // My actual Replicated Cache
    private Cache<String, Identifier> theUserId2PractitionerIdMap;
    
    /**
     * The method is a post "Constructor" which initialises the replicated
     * cache service
     * 
     */
    @PostConstruct 
    public void start(){
        LOG.debug("start(): Entry");
        theUserId2PractitionerIdMap = this.theCommunicateCacheContainer.getCache("UserID2IdentifierReferenceMap", true);
        LOG.debug("start(): Exit, Got Cache -> " + theUserId2PractitionerIdMap.getName());
    }
    
    /**
     * 
     * @param pRoomServerUserId The RoomServer User Identifier
     * @return Identifier A FHIR::Identifier resource (see https://www.hl7.org/fhir/datatypes.html#Identifier)
     */
    public Identifier getFHIRResourceIdentifier( String pRoomServerUserId ){
        LOG.debug("getFHIRResourceIdentifier(): Parameter.pRoomServerUserId -> " + pRoomServerUserId);
        if(pRoomServerUserId == null)
        {
            LOG.debug("getFHIRResourceIdentifier(): No Identifier created, pRoomServerUserId == null");
            return(null); 
        }
        if( theUserId2PractitionerIdMap.isEmpty()){
            LOG.debug("getFHIRResourceIdentifier(): No Identifier created, User/Practitioner ID Map is empty");
            return(null);
        }
        Identifier localPractitionerIdentifier = theUserId2PractitionerIdMap.get(pRoomServerUserId);
        if( localPractitionerIdentifier != null ){
            LOG.debug("getFHIRResourceIdentifier(): Returning an Identifier -> " + localPractitionerIdentifier.toString());
            return( localPractitionerIdentifier );
        }
        LOG.debug("getFHIRResourceIdentifier(): No Identifier created, no User/Practitioner ID map entry found for RoomServer User ID");        
        return(null);
    }
    
    public void setFHIRResourceIdentifier( String pUserId, Identifier pPractitionerIdentifier ){
        LOG.debug("setFHIRResourceIdentifier(): Parameter.pRoomServerUserId -> " + pUserId + " Identifier -> " + pPractitionerIdentifier);
        if(pPractitionerIdentifier == null){
            return;
        }
        if( pUserId == null){
            return;
        }
        this.theUserId2PractitionerIdMap.put(pUserId, pPractitionerIdentifier);
        LOG.debug("setFHIRResourceIdentifier(): Identifier/UserId added to cachemap");                
    }    
}
