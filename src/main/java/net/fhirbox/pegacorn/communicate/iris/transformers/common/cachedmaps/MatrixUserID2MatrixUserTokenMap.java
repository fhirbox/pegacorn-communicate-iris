/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.common.cachedmaps;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class MatrixUserID2MatrixUserTokenMap {

    private static final Logger LOG = LoggerFactory.getLogger(MatrxUserID2PractitionerIDMap.class);

    // The JNDI reference to lookup within Wildfly for the Replicate-Cache cotainer
    // My pointed to the Replicated Cache Container
    @Inject
    private DefaultCacheManager theCommunicateCacheContainer;
    // My actual Replicated Cache (UserName, UserToken) 
    private Cache<String /* UserName */, String /* UserToken */> theMatrixUser2TokenMap;
    private Cache<String /* UserToken */, String /* UserName */> theMatrixToken2UserMap;

    /**
     * The method is a post "Constructor" which initialises the replicated cache
     * service
     *
     */
    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        this.theMatrixUser2TokenMap = this.theCommunicateCacheContainer.getCache("UserName2UserTokenReferenceMap", true);
        this.theMatrixToken2UserMap = this.theCommunicateCacheContainer.getCache("UserToken2UserNameReferenceMap", true);
        LOG.debug("start(): Exit, Got Cache -> {}, and --> {}", theMatrixUser2TokenMap.getName(), this.theMatrixToken2UserMap.getName());
    }

    /**
     *
     * @param userName The RoomServer User Name
     * @return String The User's ID Token
     */
    public String getUserTokenFromUserName(String userName) {
        LOG.debug("getUserToken(): Parameter.pRoomServerUserId -> {}", userName);
        if (userName == null) {
            LOG.debug("getUserToken(): No User Token available, userName == null");
            return (null);
        }
        if (theMatrixUser2TokenMap.isEmpty()) {
            LOG.debug("getUserToken(): No User Token available, User Name/Token Map is empty");
            return (null);
        }
        String mappedUserToken = theMatrixUser2TokenMap.get(userName);
        if (mappedUserToken != null) {
            LOG.debug("getUserToken(): Returning User Token -> {}", mappedUserToken);
            return (mappedUserToken);
        }
        LOG.debug("getUserToken(): No User Token available, no User Name/Token ID map entry found for User Name {}", userName);
        return (null);
    }

    /**
     *
     * @param userToken The RoomServer User Token
     * @return String The User's UserName
     */
    public String getUserNameFromUserToken(String userToken) {
        LOG.debug("getUserName(): Entry, userToken -> {}", userToken);
        if (userToken == null) {
            LOG.debug("getUserName(): No User Name found, userToken == null");
            return (null);
        }
        if (theMatrixUser2TokenMap.isEmpty()) {
            LOG.debug("getUserName(): No User Name found, User Name/Token Map is empty");
            return (null);
        }
        String mappedUserName = theMatrixUser2TokenMap.get(userToken);
        if (mappedUserName != null) {
            LOG.debug("getUserName(): Returning User Name -> {}", mappedUserName);
            return (mappedUserName);
        }
        LOG.debug("getUserName(): No User Name available, no Token ID / User Name map entry found for User Token {}", userToken);
        return (null);
    }

    public void setUserTokenForUserName(String userName, String userToken) {
        LOG.debug("setUserTokenForUserName(): Entry");
        if (userName == null) {
            LOG.debug("setUserTokenForUserName(): Exit, no user name / user token entry made, userName == null");
            return;
        }
        if (userToken == null) {
            LOG.debug("setUserTokenForUserName(): Exit, no user name / user token entry made, userToken == null");
            return;
        }
        if( this.theMatrixUser2TokenMap.get(userName) != null )
        {
            LOG.debug("setUserTokenForUserName(): Exit, no user name / user token already in map: userName -> {}, userToken --> {}", userName, userToken);
            return;
        }
        LOG.trace("setUserTokenForUserName(): adding map entry: userName -> {}, userToken --> {}", userName, userToken);
        this.theMatrixUser2TokenMap.put(userName, userToken);
        LOG.trace("setUserNameForUserToken(): adding map entry: userToken -> {}, userName --> {}", userToken, userName);
        this.theMatrixToken2UserMap.put(userToken, userName);
        LOG.debug("setFHIRResourceIdentifier(): Exit, Identifier/UserId added to cachemap");
    }

    public void setUserNameForUserToken(String userToken, String userName) {
        LOG.debug("setUserNameForUserToken(): Entry");
        setUserTokenForUserName(userName, userToken);
        LOG.debug("setFHIRResourceIdentifier(): Exit, Identifier/UserId added to cachemap");
    }

}
