package de.ducksel.discord.bot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import de.ducksel.discord.bot.data.Game;
import de.ducksel.discord.bot.data.PlayerStat;
import de.ducksel.discord.bot.data.Round;
import de.ducksel.discord.bot.io.GamesConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class TrackScheduler extends AudioEventAdapter {

	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;
	private final Guild guild;

	public TrackScheduler(AudioPlayer player, Guild guild) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
		this.guild = guild;
	}

	public void queue(AudioTrack track) {
		if (!player.startTrack(track, true)) {
			queue.offer(track);
		} else {
			addNextRound(true);
		}
	}

	public void nextTrack() {
		try {
			Thread.sleep(3000);
			player.startTrack(queue.poll(), false);
			addNextRound(false);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void addNextRound(boolean first) {
		String trackInfo = player.getPlayingTrack().getInfo().title;
		String guildId = guild.getId();

		int maxTotalRounds = new GamesConfigManager().getTotalRounds(guildId);
		int round = maxTotalRounds - queue.size();

		if (first) {
			round = 1;
		}

		String track = trackInfo.split(";")[0];
		List<String> authors = Arrays.asList(trackInfo.split(";")[1].split(","));

		new GamesConfigManager().addNextRound(round, track, authors, guildId);
	}

	public void clearQueue() {
		queue.clear();
		new GamesConfigManager().removeGameFromConfig(guild.getId());
	}

	private void sendRoundReport() {
		try {
			String guildId = guild.getId();
			int maxTotalRounds = new GamesConfigManager().getTotalRounds(guildId);
			int roundNr = maxTotalRounds - queue.size();
			Game game = new GamesConfigManager().getGame(guildId);
			Round round = game.getRoundByNr(roundNr);

			List<String> lines = new ArrayList<>();
			List<String> privateChannels = new ArrayList<>();
			for (PlayerStat stat : round.getPlayerStats()) {
				String line = guild.getMemberById(stat.getPlayerId()).getUser().getAsMention() + " Song: "
						+ (stat.getGuessedTrack() ? ":white_check_mark:" : ":x:") + " Author: "
						+ (stat.getGuessedAuthor() ? ":white_check_mark:" : ":x:") + " Points: "
						+ stat.getTotalPoints();
				lines.add(line);
				privateChannels.add(game.getPlayerById(stat.getPlayerId()).getPrivateTextChannel());
			}

			sendEmbed("Round: " + roundNr + "/" + maxTotalRounds,
					"Song: " + round.getTrack() + "\nAuthor/s: "
							+ round.getAuthors().toString().replace("[", "").replace("]", ""),
					Color.MAGENTA, lines, game.getTextChannelId(), privateChannels);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void sendFinalReport() {
		String guildId = guild.getId();
		int maxTotalRounds = new GamesConfigManager().getTotalRounds(guildId);
		Game game = new GamesConfigManager().getGame(guildId);
		Round round = game.getRoundByNr(maxTotalRounds);

		List<String> lines = new ArrayList<>();
		List<PlayerStat> stats = round.getPlayerStats();
		List<String> privateChannels = new ArrayList<>();
		Collections.sort(stats, Comparator.comparing(PlayerStat::getTotalPoints).reversed());
		for (PlayerStat stat : stats) {
			String line = guild.getMemberById(stat.getPlayerId()).getUser().getAsMention() + " Points: "
					+ stat.getTotalPoints();
			privateChannels.add(game.getPlayerById(stat.getPlayerId()).getPrivateTextChannel());
			lines.add(line);
		}
		sendEmbed("===== :trophy::trophy::trophy: Final Stats :trophy::trophy::trophy: =====", "", Color.YELLOW, lines,
				game.getTextChannelId(), privateChannels);
	}

	private void sendEmbed(String title, String description, Color color, List<String> lines, String mainChannel,
			List<String> privateChannels) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setFooter("Bot by: Duck#4303");
		embed.setColor(color);
		embed.setTitle(title);
		embed.setDescription(description);

		embed.addField("Results:", String.join("\n", lines), false);

		if (privateChannels != null) {
			for (String privateChannel : privateChannels) {
				JDA jda = guild.getJDA();
				jda.getPrivateChannelById(privateChannel).sendMessageEmbeds(embed.build()).queue();
			}
		}
		guild.getTextChannelById(mainChannel).sendMessageEmbeds(embed.build()).queue();

	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		sendRoundReport();
		if (endReason.mayStartNext && !queue.isEmpty()) {
			nextTrack();
		} else {
			if (guild.getAudioManager().isConnected()) {
				guild.getAudioManager().closeAudioConnection();
			}
			sendFinalReport();
			new GamesConfigManager().removeGameFromConfig(guild.getId());
		}
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
	}
}
