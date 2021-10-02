package com.hiyoko.discord.bot.BCDice;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import com.hiyoko.discord.bot.BCDice.gui.BotRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BCDiceBotGUI extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		BotRunner.SceneAndController sceneAndController = BotRunner.getScene();

		Scene botRunnerScene = sceneAndController.scene();
		BotRunner botRunnerController = sceneAndController.controller();

		List<String> parameters = getParameters().getUnnamed();

		botRunnerController.setToken(parameters.get(0));
		botRunnerController.setBcdiceUrl(parameters.get(1));
		String adminPassword = AdminPasswordGenerator.getPassword();
		botRunnerController.setPassword(adminPassword);
		botRunnerController.setIgnoreError(parameters.size() >= 3 && !parameters.get(2).equals("0"));

		stage.setScene(botRunnerScene);
		stage.setTitle("discord-bcdicebot");
		stage.sizeToScene();

		stage.setOnCloseRequest(event -> {
			if (botRunnerController.botIsDisconnected()) {
				return;
			}

			var alert = new Alert(Alert.AlertType.INFORMATION, "ボットの接続を切断しますか?", ButtonType.YES, ButtonType.NO);
			alert.setTitle("確認");
			alert.setHeaderText(null);

			Optional<ButtonType> result = alert.showAndWait();
			result.ifPresentOrElse(r -> {
				if (r == ButtonType.NO) {
					event.consume();
					return;
				}

				botRunnerController.disconnect();
			}, event::consume);
		});

		stage.show();

		stage.setMinWidth(stage.getWidth());
		stage.setMinHeight(stage.getHeight());
	}

	private static String getVersion() {
		String version = BCDiceBot.class.getPackage().getImplementationVersion();
		if(version == null) {
			return "See pom.xml file";
		} else {
			return version;
		}
	}

	public static void main(String[] args) {
		final var helpArgs = new String[] {"help", "--help", "--h", "-h"};

		if (args.length < 2 || args.length > 3 || Arrays.asList(helpArgs).contains(args[0])) {
			System.out.println("Discord-BCDicebot Version " + getVersion());
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

		launch(Arrays.stream(args).map(String::trim).toArray(String[]::new));
	}
}
