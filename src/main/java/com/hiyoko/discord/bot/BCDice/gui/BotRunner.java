package com.hiyoko.discord.bot.BCDice.gui;

import com.hiyoko.discord.bot.BCDice.StoppableBCDiceBot;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.javacord.api.event.Event;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class BotRunner implements Initializable {
	public record SceneAndController(Scene scene, BotRunner controller) {
	}

	enum BotConnectionState {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
	}

	public static SceneAndController getScene() throws IOException {
		var fxmlLoader = new FXMLLoader(BotRunner.class.getResource("/gui/BotRunner.fxml"));
		Parent parent = fxmlLoader.load();

		BotRunner controller = fxmlLoader.getController();

		var scene = new Scene(parent);
		String commonCss = Objects.requireNonNull(BotRunner.class.getResource("/gui/common.css")).toExternalForm();
		scene.getStylesheets().add(commonCss);

		return new SceneAndController(scene, controller);
	}

	final private BotExecuteCallbacks botExecuteCallbacks = new BotExecuteCallbacks(this);

	private StoppableBCDiceBot bot = null;

	final private StringProperty token = new SimpleStringProperty(this, "token", "");
	final private StringProperty bcdiceUrl = new SimpleStringProperty(this, "bcdiceUrl", "");
	final private StringProperty password = new SimpleStringProperty(this, "password", "");
	final private BooleanProperty ignoreError = new SimpleBooleanProperty(this, "ignoreError", false);

	final private ObjectProperty<BotConnectionState> botConnectionState = new SimpleObjectProperty<>(this, "botConnectionState", BotConnectionState.DISCONNECTED);

	private boolean disconnectingOnExit = false;

	@FXML
	private Label tokenLabel;

	@FXML
	private TextField tokenTextField;

	@FXML
	private Label bcdiceUrlLabel;

	@FXML
	private TextField bcdiceUrlTextField;

	@FXML
	private CheckBox ignoreErrorCheckBox;

	@FXML
	private Label passwordLabel;

	@FXML
	private TextField passwordTextField;

	@FXML
	private Button connectDisconnectButton;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		tokenTextField.textProperty().bindBidirectional(token);
		bcdiceUrlTextField.textProperty().bindBidirectional(bcdiceUrl);
		passwordTextField.textProperty().bindBidirectional(password);
		ignoreErrorCheckBox.selectedProperty().bindBidirectional(ignoreError);

		bindConnectDisconnectButtonToBotConnectionStateProperty();
	}

	private void bindConnectDisconnectButtonToBotConnectionStateProperty() {
		connectDisconnectButton.textProperty().bind(new StringBinding() {
			{
				bind(botConnectionState);
			}

			@Override
			protected String computeValue() {
				return switch (botConnectionState.get()) {
					case DISCONNECTED, CONNECTING -> "接続";
					case CONNECTED, DISCONNECTING -> "切断";
				};
			}
		});

		connectDisconnectButton.disableProperty().bind(new BooleanBinding() {
			{
				bind(botConnectionState);
			}

			@Override
			protected boolean computeValue() {
				return botConnectionState.get() == BotConnectionState.CONNECTING;
			}
		});
	}

	public void setToken(String value) {
		if (Platform.isFxApplicationThread()) {
			token.set(value);
		} else {
			Platform.runLater(() -> token.set(value));
		}
	}

	public void setBcdiceUrl(String value) {
		if (Platform.isFxApplicationThread()) {
			bcdiceUrl.set(value);
		} else {
			Platform.runLater(() -> bcdiceUrl.set(value));
		}
	}

	public void setPassword(String value) {
		if (Platform.isFxApplicationThread()) {
			password.set(value);
		} else {
			Platform.runLater(() -> password.set(value));
		}
	}

	public void setIgnoreError(boolean value) {
		if (Platform.isFxApplicationThread()) {
			ignoreError.set(value);
		} else {
			Platform.runLater(() -> ignoreError.set(value));
		}
	}

	public boolean botIsDisconnected() {
		return botConnectionState.get() == BotConnectionState.DISCONNECTED;
	}

	public boolean disconnect() {
		if (bot == null) {
			return false;
		}

		disconnectingOnExit = true;
		bot.disconnect();

		return true;
	}

	private void setBotConnectionState(BotConnectionState value) {
		if (Platform.isFxApplicationThread()) {
			botConnectionState.set(value);
		} else {
			Platform.runLater(() -> botConnectionState.set(value));
		}
	}

	@FXML
	private void connectDisconnectButtonOnAction(ActionEvent event) {
		switch (botConnectionState.get()) {
			case DISCONNECTED -> connectDisconnectButtonOnActionOnDisconnected();
			case CONNECTED -> connectDisconnectButtonOnActionOnConnected();
		}
	}

	private void connectDisconnectButtonOnActionOnDisconnected() {
		botConnectionState.set(BotConnectionState.CONNECTING);

		bot = new StoppableBCDiceBot(token.get(), bcdiceUrl.get(), !ignoreError.get(), password.get());

		try {
			bot.executeAndNotifyLoggedIn(botExecuteCallbacks);
		} catch (IOException e) {
			botConnectionState.set(BotConnectionState.DISCONNECTED);

			var alert = new Alert(
					Alert.AlertType.ERROR,
					"設定および Discord サーバの状態を確認してください。",
					ButtonType.OK
			);
			alert.setTitle("ログイン失敗");
			alert.setHeaderText("ログイン失敗: " + e.getMessage());

			alert.showAndWait();
		}
	}

	private void connectDisconnectButtonOnActionOnConnected() {
		botConnectionState.set(BotConnectionState.DISCONNECTING);
		bot.disconnect();
	}

	static class BotExecuteCallbacks implements StoppableBCDiceBot.BotExecuteCallbacks {
		private final BotRunner botRunner;

		public BotExecuteCallbacks(BotRunner botRunner) {
			this.botRunner = botRunner;
		}

		@Override
		public void onLogin() {
			botRunner.setBotConnectionState(BotConnectionState.CONNECTED);
		}

		@Override
		public void onLoginFailure(Throwable e) {
			botRunner.setBotConnectionState(BotConnectionState.DISCONNECTED);

			Platform.runLater(() -> {
				var alert = new Alert(
						Alert.AlertType.ERROR,
						"設定および Discord サーバの状態を確認してください。",
						ButtonType.OK
				);
				alert.setTitle("ログイン失敗");
				alert.setHeaderText("ログイン失敗: " + e.getMessage());

				alert.showAndWait();
			});
		}

		@Override
		public void onDisconnect(Event event) {
			botRunner.setBotConnectionState(BotConnectionState.DISCONNECTED);

			if (botRunner.disconnectingOnExit) {
				return;
			}

			Platform.runLater(() -> {
				var alert = new Alert(
						Alert.AlertType.INFORMATION,
						"Discord サーバから切断されました。",
						ButtonType.OK
				);
				alert.setTitle("接続切断");
				alert.setHeaderText(null);

				alert.showAndWait();
			});
		}
	}
}
