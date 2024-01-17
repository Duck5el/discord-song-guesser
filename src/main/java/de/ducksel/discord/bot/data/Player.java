package de.ducksel.discord.bot.data;

public class Player {
	private String id;
	private String privateTextChannel;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPrivateTextChannel() {
		return privateTextChannel;
	}

	public void setPrivateTextChannel(String privateTextChannel) {
		this.privateTextChannel = privateTextChannel;
	}

}
