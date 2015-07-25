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
package org.yamj.core.database.service;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.database.dao.UpgradeDatabaseDao;

@Component
public class UpgradeDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeDatabaseService.class);

    @Autowired
    private UpgradeDatabaseDao upgradeDatabaseDao;

    @PostConstruct
    public void init() {
        // Issues: #222
        // Date:   18.07.2015
        try {
            upgradeDatabaseDao.patchTrailers();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchTrailers'", ex);
        }

        // Issues: enhancement
        // Date:   21.07.2015
        try {
            upgradeDatabaseDao.patchArtworkConfig();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchArtworkConfig'", ex);
        }

        // Issues: #234
        // Date:   24.07.2015
        try {
            upgradeDatabaseDao.patchLocales();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchLocales'", ex);
        }
    }
}
