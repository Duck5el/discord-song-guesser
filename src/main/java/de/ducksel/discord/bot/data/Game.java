package de.ducksel.discord.bot.data;

import java.util.ArrayList;
import java.util.List;

public class Game {
	private String id;
	private String textChannelId;
	private List<Player> players = new ArrayList<>();
	private int totalRounds;
	private List<Round> rounds = new ArrayList<>();

	public Round getRoundByNr(int roundNr) {
		for (int i = 0; i < rounds.size(); i++) {
			if (rounds.get(i).getRoundNr() == roundNr) {
				return rounds.get(i);
			}
		}
		return null;
	};

	public Player getPlayerById(String playerId) {
		for (Player player : players) {
			if (player.getId().equals(playerId)) {
				return player;
			}
		}
		return null;
	}

	public void addPlayer(Player player) {
		this.players.add(player);
	}

	public void addRound(Round round) {
		this.rounds.add(round);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	public List<Round> getRounds() {
		return rounds;
	}

	public void setRounds(List<Round> rounds) {
		this.rounds = rounds;
	}

	public int getTotalRounds() {
		return totalRounds;
	}

	public void setTotalRounds(int totalRounds) {
		this.totalRounds = totalRounds;
	}

	public String getTextChannelId() {
		return textChannelId;
	}

	public void setTextChannelId(String textChannelId) {
		this.textChannelId = textChannelId;
	}

}
