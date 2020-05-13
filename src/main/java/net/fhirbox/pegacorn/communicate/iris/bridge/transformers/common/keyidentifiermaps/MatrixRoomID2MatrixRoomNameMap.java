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
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.Cache;
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
    private IrisSharedCacheAccessorBean theIrisCacheSetManager;
    
    private IrisCacheMapNameSet cacheName = new IrisCacheMapNameSet();

    private Cache<String, String> theRoomId2RoomNameMap;

    @PostConstruct
    public void start() {
        LOG.debug("start(): Entry");
        theRoomId2RoomNameMap = this.theIrisCacheSetManager.getIrisSharedCache(cacheName.getMatrixRoomID2MatrixRoomMapName());;
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
