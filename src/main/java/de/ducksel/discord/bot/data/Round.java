package de.ducksel.discord.bot.data;

import java.util.ArrayList;
import java.util.List;

public class Round {
	private int roundNr;
	private String track;
	private List<String> authors = new ArrayList<>();
	private List<PlayerStat> playerStats = new ArrayList<>();

	public PlayerStat getPlayerById(String id) {
		for (int i = 0; i < playerStats.size(); i++) {
			if (playerStats.get(i).getPlayerId().equals(id)) {
				return playerStats.get(i);
			}
		}
		return null;
	}

	public void addStat(PlayerStat stat) {
		this.playerStats.add(stat);
	}

	public int getRoundNr() {
		return roundNr;
	}

	public void setRoundNr(int roundNr) {
		this.roundNr = roundNr;
	}

	public List<PlayerStat> getPlayerStats() {
		return playerStats;
	}

	public void setPlayerStats(List<PlayerStat> playerStats) {
		this.playerStats = playerStats;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

}
