package com.hiyoko.discord.bot.BCDice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * First kicked class for discord-bcdicebot.
 * It creates two instances, DiscordAPI client and BCDice client.
 * @author @Shunshun94
 */
public abstract class StoppableBCDiceBotCLI {
	/**
	 * First called method.
	 * @param args command line parameters. 1st should be Discord bot token. 2nd should be the URL of BCDice-API.
	 */
	public static void main(String[] args) {
		if( args.length < 2 || args[0].equals("help") ||
			args[0].equals("--help") || args[0].equals("--h") || args[0].equals("-h")) {
			System.out.println("Discord-BCDicebot Version " + StoppableBCDiceBot.getVersion());
			System.out.println("This application requires two params");
			System.out.println("  1. Discord Bot Token");
			System.out.println("  2. BCDice-api server URL");
			System.out.println("  3. (Optional) Error Handling Flag, When BCDice-API returns Error, If an error message should be sent, it's 0. If not, it's 1.");
			System.out.println("------------------------------------");
			System.out.println("2つコマンドライン引数が必要です");
			System.out.println("  1. Discord の bot token");
			System.out.println("  2. BCDice-api の URL");
			System.out.println("  3. (必要ならば) エラーハンドルフラグ。BCDice-API でエラー発生時にエラーメッセージを出力するなら0 しないなら1");

			return;
		}

		String password = AdminPasswordGenerator.getPassword();

		boolean errorSensitive = args.length < 3 || args[2].trim().equals("0");
		var botManager = new BCDiceBotManager(args[0].trim(), args[1].trim(), errorSensitive, password);

		Runtime.getRuntime().addShutdownHook(new Thread(botManager::stopAndWait));

		BufferedReader inBufferedReader = new BufferedReader(new InputStreamReader(System.in));
		boolean terminated = false;
		while (!terminated) {
			try {
				System.out.print("Please type command: ");
				String line = inBufferedReader.readLine();

				switch (line.trim()) {
					case "":
						break;
					case "exit":
						terminated = true;
						break;
					case "start":
						botManager.start();
						break;
					case "stop":
						botManager.stop();
						break;
					default:
						System.out.println("Unknown command.");
						break;
				}
			} catch (IOException e) {
				break;
			}
		}

		botManager.stopAndWait();
	}
}
