package de.ducksel.discord.bot.data;

public class PlayerStat {
	private String playerId;
	private boolean guessedTrack;
	private boolean guessedAuthor;
	private int totalPoints;

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public boolean getGuessedTrack() {
		return guessedTrack;
	}

	public void setGuessedTrack(boolean guessedTrack) {
		this.guessedTrack = guessedTrack;
	}

	public boolean getGuessedAuthor() {
		return guessedAuthor;
	}

	public void setGuessedAuthor(boolean guessdAuthor) {
		this.guessedAuthor = guessdAuthor;
	}

	public int getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(int totalPoints) {
		this.totalPoints = totalPoints;
	}

}
