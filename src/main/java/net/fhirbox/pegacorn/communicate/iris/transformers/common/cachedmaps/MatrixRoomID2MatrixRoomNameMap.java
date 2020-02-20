/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.common.cachedmaps;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@Singleton
public class MatrixRoomID2MatrixRoomNameMap {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomID2MatrixRoomNameMap.class);
    @Inject
    DefaultCacheManager theCommunicateCacheContainer;

    private Cache<String, String> theRoomId2RoomNameMap;

    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        theRoomId2RoomNameMap = theCommunicateCacheContainer.getCache("RoomID2RoomNameReferenceMap", true);
        LOG.debug("start(): Exit, Got Cache -> " + theRoomId2RoomNameMap.getName());
    }

    public String getName(String pRoomId) {
        if (pRoomId == null) {
            return (null);
        }
        String roomName = theRoomId2RoomNameMap.get(pRoomId);
        if (roomName != null) {
            return (roomName);
        }
        return (null);
    }

    public void setName(String pRoomId, String pRoomName) {
        if (pRoomName == null) {
            return;
        }
        if (pRoomId == null) {
            return;
        }
        this.theRoomId2RoomNameMap.put(pRoomId, pRoomName, 30, TimeUnit.DAYS);
    }

    public void modifyName(String pRoomId, String pRoomName ) {
        if (pRoomName == null) {
            return;
        }
        if (pRoomId == null) {
            return;
        }
        this.theRoomId2RoomNameMap.replace(pRoomId, pRoomName, 30, TimeUnit.DAYS);
    }
    
    public void removeName(String pRoomId) {
        if (pRoomId == null) {
            return;
        }
        this.theRoomId2RoomNameMap.remove(pRoomId);
    }

}
