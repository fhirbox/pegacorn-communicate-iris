/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.utilities;

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

public class IrisSharedCacheManager {
 
    private static final long ENTRY_LIFESPAN = 7 * 24 * 60 * 60 * 1000; // 7 Days
    
    private DefaultCacheManager shareCacheManager;
 
    public DefaultCacheManager getCacheManager() {
        if (shareCacheManager == null) {
            // configure a named clustered cache configuration using Infinispan defined defaults
            GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder().clusteredDefault().defaultCacheName("iris-clustered-cache");
            
            // complete the config with a cluster name, jgroups config, and enable JMX statistics
            GlobalConfiguration global = builder.transport().clusterName("iris-cluster").addProperty("configurationFile", "jgroups-udp.xml").jmx().enable().build();
            
            // define a local configuration for setting finer level properties including
            // individual cache statistics and methods required for configuring the cache
            // as clustered
            Configuration local = new ConfigurationBuilder().statistics().enable().clustering()
                    .cacheMode(CacheMode.DIST_SYNC).build();
            
            // create a cache manager based on the configurations
            shareCacheManager = new DefaultCacheManager(global, local, true);
        }
        return shareCacheManager;
    }
 
    @PreDestroy
    public void cleanUp() {
        shareCacheManager.stop();
        shareCacheManager = null;
    }
}
