package org.cool.chatty.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class ChatClient extends Application {

    private String address;
    private int port;
    private String username;

    // Connection
    private Socket connectionToServer;
    private BufferedReader fromServerReader;
    private PrintWriter toServerWriter;

    // GUI Elements
    private VBox messageContainer;
    private TextField inputTextField;
    private VBox participantsContainer;
    private Set<String> participants;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        participants = new HashSet<>();

        // Dialog mit modernem Layout
        Dialog<Pair<String, Pair<String, String>>> dialog = new Dialog<>();
        dialog.setTitle("Chatty - Verbinden");
        dialog.setHeaderText(null); // Kein separater Header

        // Buttons
        ButtonType connectButtonType = new ButtonType("Verbinden", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Hauptlayout des Dialogs
        VBox dialogContent = new VBox(20);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setStyle("-fx-background-color: #f4f4f4; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Schlüssel-Symbol
        Label keyIcon = new Label("🔑");
        keyIcon.setFont(Font.font("Segoe UI Emoji", 50));
        keyIcon.setTextFill(Color.DARKBLUE);

        // Titel des Dialogs
        Label titleLabel = new Label("Willkommen bei Chatty!");
        titleLabel.setFont(Font.font("Segoe UI", 20));
        titleLabel.setTextFill(Color.DARKBLUE);

        // Benutzername Eingabefeld
        HBox usernameBox = new HBox(10);
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        Label usernameIcon = new Label("👤");
        usernameIcon.setFont(Font.font("Segoe UI Emoji", 24));
        TextField usernameField = new TextField();
        usernameField.setPromptText("Benutzername eingeben");
        usernameField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        usernameBox.getChildren().addAll(usernameIcon, usernameField);

        // IP-Adresse Eingabefeld
        HBox ipBox = new HBox(10);
        ipBox.setAlignment(Pos.CENTER_LEFT);
        Label ipIcon = new Label("🌐");
        ipIcon.setFont(Font.font("Segoe UI Emoji", 24));
        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server-IP-Adresse eingeben");
        ipField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        ipBox.getChildren().addAll(ipIcon, ipField);

        // Port Eingabefeld
        HBox portBox = new HBox(10);
        portBox.setAlignment(Pos.CENTER_LEFT);
        Label portIcon = new Label("🔌");
        portIcon.setFont(Font.font("Segoe UI Emoji", 24));
        TextField portField = new TextField("3141");
        portField.setPromptText("Port eingeben");
        portField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        portBox.getChildren().addAll(portIcon, portField);

        // Hinweis
        Label hintLabel = new Label("Bitte geben Sie Ihren Benutzernamen, die Server-IP-Adresse und den Port ein.");
        hintLabel.setFont(Font.font("Segoe UI", 14));
        hintLabel.setTextFill(Color.GRAY);

        // Zusammenfügen der Inhalte
        dialogContent.getChildren().addAll(keyIcon, titleLabel, hintLabel, usernameBox, ipBox, portBox);

        // Dialog-Inhalt setzen
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setStyle("-fx-border-radius: 10; -fx-background-radius: 10;");

        // Ergebnis verarbeiten
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new Pair<>(usernameField.getText(), new Pair<>(ipField.getText(), portField.getText()));
            }
            return null;
        });

        // Dialog anzeigen
        dialog.showAndWait().ifPresent(result -> {
            username = result.getKey();
            address = result.getValue().getKey();
            try {
                port = Integer.parseInt(result.getValue().getValue());
            } catch (NumberFormatException e) {
                showError("Ungültiger Port: " + result.getValue().getValue());
                return;
            }
            setupConnection();
        });

        // Hauptfenster
        primaryStage.setTitle("Chatty - Willkommen, " + (username != null ? username : "Gast"));
        primaryStage.setScene(createScene());
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }
}
