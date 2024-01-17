package de.ducksel.discord.bot;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import de.ducksel.discord.bot.data.Constants;
import de.ducksel.discord.bot.data.Track;
import de.ducksel.discord.bot.io.GamesConfigManager;
import de.ducksel.discord.bot.spotify.SpotifyAPI;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class CommandListener extends ListenerAdapter {

	private AudioPlayerManager playerManager;
	private Map<Long, GuildMusicManager> musicManagers;

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (event.getName().equals("start")) {
			start(event);
		}
		if (event.getName().equals("cancel")) {
			cancel(event, true);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.isFromType(ChannelType.PRIVATE) && !event.getAuthor().isBot()) {
			try {
				// Check if player is even playing
				String playerId = event.getMessage().getAuthor().getId();
				String guildId = new GamesConfigManager().getGameOfPlayer(playerId);
				if (guildId == null) {
					return;
				}

				// Check if bot is connected else guessing would be unnecessary
				JDA jda = event.getJDA();
				Guild guild = jda.getGuildById(guildId);
				boolean isConnected = guild.getAudioManager().isConnected();
				if (!isConnected) {
					return;
				}

				try {
					// get info about the currently playing track
					GuildMusicManager musicManager = getGuildAudioPlayer(guild);
					AudioTrack audioTrack = musicManager.player.getPlayingTrack();
					String trackInfo = audioTrack.getInfo().title;
					String[] track = trackInfo.split(";");
					String[] autors = track[1].split(",");
					String replacements = new String(
							Files.readAllBytes(Paths.get(System.getProperty(Constants.REPLACEMENT_FILE))));
					long trackCurrentTime = musicManager.player.getPlayingTrack().getPosition();
					long trackTotalTime = musicManager.player.getPlayingTrack().getDuration();

					// Parse the song and message to prevent human errors and make it less
					// frustrating
					String message = event.getMessage().getContentRaw().toLowerCase().replaceAll(replacements, "");
					String song = track[0].replaceAll(replacements, "");

					// Remove Author from song titles and everything after that
					for (String author : autors) {
						author = author.replace(" ", "").replaceAll(Constants.HUMAN_ERROR_REGEX, "");
						song = song.replaceAll(author + ".*", "");
					}

					// Check if message equals song
					if (message.length() >= (song.length() * 0.6)) {
						// message only needs to contain and match 60% of the song name
						if (song.replace(" ", "")
								.contains(message.replace(" ", "").replaceAll(Constants.HUMAN_ERROR_REGEX, ""))) {
							sendSuccsessMessage(event, true, false, playerId, trackCurrentTime, trackTotalTime,
									guild.getId());
						}
					} else if (message.length() >= (song.length() * 0.4)) {
						// message contains and equals 40% then send you are close
						if (song.replace(" ", "")
								.contains(message.replace(" ", "").replaceAll(Constants.HUMAN_ERROR_REGEX, ""))) {
							event.getChannel().sendMessage("You are close").queue();
						}
					} else if (message.length() > (song.length())) {
						// message contains to much then send you are close
						if (message.replace(" ", "").replaceAll(Constants.HUMAN_ERROR_REGEX, "")
								.contains(song.replace(" ", ""))) {
							event.getChannel().sendMessage("You are close").queue();
						}
					}

					// Check if author equals message
					for (String autor : autors) {
						if (autor.equals(message)) {
							sendSuccsessMessage(event, false, true, playerId, trackCurrentTime, trackTotalTime,
									guild.getId());
						}
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	// Reply to user if he guessed right
	private void sendSuccsessMessage(MessageReceivedEvent event, boolean guessdTrack, boolean guessAuthor,
			String playerId, long trackCurrentTime, long trackTotalTime, String guildId) {
		int points = (int) ((trackTotalTime - trackCurrentTime) / 1000);
		boolean gotPoints = new GamesConfigManager().updateRoundStatsForPlayer(playerId, guessdTrack, guessAuthor,
				points, guildId);
		if (gotPoints) {
			event.getChannel().sendMessage("<@" + playerId + "> thats right you got " + points + " points").queue();
		} else {
			event.getChannel().sendMessage("<@" + playerId + "> thats right but you have already recieved your points!")
					.queue();
		}
	}

	// Start a new round
	private void start(SlashCommandInteractionEvent event) {

		// Initialize new player and music manager
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);

		Guild guild = event.getGuild();
		String textChannelId = event.getChannel().getId();

		try {
			// Check if the bot is already playing
			if (guild.getAudioManager().isConnected()) {
				event.reply("`ERROR: You can't start another round!`").queue();
				return;
			} else {
				// Check if user is in a voice channel
				AudioChannel audioChannel = event.getMember().getVoiceState().getChannel();
				if (audioChannel != null) {
					// Connect to voice channel of the user
					VoiceChannel voiceChannel = guild.getVoiceChannelById(audioChannel.getId());
					connectToFirstVoiceChannel(guild.getAudioManager(), voiceChannel);
				} else {
					event.reply("`ERROR: You must be in a voice channel`").queue();
					return;
				}
			}

			// Split playlists into array
			String[] links = event.getOption("spotify-playlists").getAsString().replace(" ", "").split(",");
			int maxRounds = event.getOption("rounds").getAsInt();

			// Check provided links before calling them
			for (String link : links) {
				try {
					// Check if link is even a link if not Throw Exception
					new URI(link);

					if (!link.startsWith(Constants.SPOTIFY + Constants.PLAYLIST)) {
						event.reply("`ERROR: link must start with " + Constants.SPOTIFY + Constants.PLAYLIST + "...`")
								.queue();
						guild.getAudioManager().closeAudioConnection();
						return;
					}
				} catch (Exception e) {
					event.reply("`ERROR: " + e.getMessage() + "`").queue();
					return;
				}
			}

			event.reply("Setting up game...").queue();

			// Initialize the SpotifyAPI and crawl the playlists for the tracks
			SpotifyAPI api = new SpotifyAPI();
			List<Track> tracks = api.getTracks(links, maxRounds);

			// Stop round if spotify api responded with null due to
			// (ApiRateLimit|ApiError|ConnectionError|TrackNotFound)
			if (tracks == null) {
				guild.getTextChannelById(textChannelId).sendMessage("`ERROR: Could not load tracks!`").queue();
				cancel(event, false);
				guild.getAudioManager().closeAudioConnection();
				return;
			}

			// Initialize a new game in the games config file
			new GamesConfigManager().addNewGame(event, tracks.size());

			// Add every track to the q tracks.size varies from maxRounds to max found
			// tracks
			for (Track track : tracks) {
				playTrack(event, track);
			}
			guild.getTextChannelById(textChannelId).sendMessage("Finished loading tracks!").queue();
		} catch (Exception e) {
			guild.getTextChannelById(textChannelId).sendMessage("`ERROR: " + e.getMessage() + "`").queue();
			cancel(event, false);
		}
	}

	// Cancel a round
	private void cancel(SlashCommandInteractionEvent event, boolean reply) {
		try {
			Guild guild = event.getGuild();
			GuildMusicManager musicManager = getGuildAudioPlayer(guild);
			musicManager.scheduler.clearQueue();
			musicManager.player.destroy();
			if (reply) {
				event.reply("Canceled!").queue();
			}
		} catch (Exception e) {
			event.reply("`Error: Cancel did not work!`").queue();
			System.out.println(e.getMessage());
		}
	}

	private void playTrack(SlashCommandInteractionEvent event, Track track) {
		Guild guild = event.getGuild();
		Member member = event.getMember();
		AudioChannel audioChannel = member.getVoiceState().getChannel();
		VoiceChannel voiceChannel = guild.getVoiceChannelById(audioChannel.getId());

		GuildMusicManager musicManager = getGuildAudioPlayer(guild);

		String author = String.join(",", track.getArtists());
		AudioReference auduoReference = new AudioReference(track.getMp3(), track.getTrack() + ";" + author);

		playerManager.loadItemOrdered(musicManager, auduoReference, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack audioTrack) {
				play(guild, musicManager, audioTrack, voiceChannel);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack firstTrack = playlist.getSelectedTrack();

				if (firstTrack == null) {
					firstTrack = playlist.getTracks().get(0);
				}
				play(guild, musicManager, firstTrack, voiceChannel);
			}

			@Override
			public void noMatches() {
				event.getChannel().sendMessage("Nothing found by " + track.getMp3()).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				event.getChannel().sendMessage("Could not play: " + track.getMp3() + " " + exception.getMessage())
						.queue();
			}
		});
	}

	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, VoiceChannel voiceChannel) {
		try {
			musicManager.scheduler.queue(track);
			musicManager.player.setVolume(10);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static void connectToFirstVoiceChannel(AudioManager audioManager, VoiceChannel voiceChannel) {
		if (!audioManager.isConnected()) {
			audioManager.openAudioConnection(voiceChannel);
		}
	}

	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager, guild);
			musicManagers.put(guildId, musicManager);
		}

		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}
}
