/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.utilities;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class SharedCache {
 
    private static final long ENTRY_LIFESPAN = 7 * 24 * 60 * 60 * 1000; // 7 Days
    
    private DefaultCacheManager shareCacheManager;
 
    public DefaultCacheManager getCacheManager() {
        if (shareCacheManager == null) {
 
            GlobalConfiguration glob = new GlobalConfigurationBuilder().clusteredDefault() 
                    .transport().addProperty("configurationFile", "jgroups-udp.xml")  
                    .globalJmxStatistics().allowDuplicateDomains(true).enable() 
                    .build();  
            Configuration loc = new ConfigurationBuilder().jmxStatistics().enable()  
                    .clustering().cacheMode(CacheMode.DIST_SYNC)  
                    .hash().numOwners(2)  
                    .expiration().lifespan(ENTRY_LIFESPAN)  
                    .build();
            shareCacheManager = new DefaultCacheManager(glob, loc, true);
        }
        return shareCacheManager;
    }
 
    @PreDestroy
    public void cleanUp() {
        shareCacheManager.stop();
        shareCacheManager = null;
    }
}
