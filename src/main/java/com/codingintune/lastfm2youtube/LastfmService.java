package com.codingintune.lastfm2youtube;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Track;

@Service
public class LastfmService {
	
	private Logger logger = LoggerFactory.getLogger(LastfmService.class);
	
	@Value("${lastfm.api.key}")
	private static String API_KEY;

	public List<String> getArtistTopTracks(String artist, int limit) {
		List<String> trackNames = new ArrayList<>();
		Collection<Track> topTracks = Artist.getTopTracks(artist, API_KEY);
		logger.info(String.format("Getting top %d tracks for artist: %s", limit, artist));
		int i = 0;
		for (Track track : topTracks) {
			if (i++ >= limit) {
				break;
			}
			trackNames.add(track.getName());
			logger.info(String.format("%d. %s (%d plays)", i, track.getName(), track.getPlaycount()));
		}
		return trackNames;
	}
	
}
