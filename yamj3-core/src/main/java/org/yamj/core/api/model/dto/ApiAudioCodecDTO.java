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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Information on the physical file for the API
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiAudioCodecDTO extends AbstractApiDTO {

    private String codec;
    private String codecFormat;
    private Integer bitrate = Integer.valueOf(-1);
    private Integer channels = Integer.valueOf(-1);
    private String languageCode;
    private String language;

    public ApiAudioCodecDTO() {
        // empty constructor
    }
    
    public ApiAudioCodecDTO(String codec, String codecFormat, Integer bitrate, Integer channels, String languageCode) {
        this.codec = codec;
        this.codecFormat = codecFormat;
        this.bitrate = bitrate;
        this.channels = channels;
        this.languageCode = languageCode;
    }
    
    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCodecFormat() {
        return codecFormat;
    }

    public void setCodecFormat(String codecFormat) {
        this.codecFormat = codecFormat;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getChannels() {
        return channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
