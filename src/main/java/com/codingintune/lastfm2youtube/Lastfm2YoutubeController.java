package com.codingintune.lastfm2youtube;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class Lastfm2YoutubeController {
	
	private Logger logger = LoggerFactory.getLogger(Lastfm2YoutubeController.class);

	@Autowired
	private YouTubeService youtubeService;
	@Autowired
	private LastfmService lastfmService;

	@GetMapping("/")
	public String home() {
		return "main";
	}
	
	@GetMapping("/authorize")
	public ResponseEntity<String> authorize(@RequestParam String artist, ModelMap model) {
		try {
			logger.info("/authorize");
			if (!youtubeService.isAuthorized()) {
				String authorizationUrl = youtubeService.getAuthorizationUrl(artist);
				logger.info(authorizationUrl);
				return new ResponseEntity<String>(authorizationUrl, HttpStatus.OK);
			}
			return new ResponseEntity<String>("", HttpStatus.OK);
		} catch (YouTubeServiceException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return new ResponseEntity<String>("/", HttpStatus.OK); 
		}
	}

	@GetMapping("/authorizeCallback")
	public ModelAndView callback(@RequestParam String code, @RequestParam(name = "state") String artist, RedirectAttributes attributes) throws IOException {
		logger.info("/authorizeCallback");
		logger.info(code);
		logger.info(artist);
		youtubeService.authorize(code);
		attributes.addAttribute("artist", artist);
		return new ModelAndView("redirect:/listen");
	}
	
	@GetMapping("/listen")
	public ModelAndView listen(@RequestParam String artist, ModelMap model) {
		try {
			logger.info("/listen");
			if (artist.isEmpty()) {
				model.addAttribute("errorMessage", String.format("please enter an artist name", artist));
				return new ModelAndView("main");
			}
			if (!youtubeService.isAuthorized()) {
				String authorizationUrl = youtubeService.getAuthorizationUrl(artist);
				logger.info(authorizationUrl);
				return new ModelAndView("redirect:" + authorizationUrl);
			}
			List<String> trackNames = lastfmService.getArtistTopTracks(artist, 3);
			if (trackNames.isEmpty()) {
				model.addAttribute("errorMessage", String.format("artist with name '%s' not found", artist));
				return new ModelAndView("main");
			}
			List<String> trackIds = youtubeService.searchTracks(artist, trackNames);
			String playlistId = youtubeService.createPlaylist(artist, trackIds);
			String playlistUrl = "https://www.youtube.com/embed/videoseries?list=" + playlistId;
			TimeUnit.SECONDS.sleep(10); // playlists are not yet functional right after forwarding to them? we should wait alot more time maybe...
			return new ModelAndView("redirect:" + playlistUrl);
		}
		catch (YouTubeServiceException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return new ModelAndView("main");
		}
		catch (InterruptedException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return new ModelAndView("main");
		}
	}
	
}
