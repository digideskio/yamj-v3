package com.moviejukebox.core.service.plugin;

import com.moviejukebox.core.database.model.VideoData;

public interface IMovieScanner extends IPluginDatabaseScanner {

    public String getMovieId(VideoData videoData);

    public String getMovieId(String title, int year);

    public ScanResult scan(VideoData videoData);
}