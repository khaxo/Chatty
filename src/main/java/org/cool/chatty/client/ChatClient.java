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
    private Scene createScene() {
        // Erstellt ein BorderPane als Hauptlayout-Container
        BorderPane root = new BorderPane();

        // Setzt das Hintergrundbild des BorderPane mit CSS
        root.setStyle("""
        -fx-background-image: url('https://cdn.dribbble.com/users/230290/screenshots/2804358/chatty2.jpg');
        -fx-background-size: cover;
        -fx-background-position: center;
        """);

        // Header-Bereich oben im Layout
        VBox header = new VBox();
        header.setPadding(new Insets(10)); // Fügt einen Innenabstand von 10 Pixeln hinzu
        header.setStyle("""
        -fx-background-color: rgba(75, 75, 75, 1); // Dunkler Hintergrund für den Header
        """);

        // Label im Header, zeigt den Benutzernamen an
        Label headerLabel = new Label("Chatty - Verbunden als: " + username);
        headerLabel.setTextFill(Color.WHITE); // Setzt die Textfarbe auf Weiß
        headerLabel.setFont(Font.font("Segoe UI", 20)); // Definiert die Schriftart und -größe
        headerLabel.setAlignment(Pos.CENTER); // Zentriert den Text

        // Header-Elemente zum VBox-Container hinzufügen
        header.getChildren().addAll(headerLabel);

        // Nachrichtenbereich (Container für Chat-Nachrichten)
        messageContainer = new VBox(10); // Vertikaler Abstand von 10 Pixeln zwischen den Nachrichten
        messageContainer.setPadding(new Insets(10)); // Innenabstand von 10 Pixeln
        messageContainer.setPrefWidth(600); // Fixe Breite von 600 Pixeln
        messageContainer.setStyle("""
        -fx-background-color: rgba(255, 255, 255, 0.8); // Leicht transparenter weißer Hintergrund
        -fx-border-radius: 10;
        -fx-background-radius: 10;
        """);

        // ScrollPane, um den Nachrichtenbereich scrollbar zu machen
        ScrollPane scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true); // ScrollPane passt sich der Breite an
        scrollPane.setStyle("""
        -fx-background: transparent; // Transparentes ScrollPane
        -fx-background-color: transparent;
        -fx-border-color: transparent;
        """);
        // Automatisches Scrollen zum Ende, wenn neue Nachrichten hinzukommen
        scrollPane.vvalueProperty().bind(messageContainer.heightProperty());

        // Eingabefeld für die Texteingabe
        inputTextField = new TextField();
        inputTextField.setPromptText("Nachricht schreiben..."); // Platzhaltertext
        inputTextField.setPrefWidth(580); // Breite leicht schmaler als der Nachrichtenbereich
        inputTextField.setStyle("""
        -fx-background-color: rgba(255, 255, 255, 0.9); // Weißer Hintergrund
        -fx-border-color: rgba(0, 122, 204, 0.8); // Blaue Umrandung
        -fx-border-radius: 5;
        -fx-padding: 8;
        -fx-font-family: 'Segoe UI', sans-serif; // Schriftart
        -fx-font-size: 14px;
        """);

        // Fügt die Funktionalität hinzu, Nachrichten mit Enter zu senden
        inputTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        // Senden-Button für Nachrichten
        Button sendButton = new Button("Senden");
        sendButton.setStyle("""
        -fx-background-color: #007acc; // Blaues Design
        -fx-text-fill: white; // Weißer Text
        -fx-font-size: 14px;
        -fx-font-family: 'Segoe UI', sans-serif;
        -fx-padding: 10 20 10 20; // Innenabstand
        -fx-border-radius: 5;
        -fx-cursor: hand; // Cursor zeigt Hand-Symbol
        """);
        sendButton.setOnAction(event -> sendMessage()); // Nachricht senden bei Klick

        // Button zum Senden von Bildern
        Button imageButton = new Button("📷"); // Icon-Button
        imageButton.setStyle("""
        -fx-background-color: #007acc; // Blaues Design
        -fx-text-fill: white; // Weißer Text
        -fx-font-size: 14px;
        -fx-font-family: 'Segoe UI', sans-serif;
        -fx-padding: 10 20 10 20; // Innenabstand
        -fx-border-radius: 5;
        -fx-cursor: hand; // Cursor zeigt Hand-Symbol
        """);
        imageButton.setOnAction(event -> sendImage()); // Bild senden bei Klick

        // Container für Eingabefeld und Buttons (unten im Layout)
        HBox inputContainer = new HBox(10, inputTextField, sendButton, imageButton); // Abstand von 10 Pixeln
        inputContainer.setPadding(new Insets(10)); // Innenabstand von 10 Pixeln
        inputContainer.setStyle("""
        -fx-background-color: rgba(255, 255, 255, 0.9); // Weißer Hintergrund
        -fx-border-radius: 10;
        """);
        inputContainer.setAlignment(Pos.CENTER); // Zentriert die Elemente

        // Layout-Komponenten in das BorderPane setzen
        root.setTop(header); // Header oben
        root.setCenter(scrollPane); // Nachrichtenbereich in der Mitte
        root.setBottom(inputContainer); // Eingabebereich unten

        // Rückgabe der fertigen Szene mit definierten Abmessungen
        return new Scene(root, 800, 600);
    }

}
