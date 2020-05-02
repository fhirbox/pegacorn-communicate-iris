/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.utilities;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class IrisSharedCacheAccessorBean {

    @Inject
    IrisSharedCacheManager cacheManagerProvider;

    public Cache<String, String> getIrisSharedCache() {
        DefaultCacheManager tempCacheManager = cacheManagerProvider.getCacheManager();
        return( tempCacheManager.getCache());
    }

}
