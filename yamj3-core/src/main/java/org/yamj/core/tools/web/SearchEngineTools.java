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
package org.yamj.core.tools.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.CommonHttpClient;
import org.yamj.common.tools.PropertyTools;

public class SearchEngineTools {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineTools.class);
    // Literals
    private static final String HTTP_LITERAL = "http://";
    private static final String HTTPS_LITERAL = "https://";
    private static final String UTF8 = "UTF-8";
    private static final String SITE = "+site%3A";
    private static final String PAREN_RIGHT = "%29";
    private static final String PAREN_LEFT = "+%28";

    private final CommonHttpClient httpClient;
    private final Charset charset;
    private final LinkedList<String> searchSites;

    private String country;
    private String searchSuffix = "";
    private String language;
    private String googleHost = "www.google.com";
    private String yahooHost = "search.yahoo.com";
    private String bingHost = "www.bing.com";
    private String blekkoHost = "www.blekko.com";

    public SearchEngineTools(CommonHttpClient httpClient) {
        this(httpClient, "us");
    }

    public SearchEngineTools(CommonHttpClient httpClient, String country) {
        this(httpClient, country, Charset.forName("UTF-8"));
    }

    public SearchEngineTools(CommonHttpClient httpClient, String country, Charset charset) {
        this.httpClient = httpClient;
        this.charset = charset;

        // sites to search for URLs
        searchSites = new LinkedList<>();
        searchSites.addAll(Arrays.asList(PropertyTools.getProperty("yamj3.searchengine.sites", "google,yahoo,bing,blekko").split(",")));

        // country specific presets
        if ("de".equalsIgnoreCase(country)) {
            this.country = "de";
            language = "de";
            googleHost = "www.google.de";
            yahooHost = "de.search.yahoo.com";
        } else if ("it".equalsIgnoreCase(country)) {
            this.country = "it";
            language = "it";
            googleHost = "www.google.it";
            yahooHost = "it.search.yahoo.com";
            bingHost = "it.bing.com";
        } else if ("se".equalsIgnoreCase(country)) {
            this.country = "se";
            language = "sv";
            googleHost = "www.google.se";
            yahooHost = "se.search.yahoo.com";
        } else if ("pl".equalsIgnoreCase(country)) {
            this.country = "pl";
            language = "pl";
            googleHost = "www.google.pl";
            yahooHost = "pl.search.yahoo.com";
        } else if ("ru".equalsIgnoreCase(country)) {
            this.country = "ru";
            language = "ru";
            googleHost = "www.google.ru";
            yahooHost = "ru.search.yahoo.com";
        } else if ("il".equalsIgnoreCase(country)) {
            this.country = "il";
            language = "il";
            googleHost = "www.google.co.il";
        } else if ("fr".equalsIgnoreCase(country)) {
            this.country = "fr";
            language = "fr";
            googleHost = "www.google.fr";
        } else if ("nl".equalsIgnoreCase(country)) {
            this.country = "nl";
            language = "nl";
            googleHost = "www.google.nl";
        }
    }

    public void setSearchSites(String searchSites) {
        this.searchSites.clear();
        this.searchSites.addAll(Arrays.asList(searchSites.split(",")));
    }

    public void setSearchSuffix(String searchSuffix) {
        this.searchSuffix = searchSuffix;
    }

    public String searchURL(String title, int year, String site) {
        return searchURL(title, year, site, null);
    }

    public String searchURL(String title, int year, String site, String additional) {
        String url;

        String engine = getNextSearchEngine();
        if ("yahoo".equalsIgnoreCase(engine)) {
            url = searchUrlOnYahoo(title, year, site, additional);
        } else if ("bing".equalsIgnoreCase(engine)) {
            url = searchUrlOnBing(title, year, site, additional);
        } else if ("blekko".equalsIgnoreCase(engine)) {
            url = searchUrlOnBlekko(title, year, site, additional);
        } else {
            url = searchUrlOnGoogle(title, year, site, additional);
        }

        return url;
    }

    public String getCurrentSearchEngine() {
        return searchSites.peekFirst();
    }

    private String getNextSearchEngine() {
        String engine = searchSites.remove();
        searchSites.addLast(engine);
        return engine;
    }

    public int countSearchSites() {
        return searchSites.size();
    }

    private String requestContent(CharSequence cs) throws IOException {
        HttpGet httpGet = new HttpGet(cs.toString());
        httpGet.setHeader(HTTP.USER_AGENT, "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");
        return httpClient.requestContent(httpGet, charset).getContent();
    }

    public String searchUrlOnGoogle(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on google; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTPS_LITERAL);
            sb.append(googleHost);
            sb.append("/search?");
            if (StringUtils.isNotBlank(language)) {
                sb.append("hl=");
                sb.append(language);
                sb.append("&");
            }
            sb.append("as_q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            if (StringUtils.isNotBlank(additional)) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            sb.append("&as_sitesearch=");
            sb.append(site);

            String xml = this.requestContent(sb);

            int beginIndex = xml.indexOf(HTTP_LITERAL + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by google search {}", title, error);
        }
        return null;
    }

    public String searchUrlOnYahoo(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on yahoo; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP_LITERAL);
            sb.append(yahooHost);
            sb.append("/search?vc=");
            if (country != null) {
                sb.append(country);
                sb.append("&rd=r2");
            }
            sb.append("&ei=UTF-8&p=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }

            String xml = this.requestContent(sb);

            String link = HTMLTools.extractTag(xml, "<span class=\"url\"", "</span>");
            link = HTMLTools.removeHtmlTags(link);
            int beginIndex = link.indexOf(site + searchSuffix);
            if (beginIndex != -1) {
                link = link.substring(beginIndex);
                // Remove "/info/xxx" from the end of the URL
                beginIndex = link.indexOf("/info");
                if (beginIndex > -1) {
                    link = link.substring(0, beginIndex);
                }
                return HTTP_LITERAL + link;
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by yahoo search '{}'", title, error);
        }
        return null;
    }

    public String searchUrlOnBing(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on bing; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP_LITERAL);
            sb.append(bingHost);
            sb.append("/search?q=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }
            if (country != null) {
                sb.append("&cc=");
                sb.append(country);
                sb.append("&filt=rf");
            }

            String xml = this.requestContent(sb);

            int beginIndex = xml.indexOf(HTTP_LITERAL + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by bing search {}", title, error);
        }
        return null;
    }

    public String searchUrlOnBlekko(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on blekko; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP_LITERAL);
            sb.append(blekkoHost);
            sb.append("/ws/?q=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }

            String xml = this.requestContent(sb);

            int beginIndex = xml.indexOf(HTTP_LITERAL + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by bing search {}", title, error);
        }
        return null;
    }
}
