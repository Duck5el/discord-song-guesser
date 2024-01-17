package de.ducksel.discord.bot.io;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ducksel.discord.bot.data.GamesConfig;

public class ReadGamesConfig {
	public GamesConfig readFileAsString(String path) {
		ObjectMapper objectMapper = new ObjectMapper();
		GamesConfig gamesConfig = null;
		try {
			String fileAsString = new String(Files.readAllBytes(Paths.get(path)));
			gamesConfig = objectMapper.readValue(fileAsString, GamesConfig.class);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return gamesConfig;
	}
}
