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
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.infinispan.manager.DefaultCacheManager;

import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.FHIRIdentifier2StringUtility;

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
 * <b> Note: </b> the following configuration details need to be loaded into the
 * Wildfly Application Server configuration file (standalone-ha.xml) * {@code
 * <cache-container name="pegacorn-communicate" default-cache="general" module="org.wildfly.clustering.server">
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
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020.01.15
 *
 */
@Singleton
public class MatrixUserID2PractitionerIDMap {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixUserID2PractitionerIDMap.class);
    // The JNDI reference to lookup within Wildfly for the Replicate-Cache cotainer
    // My pointed to the Replicated Cache Container
    @Inject
    private IrisSharedCacheAccessorBean theIrisCacheSetManager;
    
    private IrisCacheMapNameSet cacheName = new IrisCacheMapNameSet();
    
    // My actual Replicated Cache
    private Cache<String /* User Name */, String /* Practitioner Identifier */> theUserName2PractitionerIdMap;
    private Cache<String /* Practitioner Identifier */, String /* User Name */> thePractitionerId2UserNameMap;
    
    FhirContext r4FHIRContext; 
    IParser r4Parser;
    FHIRIdentifier2StringUtility mySimpleIdentifierConverter;

    public MatrixUserID2PractitionerIDMap(){
        r4FHIRContext = FhirContext.forR4();
        r4Parser = r4FHIRContext.newJsonParser();
        this.mySimpleIdentifierConverter = new FHIRIdentifier2StringUtility();
    }
    /**
     * The method is a post "Constructor" which initialises the replicated cache
     * service
     *
     */
    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        this.theUserName2PractitionerIdMap = this.theIrisCacheSetManager.getIrisSharedCache(cacheName.getMatrixUserName2FHIRPractitionerIdMap());
        this.thePractitionerId2UserNameMap = this.theIrisCacheSetManager.getIrisSharedCache(cacheName.getFHIRPractitionerId2MatrixUserNameMap());
        LOG.debug("start(): Exit, Got Cache -> {}, {}", theUserName2PractitionerIdMap.getName(), thePractitionerId2UserNameMap.getName());
    }

    /**
     *
     * @param userName The RoomServer User Identifier
     * @return Identifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     */
    public Identifier getPractitionerIDFromUserName(String userName) {
        LOG.debug("getPractitionerID(): Entry");
        if (userName == null) {
            LOG.debug("getPractitionerID(): Exit, userName == null");
        }
        LOG.debug("getPractitionerID(): username -> {}", userName);
        if (this.theUserName2PractitionerIdMap.isEmpty()) {
            LOG.debug("getPractitionerID(): No Identifier found, User/Practitioner ID Map is empty");
            return (null);
        }
        String practitionerIDString = this.theUserName2PractitionerIdMap.get(userName);
        if (practitionerIDString != null) {
            LOG.debug("getPractitionerID(): Returning an Identifier -> {}", practitionerIDString);
            Identifier practitionerIdentifier = this.mySimpleIdentifierConverter.fromString2Identifier(practitionerIDString);
            return (practitionerIdentifier);
        }
        LOG.debug("getPractitionerID(): No Identifier found, no User/Practitioner ID map entry found for RoomServer User Name: {}", userName);
        return (null);
    }

    /**
     *
     * @param practitionerIdentifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     * @return String The name on the RoomServer of the Practitioner
     * 
     */
    public String getUserNameFromPractitionerIdentifier(Identifier practitionerIdentifier) {
        LOG.debug("getUserName(): Entry");
        if (practitionerIdentifier == null) {
            LOG.debug("getUserName(): Exit, practitionerIdentifier == null");
        }
        LOG.trace("getUserName(): searching for user name for Identifier -> {}", practitionerIdentifier);
        if (this.theUserName2PractitionerIdMap.isEmpty()) {
            LOG.debug("getUserName(): No Identifier found, User/Practitioner ID Map is empty");
            return (null);
        }
        String practitierIDString = this.mySimpleIdentifierConverter.fromIdentifier2String(practitionerIdentifier);
        String userName = this.thePractitionerId2UserNameMap.get(practitierIDString);
        if (userName != null) {
            LOG.debug("getUserName(): Returning a User Name -> {}", userName);
            return (userName);
        }
        LOG.debug("getUserName(): No Name found, no User/Practitioner ID map entry found for Practitioner Identifier : {}", practitierIDString);
        return (null);
    }

    /**
     *
     * @param userName The RoomServer User Name
     * @param practitionerIdentifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     * 
     */
    public void setPractitionerIDForUserName(String userName, Identifier practitionerIdentifier) {
        LOG.debug("setPractitionerIDForUserName(): Entry");
        if (userName == null) {
            LOG.debug("setPractitionerIDForUserName(): No entry create in User Name / PractitionerId Map, userName == null");
            return;
        }
        if (practitionerIdentifier == null) {
            LOG.debug("setPractitionerIDForUserName(): No entry create in User Name / PractitionerId Map, practitionerIdentifier == null");
        }
        String practitionerIDString = this.mySimpleIdentifierConverter.fromIdentifier2String(practitionerIdentifier);
        LOG.trace("setPractitionerIDForUserName(): Adding entry to map: userName -> " + userName + " Identifier -> " + practitionerIDString);
        this.theUserName2PractitionerIdMap.put(userName, practitionerIDString);
        this.thePractitionerId2UserNameMap.put(practitionerIDString, userName);
        LOG.debug("setPractitionerIDForUserName(): User Name / Identifier added to cachemap");
    }

    /**
     *
     * @param practitionerIdentifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     * @param userName The RoomServer User Name
     * 
     */
    public void setUserNameForPractitionerID( Identifier practitionerIdentifier, String userName ) {
        LOG.debug("setUserNameForPractitionerID(): Entry");
        if (userName == null) {
            LOG.debug("setUserNameForPractitionerID(): No entry create in User Name / PractitionerId Map, userName == null");
            return;
        }
        if (practitionerIdentifier == null) {
            LOG.debug("setUserNameForPractitionerID(): No entry create in User Name / PractitionerId Map, practitionerIdentifier == null");
        }
        String practitionerIDString = this.mySimpleIdentifierConverter.fromIdentifier2String(practitionerIdentifier);
        LOG.trace("setPractitionerID(): Adding entry to map: userName -> {}, Identifier -> {}", userName, practitionerIDString);
        this.setPractitionerIDForUserName(userName, practitionerIdentifier);
        LOG.debug("setPractitionerID(): User Name / Identifier added to cachemap");
    }
}
