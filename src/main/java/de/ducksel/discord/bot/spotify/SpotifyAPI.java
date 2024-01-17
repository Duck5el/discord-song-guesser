package de.ducksel.discord.bot.spotify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.ducksel.discord.bot.data.Constants;
import de.ducksel.discord.bot.data.Track;

public class SpotifyAPI {

	// Returns a randomized list of tracks
	public List<Track> getTracks(String[] playlists, int maxTracks) {
		String token = getToken();
		List<Track> tracks = new ArrayList<>();
		for (String playlist : playlists) {
			try {
				String playlistId = playlist.replaceAll(Constants.EXTRACT_PLAYLIST_ID_REGEX, "");
				List<Track> newTraks = getTracksFromPlaylist(playlistId, token);
				if (newTraks == null) {
					return null;
				}
				tracks.addAll(newTraks);

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		return randomizeTracks(tracks, maxTracks);
	}

	// Get a list of tracks from a playlist by id
	private List<Track> getTracksFromPlaylist(String playlistId, String token) {
		List<Track> tracks = new ArrayList<>();

		try {
			String url = Constants.SPOTIFY_API + Constants.PLAYLISTS + playlistId + "/" + Constants.TRACKS;

			boolean stop = false;
			while (!stop) {
				String response = getResponesBody(token, url);
				if (response.equals("APIError")) {
					return null;
				}
				JSONObject jsonResponse = new JSONObject(response.toString());
				List<Track> newTracks = toTrackObjects(jsonResponse, token);
				if (newTracks == null) {
					return null;
				}
				tracks.addAll(newTracks);
				Object obj = jsonResponse.get("next");
				if (obj instanceof String) {
					url = jsonResponse.getString("next");
				} else {
					stop = true;
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return tracks;
	}

	// Convert tracks json from SPOTIFY_API.PLAYLISTS.ID.TRACKS to a list of Tracks
	private List<Track> toTrackObjects(JSONObject jsonResponse, String token) {
		List<Track> tracks = new ArrayList<>();
		JSONArray items = jsonResponse.getJSONArray("items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject jTrack = items.getJSONObject(i).getJSONObject("track");
			JSONArray jArtists = jTrack.getJSONArray("artists");
			List<String> artists = new ArrayList<>();
			for (int j = 0; j < jArtists.length(); j++) {
				String artist = jArtists.getJSONObject(j).getString("name");
				artist = artist.toLowerCase().replace(";", "").replace(",", "");
				artists.add(artist);
			}
			Track track = new Track();
			if (jTrack.get("id") instanceof String) {
				track.setId(jTrack.getString("id"));
				String name = jTrack.getString("name");
				if (name.contains("(")) {
					name = name.replaceAll("\\(.*", "");
				}
				name = name.toLowerCase().replaceAll(Constants.HUMAN_ERROR_REGEX, "");
				if (name.endsWith(" ")) {
					name = name.trim();
				}
				track.setTrack(name);
				track.setArtists(artists);
				Object obj = jTrack.get("preview_url");
				if (obj instanceof String) {
					track.setMp3(obj.toString());
					tracks.add(track);
				}
			}
		}
		return tracks;
	}

	private String getResponesBody(String token, String apiUrl) {
		try {
			Connection.Response response = Jsoup.connect(apiUrl).method(Connection.Method.GET)
					.header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
					.ignoreContentType(true).execute();
			String responseBody = response.body();
			return responseBody;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return "APIError";
		}
	}

	// Get short lived token from SPOTIFY_ACCOUNT_API.TOKEN
	private String getToken() {
		try {
			String apiUrl = "https://accounts.spotify.com/api/token";

			Map<String, String> parameters = new HashMap<>();
			parameters.put("grant_type", "client_credentials");
			parameters.put("client_id", System.getProperty(Constants.SPOTIFY_CLIENT_ID));
			parameters.put("client_secret", System.getProperty(Constants.SPOTIFY_CLIENT_SECRET));

			Connection.Response response = Jsoup.connect(apiUrl).data(parameters)
					.header("Content-Type", "application/x-www-form-urlencoded").ignoreContentType(true)
					.method(Connection.Method.POST).execute();
			Document document = response.parse();
			String accessToken = document.select("body").text();
			JSONObject json = new JSONObject(accessToken);
			return json.getString("access_token");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	// Randomize the list and only return a list with the size of maxTracks or lower
	private List<Track> randomizeTracks(List<Track> tracks, int maxTracks) {
		List<Track> newTracks = new ArrayList<>();
		Collections.shuffle(tracks);
		if (maxTracks < tracks.size()) {
			for (int i = 0; i < maxTracks; i++) {
				newTracks.add(tracks.get(i));
			}
			return newTracks;
		} else {
			return tracks;
		}
	}
}
