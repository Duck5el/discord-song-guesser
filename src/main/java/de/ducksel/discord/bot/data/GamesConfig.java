package de.ducksel.discord.bot.data;

import java.util.List;

public class GamesConfig {
	private List<Game> games;

	public Game getGameById(String guildId) {
		for (int i = 0; i < games.size(); i++) {
			if (games.get(i).getId().equals(guildId)) {
				return games.get(i);
			}
		}
		return null;
	}

	public List<Game> getGames() {
		return games;
	}

	public void setGames(List<Game> games) {
		this.games = games;
	}

}
