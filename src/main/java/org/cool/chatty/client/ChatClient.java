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


    private void setupConnection() {
        try {
            // Erstellt eine Verbindung zum Server mit der angegebenen Adresse und dem Port
            connectionToServer = new Socket(address, port);

            // Initialisiert den Reader zum Lesen von Nachrichten vom Server
            fromServerReader = new BufferedReader(new InputStreamReader(connectionToServer.getInputStream()));

            // Initialisiert den Writer zum Senden von Nachrichten an den Server
            toServerWriter = new PrintWriter(new OutputStreamWriter(connectionToServer.getOutputStream()));

            // Sendet eine Begrüßungsnachricht mit dem Benutzernamen an den Server
            toServerWriter.println("CONNECT:" + username);
            toServerWriter.flush();

            // Startet einen neuen Thread, um eingehende Nachrichten zu empfangen
            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            // Zeigt eine Fehlermeldung an, falls die Verbindung fehlschlägt
            showError("Verbindung zum Server fehlgeschlagen: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            String message;
            // Liest Nachrichten kontinuierlich vom Server
            while ((message = fromServerReader.readLine()) != null) {
                String finalMessage = message;
                // Übergibt die Nachricht zur Verarbeitung in die JavaFX-Anwendung
                Platform.runLater(() -> handleMessage(finalMessage));
            }
        } catch (IOException e) {
            // Zeigt eine Fehlermeldung an, falls die Verbindung unterbrochen wird
            showError("Verbindung verloren: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        // Verarbeitet Nachrichten basierend auf ihrem Typ
        if (message.startsWith("CONNECT:")) {
            // Handhabt eine neue Verbindung und fügt den Teilnehmer zur Liste hinzu
            String participant = message.substring(8);
            participants.add(participant);
            displaySystemMessage(participant + " hat den Chat betreten.");
        } else if (message.startsWith("DISCONNECT:")) {
            // Entfernt einen Teilnehmer bei Verbindungsabbruch und zeigt eine Systemnachricht
            String participant = message.substring(11);
            participants.remove(participant);
            displaySystemMessage(participant + " hat den Chat verlassen.");
        } else if (message.startsWith("IMAGE:")) {
            // Verarbeitet empfangene Bildnachrichten
            String content = message.substring(6);
            int firstColon = content.indexOf(":");
            if (firstColon > 0 && firstColon < content.length() - 1) {
                String sender = content.substring(0, firstColon);
                String base64Image = content.substring(firstColon + 1);
                displayImage(sender, base64Image);
            } else {
                // Zeigt eine Fehlermeldung bei ungültigem Bildformat
                System.err.println("Ungültige Bildnachricht: " + message);
                displaySystemMessage("Fehler: Ungültige Bildnachricht empfangen.");
            }
        } else {
            // Handhabt reguläre Textnachrichten
            displayMessage(message);
        }
    }

    private void sendMessage() {
        // Liest den Text aus dem Eingabefeld und sendet ihn, falls er nicht leer ist
        String message = inputTextField.getText().trim();
        if (!message.isEmpty()) {
            toServerWriter.println(username + ": " + message);
            toServerWriter.flush();
            inputTextField.clear(); // Leert das Eingabefeld nach dem Senden
        }
    }

    private void sendImage() {
        // Öffnet einen Dateiauswahldialog, um ein Bild zu senden
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Bild auswählen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bilddateien", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Liest die ausgewählte Bilddatei und sendet sie im Base64-Format
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try (FileInputStream fis = new FileInputStream(selectedFile)) {
                byte[] imageBytes = fis.readAllBytes();
                String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
                toServerWriter.println("IMAGE:" + username + ":" + encodedImage);
                toServerWriter.flush();
            } catch (IOException e) {
                // Zeigt eine Fehlermeldung an, falls das Bild nicht gesendet werden kann
                showError("Fehler beim Senden des Bildes: " + e.getMessage());
            }
        }
    }

    private void displayMessage(String message) {
        // Zeigt eine empfangene Textnachricht in der Benutzeroberfläche an
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(TextAlignment.LEFT);
        messageLabel.setFont(Font.font("Segoe UI", 14));
        messageLabel.setStyle("""
            -fx-background-color: #e0f7fa;
            -fx-text-fill: #000000;
            -fx-padding: 10;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            """);
        messageContainer.getChildren().add(messageLabel);
    }

    private void displayImage(String sender, String base64Image) {
        // Dekodiert das Base64-Bild und zeigt es mit einem Label an
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);

        // Label für den Absender der Bildnachricht
        Label senderLabel = new Label(sender + " hat ein Bild gesendet:");
        senderLabel.setFont(Font.font("Segoe UI", 14));
        senderLabel.setStyle("-fx-text-fill: black;");

        // Ermöglicht das Öffnen des Bildes in einem neuen Fenster bei Klick
        imageView.setOnMouseClicked(event -> openImageInNewWindow(image, sender));

        VBox imageBox = new VBox(5, senderLabel, imageView);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        messageContainer.getChildren().add(imageBox);
    }

    private void openImageInNewWindow(Image image, String sender) {
        // Öffnet ein Bild in einem neuen Fenster
        Stage imageStage = new Stage();
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(500);

        root.getChildren().addAll(imageView);

        Scene scene = new Scene(root, 600, 400);
        imageStage.setTitle("Bild von " + sender);
        imageStage.setScene(scene);
        imageStage.show();
    }

    private void displaySystemMessage(String message) {
        // Zeigt Systemnachrichten in der Benutzeroberfläche an
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(TextAlignment.LEFT);
        messageLabel.setFont(Font.font("Segoe UI", 14));
        messageLabel.setStyle("""
            -fx-background-color: #ffcccb;
            -fx-text-fill: #000000;
            -fx-padding: 10;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            """);
        messageContainer.getChildren().add(messageLabel);
    }

    private void showError(String message) {
        // Zeigt eine Fehlermeldung in einem JavaFX-Dialog an
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}
