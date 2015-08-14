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
package org.yamj.core.service.metadata.online;

import com.omertron.imdbapi.model.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.PersonNameDTO;
import org.yamj.core.web.HTMLTools;
import org.yamj.core.web.apis.ImdbApiWrapper;
import org.yamj.core.web.apis.ImdbEpisodeDTO;
import org.yamj.core.web.apis.ImdbSearchEngine;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, IPersonScanner {

    public static final String SCANNER_ID = "imdb";

    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_SPAN_END = "</span>";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TD_END = "</td>";
    private static final String HTML_TR_END = "</tr>";
    private static final String HTML_GT = ">";
    private static final Pattern PATTERN_PERSON_DOB = Pattern.compile("(\\d{1,2})-(\\d{1,2})");
    
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private ImdbApiWrapper imdbApiWrapper;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize IMDb scanner");
        
        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String imdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false, throwTempError);
            videoData.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String imdbId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(series.getTitle(), series.getStartYear(), true, throwTempError);
            series.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeasonId(Season season) {
        String imdbId = season.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            // same as series id
            imdbId = season.getSeries().getSourceDbId(SCANNER_ID);
            season.setSourceDbId(SCANNER_ID, imdbId);
        }
        return  imdbId;
    }

    @Override
    public String getEpisodeId(VideoData videoData) {
        String imdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            // NOTE: seriesId = seasonId
            String seasonId = videoData.getSeason().getSourceDbId(SCANNER_ID);
            if (StringUtils.isNotBlank(seasonId)) {
                Locale imdbLocale = localeService.getLocaleForConfig("imdb");
                List<ImdbEpisodeDTO> episodes = imdbApiWrapper.getTitleEpisodes(seasonId, imdbLocale).get(Integer.valueOf(Integer.valueOf(videoData.getSeason().getSeason())));
                if (CollectionUtils.isNotEmpty(episodes)) {
                    for (ImdbEpisodeDTO episode : episodes) {
                        if (episode.getEpisode() == videoData.getEpisode()) {
                            imdbId = episode.getImdbId();
                            videoData.setSourceDbId(SCANNER_ID, imdbId);
                            break;
                        }
                    }
                }
            }
        }
        return imdbId;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String imdbId = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(imdbId)) {
            return imdbId;
        }
        if (StringUtils.isNotBlank(person.getName())) {
            imdbId = this.imdbSearchEngine.getImdbPersonId(person.getName(), throwTempError);
            person.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }
    
    @Override
    public ScanResult scan(VideoData videoData) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getMovieId(videoData, throwTempError);
            
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available : {}", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating movie", imdbId);
            return updateVideoData(videoData, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", videoData.getTitle());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", videoData.getTitle());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + videoData.getTitle() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateVideoData(VideoData videoData, String imdbId, boolean throwTempError) throws IOException {
        String xml = imdbApiWrapper.getMovieDetailsXML(imdbId, throwTempError);

        // check type change
        if (xml.contains("\"tv-extra\"") || xml.contains("\"tv-series-series\"")) {
            return ScanResult.TYPE_CHANGE;
        }
        if (StringUtils.contains(HTMLTools.extractTag(xml, "<title>"), "(TV Series")) {
            return ScanResult.TYPE_CHANGE;
        }

        // locale for IMDb
        Locale imdbLocale = localeService.getLocaleForConfig("imdb");

        // get header tag
        String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

        // TITLE
        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(parseTitle(headerXml), SCANNER_ID);
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            videoData.setTitleOriginal(parseOriginalTitle(headerXml), SCANNER_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            videoData.setPublicationYear(parseYear(headerXml), SCANNER_ID);
        }

        // PLOT
        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(parsePlot(xml), SCANNER_ID);
        }

        // OUTLINE
        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(parseOutline(xml), SCANNER_ID);
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setTagline(parseTagline(xml), SCANNER_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(videoData, SCANNER_ID)) {
            videoData.setQuote(parseQuote(xml), SCANNER_ID);
        }

        // RATING
        String srtRating = HTMLTools.extractTag(xml, "star-box-giga-star\">", HTML_DIV_END).replace(",", ".");
        int intRating = parseRating(HTMLTools.stripTags(srtRating));
        // try another format for the rating
        if (intRating == -1) {
            srtRating = HTMLTools.extractTag(xml, "star-bar-user-rate\">", HTML_SPAN_END).replace(",", ".");
            intRating = parseRating(HTMLTools.stripTags(srtRating));
        }
        videoData.addRating(SCANNER_ID, intRating);

        // TOP250
        String strTop = HTMLTools.extractTag(xml, "Top 250 #");
        if (StringUtils.isNumeric(strTop)) {
            videoData.setTopRank(NumberUtils.toInt(strTop, -1));
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            videoData.setCountryCodes(parseCountryCodes(xml), SCANNER_ID);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            videoData.setStudioNames(imdbApiWrapper.getProductionStudios(imdbId), SCANNER_ID);
        }

        // CERTIFICATIONS
        videoData.setCertificationInfos(imdbApiWrapper.getCertifications(imdbId, imdbLocale));

        // RELEASE INFO
        parseReleaseInfo(videoData, imdbId, imdbLocale);

        // CAST and CREW
        parseCastCrew(videoData, imdbId);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.movie.awards", Boolean.FALSE)) {
            videoData.addAwardDTOS(imdbApiWrapper.getAwards(imdbId));
        }
        
        return ScanResult.OK;
    }


    @Override
    public ScanResult scan(Series series) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getSeriesId(series, throwTempError);
            
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", series.getTitle());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating series", imdbId);
            return updateSeries(series, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", series.getTitle());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", series.getTitle());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + series.getTitle() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateSeries(Series series, String imdbId, boolean throwTempError) throws IOException {
        String xml = imdbApiWrapper.getMovieDetailsXML(imdbId, throwTempError);
        Locale imdbLocale = localeService.getLocaleForConfig("imdb");
        
        // get header tag
        final String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

        // TITLE
        final String title = parseTitle(headerXml);
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(title, SCANNER_ID);
        }

        // ORIGINAL TITLE
        final String titleOriginal = parseOriginalTitle(headerXml);
        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(titleOriginal, SCANNER_ID);
        }

        // START YEAR and END YEAR
        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            parseYears(headerXml, series);
        }

        // PLOT
        String plot = parsePlot(xml);
        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(plot, SCANNER_ID);
        }

        // OUTLINE
        String outline = parseOutline(xml);
        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(outline, SCANNER_ID);
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            series.setStudioNames(imdbApiWrapper.getProductionStudios(imdbId), SCANNER_ID);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            series.setCountryCodes(parseCountryCodes(xml), SCANNER_ID);
        }

        // CERTIFICATIONS
        series.setCertificationInfos(imdbApiWrapper.getCertifications(imdbId, imdbLocale));

        // RELEASE INFO
        parseReleaseInfo(series, imdbId, imdbLocale);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.tvshow.awards", Boolean.FALSE)) {
            series.addAwardDTOS(imdbApiWrapper.getAwards(imdbId));
        }

        // scan seasons
        this.scanSeasons(series, imdbId, title, titleOriginal, plot, outline, imdbLocale);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, String imdbId, String title, String titleOriginal, String plot, String outline, Locale imdbLocale) {
        for (Season season : series.getSeasons()) {

            // use values from series
            if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                season.setTitle(title, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                season.setTitle(titleOriginal, SCANNER_ID);
            }
            if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                season.setPlot(plot, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                season.setOutline(outline, SCANNER_ID);
            }

            Map<Integer, ImdbEpisodeDTO> episodes = null;
            if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                episodes = getEpisodes(imdbId, season.getSeason(), imdbLocale);

                Date publicationYear = null;
                for (ImdbEpisodeDTO episode : episodes.values()) {
                    if (publicationYear == null) {
                        publicationYear = episode.getReleaseDate();
                    } else if (episode.getReleaseDate() != null) {
                        if (publicationYear.after(episode.getReleaseDate())) {
                            // previous episode
                            publicationYear = episode.getReleaseDate();
                        }
                    }
                }
                season.setPublicationYear(MetadataTools.extractYearAsInt(publicationYear), SCANNER_ID);
            }

            // mark season as done
            season.setTvSeasonDone();

            // only scan episodes if not done before
            if (!season.isTvEpisodesScanned(SCANNER_ID)) {
                if (episodes == null) {
                    episodes = getEpisodes(imdbId, season.getSeason(), imdbLocale);
                }

                for (VideoData videoData : season.getVideoDatas()) {
                    if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                        // nothing to do if already done
                        continue;
                    }

                    // scan episode
                    this.scanEpisode(videoData, episodes.get(videoData.getEpisode()), imdbLocale);
                }
            }
        }
    }

    private void scanEpisode(VideoData videoData, ImdbEpisodeDTO dto, Locale imdbLocale) {
        if (dto == null) {
            videoData.setTvEpisodeNotFound();
            return;
        }
        videoData.setSourceDbId(SCANNER_ID, dto.getImdbId());

        // set other values
        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(dto.getTitle(), SCANNER_ID);
        }
        if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
            videoData.setRelease(dto.getReleaseCountry(), dto.getReleaseDate(), SCANNER_ID);
        }

        // get movie details from IMDB
        ImdbMovieDetails movieDetails = imdbApiWrapper.gerMovieDetails(dto.getImdbId(), imdbLocale);
        if (StringUtils.isBlank(movieDetails.getImdbId())) {
            videoData.setTvEpisodeNotFound();
            return;
        }
        
        // TITLE
        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(movieDetails.getTitle(), SCANNER_ID);
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            // no original title present; so always get the title
            videoData.setTitleOriginal(movieDetails.getTitle(), SCANNER_ID);
        }

        // RELEASE DATE
        if (MapUtils.isNotEmpty(movieDetails.getReleaseDate()) && OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
            Date releaseDate = MetadataTools.parseToDate(movieDetails.getReleaseDate().get("normal"));
            videoData.setRelease(releaseDate, SCANNER_ID);
        }

        // PLOT
        if (movieDetails.getBestPlot() != null && OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(movieDetails.getBestPlot().getSummary(), SCANNER_ID);
        }

        // OUTLINE
        if (movieDetails.getPlot() != null && OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(movieDetails.getPlot().getOutline(), SCANNER_ID);
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setTagline(movieDetails.getTagline(), SCANNER_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(videoData, SCANNER_ID)) {
            if (movieDetails.getQuote() != null && CollectionUtils.isNotEmpty(movieDetails.getQuote().getLines())) {
                videoData.setQuote(movieDetails.getQuote().getLines().get(0).getQuote(), SCANNER_ID);
            }
        }

        // CAST and CREW
        parseCastCrew(videoData, dto.getImdbId());
    }

    private Map<Integer, ImdbEpisodeDTO> getEpisodes(String imdbId, int season, Locale imdbLocale) {
        Map<Integer, ImdbEpisodeDTO> episodes = new HashMap<>();
        
        List<ImdbEpisodeDTO> episodeList = imdbApiWrapper.getTitleEpisodes(imdbId, imdbLocale).get(Integer.valueOf(season));
        if (episodeList != null) {
            for (ImdbEpisodeDTO episode : episodeList) {
                episodes.put(Integer.valueOf(episode.getEpisode()), episode);
            }
        }
        return episodes;
    }

    private void parseReleaseInfo(AbstractMetadata metadata, String imdbId, Locale locale) {

        // RELEASE DATE
        if (metadata instanceof VideoData) {
            VideoData videoData = (VideoData) metadata;
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                // load the release page from IMDb
                String releaseInfoXML = imdbApiWrapper.getReleasInfoXML(imdbId);
                if (releaseInfoXML != null) {
                    List<String> releaseTags = HTMLTools.extractTags(releaseInfoXML, "<table id=\"release_dates\"", HTML_TABLE_END, "<tr", HTML_TR_END);
                    boolean found = findReleaseDate(videoData, releaseTags, locale.getCountry());
                    if (!found && !Locale.US.getCountry().equals(locale.getCountry())) {
                        // try with US country code
                        findReleaseDate(videoData, releaseTags, Locale.US.getCountry());
                    }
                }
            }
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(metadata, SCANNER_ID)) {
            // get the AKAs from release info XML
            Map<String, String> akas = getAkaMap(imdbId);
            if (MapUtils.isNotEmpty(akas)) {
                for (Map.Entry<String, String> aka : akas.entrySet()) {
                    if (StringUtils.indexOfIgnoreCase(aka.getKey(), "original title") > 0) {
                        metadata.setTitleOriginal(aka.getValue().trim(), SCANNER_ID);
                        break;
                    }
                }
            }
        }

        // TITLE for preferred country from AKAS
        boolean akaScrapeTitle = configServiceWrapper.getBooleanProperty("imdb.aka.scrape.title", Boolean.FALSE);
        if (akaScrapeTitle && OverrideTools.checkOverwriteTitle(metadata, SCANNER_ID)) {
            Map<String, String> akas = getAkaMap(imdbId);
            
            if (MapUtils.isNotEmpty(akas)) {
                List<String> akaIgnoreVersions = configServiceWrapper.getPropertyAsList("imdb.aka.ignore.versions", "");

                // build countries to search for within AKA list
                Set<String> akaMatchingCountries = new TreeSet<>(localeService.getCountryNames(locale.getCountry()));
                for (String fallback : configServiceWrapper.getPropertyAsList("imdb.aka.fallback.countries", "")) {
                    String countryCode = localeService.findCountryCode(fallback);
                    akaMatchingCountries.addAll(localeService.getCountryNames(countryCode));
                }

                String foundValue = null;
                // NOTE: First matching country is the preferred country
                for (String matchCountry : akaMatchingCountries) {
                    for (Map.Entry<String, String> aka : akas.entrySet()) {
                        int startIndex = aka.getKey().indexOf(matchCountry);
                        if (startIndex > -1) {
                            String extracted = aka.getKey().substring(startIndex);
                            int endIndex = extracted.indexOf('/');
                            if (endIndex > -1) {
                                extracted = extracted.substring(0, endIndex);
                            }

                            boolean valid = Boolean.TRUE;
                            for (String ignore : akaIgnoreVersions) {
                                if (StringUtils.isNotBlank(ignore) && StringUtils.containsIgnoreCase(extracted, ignore.trim())) {
                                    valid = Boolean.FALSE;
                                    break;
                                }
                            }

                            if (valid) {
                                foundValue = aka.getValue().trim();
                                break;
                            }
                        }
                    }

                    if (foundValue != null) {
                        // we found a title for the country matcher
                        break;
                    }
                }

                metadata.setTitle(foundValue, SCANNER_ID);
            }
        }
    }

    private boolean findReleaseDate(VideoData videoData, List<String> releaseTags, String countryCode) {
        for (String tag : releaseTags) {
            for (String country : localeService.getCountryNames(countryCode)) {
                if (tag.indexOf(HTML_GT + country) > -1) {
                    String dateToParse = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "<td class=\"release_date\">", HTML_TD_END));
                    Date releaseDate = MetadataTools.parseToDate(dateToParse);
                    if (releaseDate != null) {
                        videoData.setRelease(countryCode, releaseDate, SCANNER_ID);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private Map<String, String> getAkaMap(String imdbId) {
        String releaseInfoXML = imdbApiWrapper.getReleasInfoXML(imdbId);
        if (releaseInfoXML != null) {
            // Just extract the AKA section from the page
            List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
            return buildAkaMap(akaList);
        }
        return null;
    }
        
    
    /**
     * Parse the rating
     *
     * @param rating
     * @return
     */
    private static int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        if (st.hasMoreTokens()) {
            return MetadataTools.parseRating(st.nextToken());
        }
        return -1;
    }

    private static Set<String> parseGenres(String xml) {
        Set<String> genres = new LinkedHashSet<>();
        for (String genre : HTMLTools.extractTags(xml, "Genres" + HTML_H4_END, HTML_DIV_END)) {
            // check normally for the genre
            String iGenre = HTMLTools.getTextAfterElem(genre, "<a");
            // sometimes the genre is just "{genre}</a>???" so try and remove the trailing element
            if (StringUtils.isBlank(iGenre) && genre.contains(HTML_A_END)) {
                iGenre = genre.substring(0, genre.indexOf(HTML_A_END));
            }
            genres.add(iGenre);
        }
        return genres;
    }

    private static String parsePlot(String xml) {
        String plot = HTMLTools.extractTag(xml, "<h2>Storyline</h2>", "<em class=\"nobr\">");
        plot = HTMLTools.removeHtmlTags(plot);
        if (StringUtils.isNotBlank(plot)) {
            // See if the plot has the "metacritic" text and remove it
            int pos = plot.indexOf("Metacritic.com)");
            if (pos > 0) {
                plot = plot.substring(pos + "Metacritic.com)".length());
            }
            plot = plot.trim();
        }
        return plot;
    }

    private static String parseOutline(String xml) {
        // the new outline is at the end of the review section with no preceding text
        String outline = HTMLTools.extractTag(xml, "<p itemprop=\"description\">", "</p>");
        return cleanStringEnding(HTMLTools.removeHtmlTags(outline)).trim();
    }

    private static String parseTagline(String xml) {
        int startTag = xml.indexOf("<h4 class=\"inline\">Tagline" + HTML_H4_END);
        if (startTag != -1) {
            // We need to work out which of the two formats to use, this is dependent on which comes first "<span" or "</div"
            String endMarker;
            if (StringUtils.indexOf(xml, "<span", startTag) < StringUtils.indexOf(xml, HTML_DIV_END, startTag)) {
                endMarker = "<span";
            } else {
                endMarker = HTML_DIV_END;
            }

            // Now look for the right string
            String tagline = HTMLTools.extractTag(xml, "<h4 class=\"inline\">Tagline" + HTML_H4_END, endMarker);
            tagline = HTMLTools.stripTags(tagline);
            return cleanStringEnding(tagline);
        }
        return null;
    }

    private static String parseQuote(String xml) {
        for (String quote : HTMLTools.extractTags(xml, "<h4>Quotes</h4>", "<span class=\"", "<br", "<br")) {
            if (quote != null) {
                quote = HTMLTools.stripTags(quote);
                return cleanStringEnding(quote);
            }
        }
        return null;
    }

    private static String parseTitle(String xml) {
        String title = HTMLTools.extractTag(xml, "<span class=\"itemprop\" itemprop=\"name\">", "</span>");
        return StringUtils.trimToNull(title);
    }

    private static String parseOriginalTitle(String xml) {
        String originalTitle = HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>");
        StringUtils.remove(originalTitle, "<i>(original title)</i>");
        StringUtils.remove(originalTitle, "\"");
        return StringUtils.trimToNull(originalTitle);
    }

    private static int parseYear(String xml) {
        String date = HTMLTools.extractTag(xml, "<span class=\"nobr\">", "</span>");
        date = StringUtils.remove(date, "(");
        date = StringUtils.remove(date, ")");
        date = StringUtils.trimToNull(date);
        return MetadataTools.extractYearAsInt(date);
    }

    private static void parseYears(String xml, Series series) {
        String years = HTMLTools.extractTag(xml, "<span class=\"nobr\">", "</span>");
        years = StringUtils.remove(years, "(");
        years = StringUtils.remove(years, ")");
        years = StringUtils.trimToEmpty(years);
        String[] parts = years.split("-");
        if (parts.length > 0) {
            series.setStartYear(MetadataTools.extractYearAsInt(parts[0]), SCANNER_ID);
            if (parts.length > 1) {
                series.setEndYear(MetadataTools.extractYearAsInt(parts[1]), SCANNER_ID);
            }
        }
    }

    private Set<String> parseCountryCodes(String xml) {
        Set<String> countryCodes = new HashSet<>();
        for (String country : HTMLTools.extractTags(xml, "Country" + HTML_H4_END, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
            final String countryCode = localeService.findCountryCode(HTMLTools.removeHtmlTags(country));
            if (countryCode != null) countryCodes.add(countryCode);
        }
        return countryCodes;
    }

    /**
     * Remove the "see more" or "more" values from the end of a string
     *
     * @param uncleanString
     * @return
     */
    private static String cleanStringEnding(String uncleanString) {
        int pos = uncleanString.indexOf("more");
        // First let's check if "more" exists in the string
        if (pos > 0) {
            if (uncleanString.endsWith("more")) {
                return uncleanString.substring(0, uncleanString.length() - 4).trim();
            }

            pos = uncleanString.toLowerCase().indexOf("see more");
            if (pos > 0) {
                return uncleanString.substring(0, pos).trim();
            }
        }

        pos = uncleanString.toLowerCase().indexOf("see full summary");
        if (pos > 0) {
            return uncleanString.substring(0, pos).trim();
        }

        return uncleanString.trim();
    }

    /**
     * Create a map of the AKA values
     *
     * @param list
     * @return
     */
    private static Map<String, String> buildAkaMap(List<String> list) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        do {
            try {
                String key = list.get(i++);
                String value = list.get(i++);
                map.put(key, value);
            } catch (Exception ignore) {
                i = -1;
            }
        } while (i != -1);
        return map;
    }

    private void parseCastCrew(VideoData videoData, String imdbId) {
        List<ImdbCredit> fullCast = imdbApiWrapper.getFullCast(imdbId);
        
        if (CollectionUtils.isEmpty(fullCast)) {
            LOG.info("No cast for imdb ID: {}", imdbId);
            return;
        }

        // build jobs map
        EnumMap<JobType,List<ImdbCast>> jobs = getJobs(fullCast);
        // get configuration parameters
        boolean skipFaceless = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.faceless", Boolean.FALSE);
        boolean skipUncredited = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.uncredited", Boolean.TRUE);
        
        // add credits
        addCredits(videoData, JobType.DIRECTOR, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.WRITER, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.ACTOR, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.PRODUCER, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.CAMERA, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.EDITING, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.ART, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.SOUND, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.EFFECTS, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.LIGHTING, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.COSTUME_MAKEUP, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.CREW, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.UNKNOWN, jobs, skipUncredited, skipFaceless);
    }

    private static EnumMap<JobType,List<ImdbCast>>  getJobs(List<ImdbCredit> credits) {
        EnumMap<JobType,List<ImdbCast>> result = new EnumMap<>(JobType.class);
        
        for (ImdbCredit credit : credits) {
            if (CollectionUtils.isEmpty(credit.getCredits())) {
                continue;
            }
            
            switch (credit.getToken()) {
                case "cast":
                    result.put(JobType.ACTOR, credit.getCredits());
                    break;
                case "writers":
                    result.put(JobType.WRITER, credit.getCredits());
                    break;
                case "directors":
                    result.put(JobType.DIRECTOR, credit.getCredits());
                    break;
                case "cinematographers":
                    result.put(JobType.CAMERA, credit.getCredits());
                    break;
                case "editors":
                    result.put(JobType.EDITING, credit.getCredits());
                    break;
                case "producers":
                case "casting_directors":
                    if (result.containsKey(JobType.PRODUCER)) {
                        result.get(JobType.PRODUCER).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.PRODUCER, credit.getCredits());
                    }
                    break;
                case "music_original":
                    result.put(JobType.SOUND, credit.getCredits());
                    break;
                case "production_designers":
                case "art_directors":
                case "set_decorators":
                    if (result.containsKey(JobType.ART)) {
                        result.get(JobType.ART).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.ART, credit.getCredits());
                    }
                    break;
                case "costume_designers":
                    if (result.containsKey(JobType.COSTUME_MAKEUP)) {
                        result.get(JobType.COSTUME_MAKEUP).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.COSTUME_MAKEUP, credit.getCredits());
                    }
                    break;
                case "assistant_directors":
                case "production_managers":
                case "art_department":
                case "sound_department":
                case "special_effects_department":
                case "visual_effects_department":
                case "stunts":
                case "camera_department":
                case "animation_department":
                case "casting_department":
                case "costume_department":
                case "editorial_department":
                case "music_department":
                case "transportation_department":
                case "make_up_department":
                case "miscellaneous":
                    if (result.containsKey(JobType.CREW)) {
                        result.get(JobType.CREW).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.CREW, credit.getCredits());
                    }
                    break;
                default:
                    if (result.containsKey(JobType.UNKNOWN)) {
                        result.get(JobType.UNKNOWN).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.UNKNOWN, credit.getCredits());
                    }
                    break;
            }
        }
        
        return result;
    }
    
    private void addCredits(VideoData videoData, JobType jobType, EnumMap<JobType,List<ImdbCast>> jobs, boolean skipUncredited, boolean skipFaceless) {
        if (CollectionUtils.isEmpty(jobs.get(jobType))) return;
        if (!this.configServiceWrapper.isCastScanEnabled(jobType)) return;
            
        for (ImdbCast cast : jobs.get(jobType)) {
            final ImdbPerson person = cast.getPerson();
            if (person == null || StringUtils.isBlank(person.getName())) {
                continue;
            }
            
            if (skipUncredited && StringUtils.contains(cast.getAttr(), "(uncredited")) {
                continue;
            }

            final String photoURL = (person.getImage() == null ? null : person.getImage().getUrl());
            if (skipFaceless && JobType.ACTOR.equals(jobType) && StringUtils.isEmpty(photoURL)) {
                // skip faceless actors only
                continue;
            }
            
            CreditDTO creditDTO = new CreditDTO(SCANNER_ID, person.getActorId(), jobType, person.getName());
            creditDTO.setRole(cast.getCharacter());
            creditDTO.setVoice(StringUtils.contains(cast.getAttr(), "(voice"));
            creditDTO.addPhotoURL(photoURL);
            videoData.addCreditDTO(creditDTO);
        }
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        return scanImdbID(nfoContent, dto, ignorePresentId);
    }

    public static boolean scanImdbID(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }

        LOG.trace("Scanning NFO for IMDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                String sourceId = st.nextToken();
                LOG.debug("IMDb ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        try {
            int beginIndex = nfoContent.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfoContent.length()) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 7), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                String sourceId = "tt" + st.nextToken();
                LOG.debug("IMDb ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No IMDb ID found in NFO");
        return Boolean.FALSE;
    }

    @Override
    public ScanResult scan(Person person) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getPersonId(person, throwTempError);
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", person.getName());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating person", imdbId);
            return updatePerson(person, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.person", 0);
            if (person.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", person.getName());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", person.getName());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + person.getName() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updatePerson(Person person, String imdbId, boolean throwTempError) throws IOException {
        Locale imdbLocale = localeService.getLocaleForConfig("imdb");
        ImdbPerson imdbPerson = imdbApiWrapper.getPerson(imdbId, imdbLocale);
        if (StringUtils.isBlank(imdbPerson.getActorId())) {
            return ScanResult.NO_RESULT;
        }
        // TODO remove BIO XML if IMDb person contains the needed values
        final String bio = imdbApiWrapper.getPersonBioXML(imdbId, throwTempError);
        
        if (OverrideTools.checkOverwritePersonNames(person, SCANNER_ID)) {
            // split person names
            PersonNameDTO nameDTO = MetadataTools.splitFullName(imdbPerson.getName());
            if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
                person.setName(nameDTO.getName(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteFirstName(person, SCANNER_ID)) {
                person.setFirstName(nameDTO.getFirstName(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteLastName(person, SCANNER_ID)) {
                person.setLastName(nameDTO.getLastName(), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBiography(person, SCANNER_ID)) {
            person.setBiography(MetadataTools.cleanBiography(imdbPerson.getBiography()), SCANNER_ID);
        }

        int endIndex;
        int beginIndex;
        
        if (OverrideTools.checkOverwriteBirthDay(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Birth</td>");
            if (beginIndex > -1) {
                StringBuilder date = new StringBuilder();
                endIndex = bio.indexOf(">Date of Death</td>");
                beginIndex = bio.indexOf("birth_monthday=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    Matcher m = PATTERN_PERSON_DOB.matcher(bio.substring(beginIndex + 15, beginIndex + 20));
                    if (m.find()) {
                        date.append(m.group(2)).append("-").append(m.group(1));
                    }
                }

                beginIndex = bio.indexOf("birth_year=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    if (date.length() > 0) {
                        date.append("-");
                    }
                    date.append(bio.substring(beginIndex + 11, beginIndex + 15));
                }

                person.setBirthDay(MetadataTools.parseToDate(date.toString()), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Birth</td>");
            if (beginIndex > -1) {
                beginIndex = bio.indexOf("birth_place=", beginIndex);
                String place;
                if (beginIndex > -1) {
                    place = HTMLTools.extractTag(bio, "birth_place=", HTML_A_END);
                    int start = place.indexOf('>');
                    if (start > -1 && start < place.length()) {
                        place = place.substring(start + 1);
                    }
                    person.setBirthPlace(place, SCANNER_ID);
                }
            }
        }

        if (OverrideTools.checkOverwriteDeathDay(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Death</td>");
            if (beginIndex > -1) {
                StringBuilder date = new StringBuilder();
                endIndex = bio.indexOf(">Mini Bio (1)</h4>", beginIndex);
                beginIndex = bio.indexOf("death_monthday=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    Matcher m = PATTERN_PERSON_DOB.matcher(bio.substring(beginIndex + 15, beginIndex + 20));
                    if (m.find()) {
                        date.append(m.group(2));
                        date.append("-");
                        date.append(m.group(1));
                    }
                }
                beginIndex = bio.indexOf("death_date=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    if (date.length() > 0) {
                        date.append("-");
                    }
                    date.append(bio.substring(beginIndex + 11, beginIndex + 15));
                }
                person.setDeathDay(MetadataTools.parseToDate(date.toString()), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBirthName(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Birth Name</td>");
            if (beginIndex > -1) {
                beginIndex += 20;
                String name = bio.substring(beginIndex, bio.indexOf(HTML_TD_END, beginIndex));
                person.setBirthName(HTMLTools.decodeHtml(name), SCANNER_ID);
            }
        }

        return ScanResult.OK;
    }
}
