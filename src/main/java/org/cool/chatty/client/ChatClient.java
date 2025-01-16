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

/**
 * Die Hauptklasse des Chat-Clients.
 * Diese Klasse verbindet den Benutzer mit dem Server, stellt die Benutzeroberfläche bereit
 * und ermöglicht die Kommunikation mit anderen Benutzern im Chat.
 */
public class ChatClient extends Application {

    // Server-Adresse und Portnummer
    private String address; // Die IP-Adresse des Servers
    private int port; // Der Port des Servers
    private String username; // Der Benutzername des aktuellen Benutzers

    // Verbindungselemente
    private Socket connectionToServer; // Verbindung zum Server
    private BufferedReader fromServerReader; // Leser für eingehende Nachrichten
    private PrintWriter toServerWriter; // Schreiber für ausgehende Nachrichten

    // GUI-Elemente
    private VBox messageContainer; // Container für Nachrichten
    private TextField inputTextField; // Eingabefeld für Nachrichten
    private VBox participantsContainer; // Container für Teilnehmerliste
    private Set<String> participants; // Liste der aktuellen Teilnehmer

    /**
     * Hauptmethode der Anwendung.
     * Startet die JavaFX-Anwendung.
     * @param args Argumente der Kommandozeile (nicht verwendet).
     */
    public static void main(String[] args) {
        launch(args); // Startet die JavaFX-Anwendung
    }

    @Override
    public void start(Stage primaryStage) {
        participants = new HashSet<>(); // Initialisierung der Teilnehmerliste

        // Erstellen eines Dialogs für Verbindungsinformationen
        Dialog<Pair<String, Pair<String, String>>> dialog = new Dialog<>();
        dialog.setTitle("Chatty - Verbinden");
        dialog.setHeaderText(null); // Kein zusätzlicher Header

        // Verbindungs-Button hinzufügen
        ButtonType connectButtonType = new ButtonType("Verbinden", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Layout des Dialogs
        VBox dialogContent = new VBox(20); // Vertikales Layout mit 20px Abstand
        dialogContent.setAlignment(Pos.CENTER); // Zentrierte Inhalte
        dialogContent.setPadding(new Insets(20)); // Innenabstand
        dialogContent.setStyle("-fx-background-color: #f4f4f4; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Symbol für den Dialog
        Label keyIcon = new Label("🔑");
        keyIcon.setFont(Font.font("Segoe UI Emoji", 50)); // Schriftgröße und Emoji
        keyIcon.setTextFill(Color.DARKBLUE); // Textfarbe

        // Titel des Dialogs
        Label titleLabel = new Label("Willkommen bei Chatty!");
        titleLabel.setFont(Font.font("Segoe UI", 20)); // Schriftgröße
        titleLabel.setTextFill(Color.DARKBLUE); // Textfarbe

        // Eingabefeld für den Benutzernamen
        HBox usernameBox = new HBox(10); // Horizontales Layout mit 10px Abstand
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        Label usernameIcon = new Label("👤");
        usernameIcon.setFont(Font.font("Segoe UI Emoji", 24)); // Icon für Benutzername
        TextField usernameField = new TextField(); // Eingabefeld für den Benutzernamen
        usernameField.setPromptText("Benutzername eingeben");
        usernameField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        usernameBox.getChildren().addAll(usernameIcon, usernameField);

        // Eingabefeld für die IP-Adresse
        HBox ipBox = new HBox(10);
        ipBox.setAlignment(Pos.CENTER_LEFT);
        Label ipIcon = new Label("🌐");
        ipIcon.setFont(Font.font("Segoe UI Emoji", 24));
        TextField ipField = new TextField("localhost"); // Standardmäßig "localhost"
        ipField.setPromptText("Server-IP-Adresse eingeben");
        ipField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        ipBox.getChildren().addAll(ipIcon, ipField);

        // Eingabefeld für den Port
        HBox portBox = new HBox(10);
        portBox.setAlignment(Pos.CENTER_LEFT);
        Label portIcon = new Label("🔌");
        portIcon.setFont(Font.font("Segoe UI Emoji", 24));
        TextField portField = new TextField("3141"); // Standard-Port
        portField.setPromptText("Port eingeben");
        portField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #007acc;
            -fx-padding: 8;
            -fx-border-radius: 5;
            -fx-font-family: 'Segoe UI', sans-serif;
            """);
        portBox.getChildren().addAll(portIcon, portField);

        // Hinweistext
        Label hintLabel = new Label("Bitte geben Sie Ihren Benutzernamen, die Server-IP-Adresse und den Port ein.");
        hintLabel.setFont(Font.font("Segoe UI", 14));
        hintLabel.setTextFill(Color.GRAY); // Grauer Hinweistext

        // Zusammenfügen der Dialogelemente
        dialogContent.getChildren().addAll(keyIcon, titleLabel, hintLabel, usernameBox, ipBox, portBox);
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setStyle("-fx-border-radius: 10; -fx-background-radius: 10;");

        // Verarbeiten der Dialogeingaben
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new Pair<>(usernameField.getText(), new Pair<>(ipField.getText(), portField.getText()));
            }
            return null;
        });

        // Dialog anzeigen und Benutzereingaben verarbeiten
        dialog.showAndWait().ifPresent(result -> {
            username = result.getKey(); // Benutzername setzen
            address = result.getValue().getKey(); // IP-Adresse setzen
            try {
                port = Integer.parseInt(result.getValue().getValue()); // Portnummer setzen
            } catch (NumberFormatException e) {
                showError("Ungültiger Port: " + result.getValue().getValue());
                return;
            }
            setupConnection(); // Verbindung mit den eingegebenen Daten herstellen
        });

        // Hauptfenster erstellen
        primaryStage.setTitle("Chatty - Willkommen, " + (username != null ? username : "Gast"));
        primaryStage.setScene(createScene()); // Szene mit der Benutzeroberfläche erstellen
        primaryStage.setMinWidth(600); // Mindestbreite
        primaryStage.setMinHeight(400); // Mindesthöhe
        primaryStage.show(); // Fenster anzeigen
    }
}
