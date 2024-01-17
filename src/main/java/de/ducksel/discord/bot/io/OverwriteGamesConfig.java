package de.ducksel.discord.bot.io;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class OverwriteGamesConfig {
	public void writeToFile(String path, String content) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			writer.write(content);
			writer.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}