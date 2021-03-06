/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.scheduling;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.service.trailer.TrailerScannerService;

@Component
public class TrailerScanScheduler extends AbstractQueueScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerScanScheduler.class);
    private static final ReentrantLock PROCESS_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private TrailerScannerService trailerScannerService;
    @Autowired
    private TrailerStorageService trailerStorageService;
    @Autowired
    private TrailerProcessScheduler trailerProcessScheduler;
    
    private boolean messageDisabled = false; // Have we already printed the disabled message
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);
    
    @Scheduled(initialDelay = 10000, fixedDelay = 300000)
    public void trigger() {
        LOG.trace("Trigger trailer scan");
        watchProcess.set(true);
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 1000)
    public void run() {
        if (watchProcess.get() && PROCESS_LOCK.tryLock()) {
            try {
                scanTrailer();
            } finally {
                PROCESS_LOCK.unlock();
            }
        }
    }
    
    private void scanTrailer() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.trailerscan.maxThreads", 0);
        if (maxThreads <= 0) {
            if (!messageDisabled) {
                messageDisabled = true;
                LOG.info("Trailer scanning is disabled");
            }
            watchProcess.set(false);
        } else {
            if (messageDisabled) {
                LOG.info("Trailer scanning is enabled");
                messageDisabled = false;
            }
    
            // process located or generated artwork
            int maxResults = Math.max(1, configService.getIntProperty("yamj3.scheduler.trailerscan.maxResults", 30));
            List<QueueDTO> queueElements = trailerStorageService.getTrailerQueueForScanning(maxResults);
            if (CollectionUtils.isEmpty(queueElements)) {
                LOG.trace("No trailer found to scan");
                watchProcess.set(false);
            } else {
                LOG.info("Found {} trailer objects to process; scan with {} threads", queueElements.size(), maxThreads);
                threadedProcessing(queueElements, maxThreads, trailerScannerService);
                LOG.debug("Finished trailer scanning");
            }
        }
        
        // trigger trailer processing
        trailerProcessScheduler.trigger();
    }
}
