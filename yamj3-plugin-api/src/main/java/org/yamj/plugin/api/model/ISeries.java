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
package org.yamj.plugin.api.model;

import java.util.Collection;

public interface ISeries {

    String getId(String source);
    
    void addId(String source, String id);

    String getTitle();

    void setTitle(String title);

    String getOriginalTitle();

    void setOriginalTitle(String originalTitle);

    int getStartYear();

    void setStartYear(int startYear);

    int getEndYear();

    void setEndYear(int endYear);

    void setPlot(String plot);

    void setOutline(String outline);

    void setRating(int rating);

    void setStudios(Collection<String> studios);

    void setGenres(Collection<String> genres);

    void setCountries(Collection<String> countries);

    void addCertification(String country, String certificate);
    
    void addAward(String event, String category, int year);

    void addAward(String event, String category, int year, boolean won, boolean nominated);
    
    Collection<ISeason> getSeasons(); 
}