package com.codingintune.lastfm2youtube;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

@Service
public class YouTubeService {
	
	private Logger logger = LoggerFactory.getLogger(YouTubeService.class);

//	private static final String REIDIRECT_URI = "https://lastfm2youtube.herokuapp.com/authorizeCallback";
	private static final String REIDIRECT_URI = "http://localhost:8080/authorizeCallback";
	private static final String CLIENT_SECRET = "src/main/resources/client_secret.json";
	private static final Collection<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/youtube");
	private static final String APPLICATION_NAME = "Last.FM to YouTube";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private YouTube youtube;
	private GoogleAuthorizationCodeFlow flow;
	private NetHttpTransport httpTransport;
	
	public YouTubeService() {
	}
	
	public boolean isAuthorized() {
		return youtube != null;
	}

	public String getAuthorizationUrl(String artist) throws YouTubeServiceException {
		try {
			if (flow == null) {
				httpTransport = GoogleNetHttpTransport.newTrustedTransport();
				InputStream in = new FileInputStream(CLIENT_SECRET);
				GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
				flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES).build();
			}
			return flow.newAuthorizationUrl().setRedirectUri(REIDIRECT_URI).setState(artist).build();
		} catch (GeneralSecurityException e) {
			throw new YouTubeServiceException(e);
		} catch (FileNotFoundException e) {
			throw new YouTubeServiceException(e);
		} catch (IOException e) {
			throw new YouTubeServiceException(e);
		}
	}

	public void authorize(String code) throws IOException {
		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REIDIRECT_URI).execute();
		Credential credential = flow.createAndStoreCredential(response, "user");
		youtube = new YouTube.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	}

	public List<String> searchTracks(String artist, List<String> trackNames) throws YouTubeServiceException {
		List<String> trackIds = new ArrayList<>();
		try {
			YouTube.Search.List request = youtube.search().list("snippet");
			for (String trackName : trackNames) {
				String query = String.format("%s %s", artist, trackName);
				SearchListResponse response = request.setMaxResults(1L).setQ(query).setType("video").setVideoEmbeddable("true").execute();
				logger.info(String.format("Searching for track with query '%s'", query));
				List<SearchResult> items = response.getItems();
				if (items.isEmpty()) {
					logger.info("No videos found.");
					return trackIds;
				}
				SearchResult searchResult = items.get(0);
				ResourceId id = searchResult.getId();
				logger.info(String.format("Found: %s (VideoID: %s)", searchResult.getSnippet().getTitle(), id.getVideoId()));
				trackIds.add(id.getVideoId());
			}
		} catch (IOException e) {
			throw new YouTubeServiceException(e);
		}
		return trackIds;
	}

	public String createPlaylist(String artist, List<String> videoIds) throws YouTubeServiceException {
		Playlist playlist = createPlaylist(getPlaylistName(artist, videoIds.size()));
		logger.info(String.format("Playlist created: %s", playlist));
		for (String videoId : videoIds) {
			PlaylistItem playlistItem = addVideoToPlaylist(playlist.getId(), videoId);
			logger.info(String.format("Adding video %s to playlist %s: Response: %s", videoId, playlist.getId(), playlistItem));
		}
		return playlist.getId();
	}

	private PlaylistItem addVideoToPlaylist(String playlistId, String videoId) throws YouTubeServiceException {
		PlaylistItemSnippet snippet = new PlaylistItemSnippet();
		snippet.setPlaylistId(playlistId);
		snippet.setResourceId(new ResourceId().setKind("youtube#video").setVideoId(videoId));
		try {
			YouTube.PlaylistItems.Insert request = youtube.playlistItems().insert("snippet", new PlaylistItem().setSnippet(snippet));
			return request.execute();
		} catch (IOException e) {
			throw new YouTubeServiceException(e);
		}
	}

	private Playlist createPlaylist(String playlistTitle) throws YouTubeServiceException {
		Playlist playlist = new Playlist();
		playlist.setSnippet(new PlaylistSnippet().setTitle(playlistTitle));
		playlist.setStatus(new PlaylistStatus().setPrivacyStatus("public"));
		try {
			YouTube.Playlists.Insert request = youtube.playlists().insert("snippet,status", playlist);
			return request.execute();
		} catch (IOException e) {
			throw new YouTubeServiceException(e);
		}
	}

	public static String getPlaylistName(String artist, int tracksCount) {
		return String.format("Last.FM Top %d Tracks - %s", tracksCount, artist);
	}

}
