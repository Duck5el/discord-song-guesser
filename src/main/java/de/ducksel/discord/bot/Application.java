package de.ducksel.discord.bot;

import de.ducksel.discord.bot.data.Constants;
import de.ducksel.discord.bot.io.OverwriteGamesConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Application {
	public static void main(String[] args) {
		new Application();
	}

	public Application() {
		try {
			try {
				new OverwriteGamesConfig().writeToFile(System.getProperty(Constants.GAMES_CONFIG), "{\"games\":[]}");
				JDA builder = JDABuilder.createDefault(System.getProperty(Constants.BOT_TOKEN))
						.setChunkingFilter(ChunkingFilter.ALL) // enable member chunking for all guilds
						.setMemberCachePolicy(MemberCachePolicy.ALL) // ignored if chunking is enabled
						.enableIntents(GatewayIntent.GUILD_MEMBERS).addEventListeners(new CommandListener()).build();
				builder.awaitReady();
//				deleteSlashCommand(builder);
				registrateSlashCommands(builder);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void registrateSlashCommands(JDA builder) {
		builder.upsertCommand("start", "Start a game of SongGuesser")
				.addOption(OptionType.STRING, "spotify-playlists", "Use comma separated links", true)
				.addOption(OptionType.INTEGER, "rounds", "How many rounds do you want to play", true).complete();
		builder.upsertCommand("cancel", "Cancel a round").complete();
	}

	public void deleteSlashCommand(JDA builder) {
		builder.updateCommands().complete();
	}
}
