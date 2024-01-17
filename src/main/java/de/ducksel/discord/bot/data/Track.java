package de.ducksel.discord.bot.data;

import java.util.List;

public class Track {
	private List<String> artists;
	private String id;
	private String track;
	private String mp3;

	public Track() {
	}

	public Track(List<String> artists, String id, String track, String mp3) {
		this.artists = artists;
		this.id = id;
		this.track = track;
		this.mp3 = mp3;
	}

	public List<String> getArtists() {
		return artists;
	}

	public void setArtists(List<String> artists) {
		this.artists = artists;
	}

	public String getId() {
		return id;
	}

	public void setId(String link) {
		this.id = link;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public String getMp3() {
		return mp3;
	}

	public void setMp3(String mp3) {
		this.mp3 = mp3;
	}
}
