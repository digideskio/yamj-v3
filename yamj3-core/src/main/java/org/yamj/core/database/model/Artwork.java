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
package org.yamj.core.database.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.core.database.model.type.ArtworkType;

@NamedQueries({    
    @NamedQuery(name = Artwork.QUERY_FIND_PERSON_ARTWORKS,
        query = "select a from Artwork a join a.person p WHERE a.artworkType=:artworkType AND lower(p.identifier)=:identifier"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = Artwork.QUERY_SCANNING_QUEUE,
        query = "SELECT DISTINCT art.id,art.artwork_type,(case when art.update_timestamp is null then art.create_timestamp else art.update_timestamp end) as maxdate "+
                "FROM artwork art LEFT OUTER JOIN videodata vd ON vd.id=art.videodata_id LEFT OUTER JOIN season sea ON sea.id=art.season_id "+
                "LEFT OUTER JOIN series ser ON ser.id=art.series_id WHERE art.status in ('NEW','UPDATED') "+
                "AND (vd.status is null OR vd.status='DONE') AND (sea.status is null OR sea.status='DONE') AND (ser.status is null OR ser.status='DONE') "+
                "ORDER BY maxdate ASC"
    )
})

@Entity
@Table(name = "artwork",
       uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORK_NATURALID", columnNames = {"artwork_type", "videodata_id", "season_id", "series_id", "person_id", "boxedset_id"}),
       indexes = {@Index(name = "IX_ARTWORK_TYPE", columnList = "artwork_type"),
                  @Index(name = "IX_ARTWORK_STATUS", columnList = "status")}
)
@SuppressWarnings("unused")
public class Artwork extends AbstractStateful {

    private static final long serialVersionUID = -981494909436217076L;
    public static final String QUERY_FIND_PERSON_ARTWORKS = "artwork.findPersonArtworks";
    public static final String QUERY_SCANNING_QUEUE = "artwork.scanning.queue";
    
    
    @NaturalId(mutable = true)    
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_VIDEODATA"))
    private VideoData videoData;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_SEASON"))
    private Season season;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_SERIES"))
    private Series series;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_PHOTO"))
    private Person person;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "boxedset_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_BOXEDSET"))
    private BoxedSet boxedSet;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artwork")
    private List<ArtworkLocated> artworkLocated = new ArrayList<>(0);

    // GETTER and SETTER
    
    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public BoxedSet getBoxedSet() {
        return boxedSet;
    }

    public void setBoxedSet(BoxedSet boxedSet) {
        this.boxedSet = boxedSet;
    }

    private void setArtworkLocated(List<ArtworkLocated> artworkLocated) {
        this.artworkLocated = artworkLocated;
    }

    public List<ArtworkLocated> getArtworkLocated() {
        return artworkLocated;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getArtworkType())
                .append(getVideoData())
                .append(getSeason())
                .append(getSeries())
                .append(getPerson())
                .append(getBoxedSet())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Artwork)) {
            return false;
        }
        Artwork other = (Artwork) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getArtworkType(), other.getArtworkType())
                .append(getVideoData(), other.getVideoData())
                .append(getSeason(), other.getSeason())
                .append(getSeries(), other.getSeries())
                .append(getPerson(), other.getPerson())
                .append(getBoxedSet(), other.getBoxedSet())
                .isEquals();
    }

    @Override
    public String toString() { //NOSONAR
        StringBuilder sb = new StringBuilder();
        sb.append("Artwork [ID=");
        sb.append(getId());
        sb.append(", type=");
        sb.append(getArtworkType());
        if (getVideoData() != null) {
            if (Hibernate.isInitialized(getVideoData())) {
                if (getVideoData().isMovie()) {
                    sb.append(", movie-id='");
                    sb.append(getVideoData().getIdentifier());
                    sb.append("'");
                } else {
                    sb.append(", episode-id='");
                    sb.append(getVideoData().getIdentifier());
                    sb.append("', episode=");
                    sb.append(getVideoData().getEpisode());
                }
            } else {
                sb.append(", target=VideoData");
            }
        } else if (getSeason() != null) {
            if (Hibernate.isInitialized(getSeason())) {
                sb.append(", season-id='");
                sb.append(getSeason().getIdentifier());
                sb.append("', season=");
                sb.append(getSeason().getSeason());
            } else {
                sb.append(", target=Season");
            }
        } else if (getSeries() != null) {
            if (Hibernate.isInitialized(getSeries())) {
                sb.append(", series-id='");
                sb.append(getSeries().getIdentifier());
                sb.append("'");
            } else {
                sb.append(", target=Series");
            }
        } else if (getPerson() != null) {
            if (Hibernate.isInitialized(getPerson())) {
                sb.append(", person-id='");
                sb.append(getPerson().getIdentifier());
                sb.append("'");
            } else {
                sb.append(", target=Person");
            }
        } else if (getBoxedSet() != null) {
            if (Hibernate.isInitialized(getBoxedSet())) {
                sb.append(", boxedset-id='");
                sb.append(getBoxedSet().getIdentifier());
                sb.append("'");
            } else {
                sb.append(", target=BoxedSet");
            }
        } else {
            sb.append("Unknown");
        }
        sb.append("]");
        return sb.toString();
    }
}
