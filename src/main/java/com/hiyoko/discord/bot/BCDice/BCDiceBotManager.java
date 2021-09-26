package com.hiyoko.discord.bot.BCDice;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class BCDiceBotManager {
	/** Discordボットのトークン。 */
	private final String token;
	/** BCDice APIのURL。 */
	private final String bcdiceUrl;
	/** エラーをログに出力するか。 */
	private final boolean errorSensitive;
	/** パスワード。 */
	private final String password;

	/** ボットオブジェクト格納時に使用するロック。 */
	private final Object botLock = new Object();
	/** BCDiceボット。 */
	private StoppableBCDiceBot bot = null;
	/** ボットの実行結果が格納されるFuture。 */
	private CompletableFuture<Void> botFuture = null;
	/** 切断中かどうか。 */
	private boolean disconnecting = false;

	/**
	 * @param token Discordボットのトークン。
	 * @param bcdiceUrl BCDice APIのURL。
	 * @param errorSensitive エラーをログに出力するか。
	 * @param password パスワード。
	 */
	public BCDiceBotManager(String token, String bcdiceUrl, boolean errorSensitive, String password) {
		this.token = token;
		this.bcdiceUrl = bcdiceUrl;
		this.errorSensitive = errorSensitive;
		this.password = password;
	}

	/** 格納されていたボットをクリアする。 */
	private void clearBot() {
		bot = null;
		botFuture = null;
		disconnecting = false;
	}

	/**
	 * @return ボットを開始可能か。
	 */
	public boolean canStart() {
		return bot == null && botFuture == null;
	}

	/**
	 * @return ボットを停止可能か。
	 */
	public boolean canStop() {
		return bot != null && botFuture != null && !botFuture.isDone() && !disconnecting;
	}

	/**
	 * ボットを開始する。
	 * @return ボットが開始されたか。
	 */
	public boolean start() {
		if (!canStart()) {
			return false;
		}

		clearBot();

		synchronized (botLock) {
			bot = new StoppableBCDiceBot(token, bcdiceUrl, errorSensitive, password);
			try {
				botFuture = bot
						.Execute()
						.exceptionally(e -> {
							bot.logger.error(String.format("Could not log in to Discord: %s", e.getMessage()));

							synchronized (botLock) {
								clearBot();
							}

							return e;
						})
						.thenAccept(o -> {
							bot.logger.info("Disconnected.");

							synchronized (botLock) {
								clearBot();
							}
						});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Bot is running...");

		return true;
	}

	/**
	 * ボットを停止する。
	 * @return ボットが停止されたか。
	 */
	public boolean stop() {
		synchronized (botLock) {
			if (!canStop()) {
				return false;
			}
		}

		bot.disconnect();

		return true;
	}

	/**
	 * ボットを停止し、完了するまで待つ。
	 * @return ボットが停止されたか。
	 */
	public boolean stopAndWait() {
		boolean didStop = stop();
		if (!didStop || botFuture == null) {
			return false;
		}

		botFuture.join();

		return true;
	}
}
