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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class IrisSharedCacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(IrisSharedCacheManager.class);

    private static final long ENTRY_LIFESPAN = 7 * 24 * 60 * 60 * 1000; // 7 Days

    private DefaultCacheManager shareCacheManager;
    private IrisCacheMapNameSet nameCacheSet = new IrisCacheMapNameSet();

    public DefaultCacheManager getDefaultCacheManager() {
        LOG.info("getCacheManager(): Entry");
        if (shareCacheManager == null) {
            LOG.info("getCacheManager(): configuring a named clustered cache configuration using Infinispan defined defaults");
            GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder().clusteredDefault();

            LOG.info("getCacheManager(): completing the config with a cluster name, jgroups config");
            GlobalConfiguration globalConfig = builder.defaultCacheName("pegacorn-communicate-iris-default-cache")
                    .transport().addProperty("configurationFile", "jgroups-udp.xml").build();

            LOG.info("getCacheManager(): Defining a local configuration for setting finer level properties");
            // including individual cache statistics and methods required for configuring the cache
            // as clustered
            Configuration localConfig = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).build();

            LOG.info("getCacheManager(): creating a cache manager based on the configurations");
            shareCacheManager = new DefaultCacheManager(globalConfig, localConfig, true);
            LOG.info("getCacheManager(): About to add specific Caches: 1st is --> {} ", nameCacheSet.getFHIRPractitionerId2MatrixUserNameMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getFHIRPractitionerId2MatrixUserNameMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 2nd is --> {} ", nameCacheSet.getFHIRResourceReference2MatrixRoomIDMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getFHIRResourceReference2MatrixRoomIDMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 3rd is --> {} ", nameCacheSet.getMatrixRoomID2FHIRResourceReferenceMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getMatrixRoomID2FHIRResourceReferenceMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 4th is --> {} ", nameCacheSet.getMatrixRoomID2MatrixRoomMapName());
            shareCacheManager.defineConfiguration(nameCacheSet.getMatrixRoomID2MatrixRoomMapName(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 5th is --> {} ", nameCacheSet.getMatrixToken2MatrixUserMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getMatrixToken2MatrixUserMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 6th is --> {} ", nameCacheSet.getMatrixUser2TokenMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getMatrixUser2TokenMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("getCacheManager(): About to add specific Caches: 7th is --> {} ", nameCacheSet.getMatrixUserName2FHIRPractitionerIdMap());
            shareCacheManager.defineConfiguration(nameCacheSet.getMatrixUserName2FHIRPractitionerIdMap(), "pegacorn-communicate-iris-default-cache", localConfig);
            LOG.info("CacheConfig count = " + shareCacheManager.getCacheConfigurationNames().size());
        }
        return shareCacheManager;
    }

    @PreDestroy
    public void cleanUp() {
        shareCacheManager.stop();
        shareCacheManager = null;
    }
}
