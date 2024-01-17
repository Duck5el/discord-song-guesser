package de.ducksel.discord.bot.data;

public class Constants {
	//Valid Spotify urls
	public static final String SPOTIFY = "https://open.spotify.com/"; 
	
	//Spotify endpoints
	public static final String PLAYLIST = "playlist/";
	
	//Spotify APIs
	public static final String SPOTIFY_API = "https://api.spotify.com/v1/";
	public static final String SPOTIFY_ACCOUNT_API = "https://accounts.spotify.com/api/";
	
	//Spotify API endpoints
	public static final String TOKEN = "token/"; //Endpoint to get a short lived bearer token from the SPOTIFY_ACCOUNT_API
	public static final String PLAYLISTS = "playlists/"; //Endpoint of the SPOTIFY_API
	public static final String TRACKS = "tracks/"; //Additional endpoint of SPOTIFY_API.PLAYLISTS to only retrieve the tracks of a playlist
	
	// Regex
	public static final String HUMAN_ERROR_REGEX = "[^ a-z0-9äöüß]";
	public static final String EXTRACT_PLAYLIST_ID_REGEX = "https:\\/\\/open\\.spotify\\.com\\/playlist\\/|\\?.*";
	
	//VM ARGS
	public static final String BOT_TOKEN = "BotToken";
	public static final String SPOTIFY_CLIENT_ID = "SpotifyClientId";
	public static final String SPOTIFY_CLIENT_SECRET = "SpotifyClientSecret";
	public static final String GAMES_CONFIG = "GamesConfig";
	public static final String REPLACEMENT_FILE = "ReplacementFile";
	
}
