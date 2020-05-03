/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class IrisSharedCacheAccessorBean {
    private static final Logger LOG = LoggerFactory.getLogger(IrisSharedCacheAccessorBean.class);

    @Inject
    IrisSharedCacheManager cacheManagerProvider;

    public Cache<String, String> getIrisSharedCache(String cacheName) {
        LOG.debug("getIrisSharedCache(): entry, cacheName --> {}", cacheName);
        DefaultCacheManager tempCacheManager = cacheManagerProvider.getDefaultCacheManager();
        LOG.trace("getIrisSharedCache(): got the DefaultCacheManager, name --> {}", tempCacheManager.getName() );
        Cache<String, String> newCache = tempCacheManager.getCache(cacheName, true);
        LOG.debug("getIrisSharedCache(): exit, got the new Cache, name --> {} ", newCache.getName() );
        return( newCache);
    }

}
