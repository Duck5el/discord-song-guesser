package de.ducksel.discord.bot.io;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import de.ducksel.discord.bot.data.Constants;
import de.ducksel.discord.bot.data.Game;
import de.ducksel.discord.bot.data.GamesConfig;
import de.ducksel.discord.bot.data.Player;
import de.ducksel.discord.bot.data.PlayerStat;
import de.ducksel.discord.bot.data.Round;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

public class GamesConfigManager {

	//Path to the games configuration file
	private final String gamesConfigPath = System.getProperty(Constants.GAMES_CONFIG);

	// Initialize a new game in the games array of the games configuration
	public void addNewGame(SlashCommandInteractionEvent event, int totalRounds) {
		try {
			AudioChannel audioChannel = event.getMember().getVoiceState().getChannel();
			VoiceChannel voiceChannel = event.getGuild().getVoiceChannelById(audioChannel.getId());

			GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);

			Game game = new Game();
			game.setId(event.getGuild().getId());
			game.setTextChannelId(event.getChannelId());
			game.setTotalRounds(totalRounds);
			List<Member> memebers = voiceChannel.getMembers();

			for (Member member : memebers) {
				if (!member.getId().equals(event.getJDA().getSelfUser().getId())) {
					Player player = new Player();
					String id = member.getId();
					CacheRestAction<PrivateChannel> await = member.getUser().openPrivateChannel();
					await.queue(channel -> {
						player.setPrivateTextChannel(channel.getId());
					});
					await.complete();
					player.setId(id);
					game.addPlayer(player);
				}
			}
			List<Game> newGame = new ArrayList<>();
			for (Game configGame : gamesConfig.getGames()) {
				if (!configGame.getId().equals(game.getId())) {
					newGame.add(configGame);
				}
			}
			newGame.add(game);
			gamesConfig.setGames(newGame);
			JSONObject json = new JSONObject(gamesConfig);
			new OverwriteGamesConfig().writeToFile(gamesConfigPath, json.toString());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	// Get a game by the guild id
	public Game getGame(String guildId) {
		return new ReadGamesConfig().readFileAsString(gamesConfigPath).getGameById(guildId);
	}

	// Remove saved config form a game after cancel or after the game is over
	public void removeGameFromConfig(String guildId) {
		List<Game> games = new ReadGamesConfig().readFileAsString(gamesConfigPath).getGames();
		List<Game> newConfig = new ArrayList<>();

		for (Game game : games) {
			if (!game.getId().equals(guildId)) {
				newConfig.add(game);
			}
		}
		GamesConfig gamesConfig = new GamesConfig();
		gamesConfig.setGames(newConfig);
		JSONObject json = new JSONObject(gamesConfig);
		new OverwriteGamesConfig().writeToFile(gamesConfigPath, json.toString());
	}

	// Get the guild id of a currently playing player
	public String getGameOfPlayer(String playerId) {
		List<Game> games = new ReadGamesConfig().readFileAsString(gamesConfigPath).getGames();
		for (Game game : games) {
			if (game.getPlayers().stream().anyMatch(p -> p.getId().equals(playerId))) {
				return game.getId();
			}
		}
		return null;
	}

	// Get the amount of rounds of a guild
	public int getTotalRounds(String guildId) {
		GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);
		for (Game game : gamesConfig.getGames()) {
			if (game.getId().equals(guildId)) {
				return game.getTotalRounds();
			}
		}
		return -1;
	}

	// Get a specific round of a guild
	public Round getRound(int roundNr, String guildId) {
		GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);
		return gamesConfig.getGameById(guildId).getRoundByNr(roundNr);
	}

	// Add new track and default statistics to the rounds array
	public void addNextRound(int roundNr, String track, List<String> authors, String guildId) {
		GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);
		Round round = new Round();
		round.setAuthors(authors);
		round.setTrack(track);
		round.setRoundNr(roundNr);
		Game game = gamesConfig.getGameById(guildId);
		for (Player player : game.getPlayers()) {
			PlayerStat stat = new PlayerStat();
			stat.setGuessedAuthor(false);
			stat.setGuessedTrack(false);
			if (roundNr != 1) {
				stat.setTotalPoints(game.getRoundByNr(roundNr - 1).getPlayerById(player.getId()).getTotalPoints());
			} else {
				stat.setTotalPoints(0);
			}
			stat.setPlayerId(player.getId());
			round.addStat(stat);
		}
		game.addRound(round);
		JSONObject json = new JSONObject(gamesConfig);
		new OverwriteGamesConfig().writeToFile(gamesConfigPath, json.toString());
	}

	// Update statistics of a player if he has guessed a track or author right
	public boolean updateRoundStatsForPlayer(String playerId, boolean guessedTrack, boolean guessedAuthor,
			int additionalPoints, String guildId) {
		boolean gotPoints = false;
		GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);
		Game game = gamesConfig.getGameById(guildId);
		Round round = game.getRoundByNr(getCurrntRoundNr(guildId));
		if (guessedAuthor && !round.getPlayerById(playerId).getGuessedAuthor()) {
			PlayerStat stat = round.getPlayerById(playerId);
			stat.setGuessedAuthor(true);
			stat.setTotalPoints(additionalPoints + stat.getTotalPoints());
			gotPoints = true;
		}
		if (guessedTrack && !round.getPlayerById(playerId).getGuessedTrack()) {
			PlayerStat stat = round.getPlayerById(playerId);
			stat.setGuessedTrack(true);
			stat.setTotalPoints(additionalPoints + stat.getTotalPoints());
			gotPoints = true;
		}
		JSONObject json = new JSONObject(gamesConfig);
		new OverwriteGamesConfig().writeToFile(gamesConfigPath, json.toString());
		return gotPoints;
	}

	// Returns an integer of the current round number
	private int getCurrntRoundNr(String guildId) {
		GamesConfig gamesConfig = new ReadGamesConfig().readFileAsString(gamesConfigPath);
		Game game = gamesConfig.getGameById(guildId);
		return game.getRounds().size();
	}
}
