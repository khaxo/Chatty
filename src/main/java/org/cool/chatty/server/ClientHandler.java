package org.cool.chatty.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private ChatServer chatServer; // Referenz zum ChatServer, um Nachrichten zu senden und Teilnehmer zu verwalten
    private Socket connectionToClient; // Socket, um mit dem Client zu kommunizieren

    private String name; // Der Name des Clients (wird nach der Verbindung gesetzt)

    private BufferedReader fromClientReader; // Zum Lesen von Nachrichten vom Client
    private PrintWriter toClientWriter; // Zum Senden von Nachrichten an den Client

    // Konstruktor, der den ChatServer und die Client-Verbindung übergibt
    public ClientHandler(ChatServer chatServer, Socket connectionToClient) {
        this.chatServer = chatServer;
        this.connectionToClient = connectionToClient;

        // Startet den Thread, der die run-Methode ausführt
        new Thread(this).start();
    }

    // Gibt den Namen des Clients zurück
    public String getName() {
        return name;
    }

    // Der Haupt-Thread, der die Kommunikation mit dem Client handhabt
    @Override
    public void run() {
        try {
            // Initialisieren der Eingabe- und Ausgabe-Streams für die Kommunikation mit dem Client
            fromClientReader = new BufferedReader(new InputStreamReader(connectionToClient.getInputStream()));
            toClientWriter = new PrintWriter(new OutputStreamWriter(connectionToClient.getOutputStream()));

            // Einlesen des ersten Nachrichtenpakets vom Client (erster Schritt: Verbindung und Name)
            String initialMessage = fromClientReader.readLine();
            if (initialMessage != null && initialMessage.startsWith("CONNECT:")) {
                // Extrahieren des Namens des Clients aus der Nachricht und Festlegen des Namens
                name = initialMessage.substring(8).trim();
                // Benachrichtige alle anderen Teilnehmer, dass der neue Client dem Chat beigetreten ist
                chatServer.broadcastMessage("SYSTEM: " + name + " hat den Chat betreten.");
                // Aktualisiere die Liste der Teilnehmer im Chat
                chatServer.updateParticipantsList();
            }

            String message;
            // Solange der Client Nachrichten sendet, werden diese verarbeitet
            while ((message = fromClientReader.readLine()) != null) {
                // Nachricht an alle anderen Clients senden
                chatServer.broadcastMessage(message);
            }
        } catch (IOException e) {
            // Fehlerbehandlung, falls während der Kommunikation ein Problem auftritt
            System.err.println("Error handling client " + (name != null ? name : "unknown") + ": " + e.getMessage());
        } finally {
            // Beim Verlassen des Chat-Threads den Client aus der Teilnehmerliste entfernen und Benachrichtigen
            chatServer.removeClient(this);
            chatServer.broadcastMessage("SYSTEM: " + name + " hat den Chat verlassen.");
            // Ressourcen freigeben (Streams und Socket schließen)
            closeResources();
        }
    }

    // Hilfsmethode zum Schließen der Ressourcen (Streams und Socket)
    private void closeResources() {
        try {
            if (fromClientReader != null) {
                fromClientReader.close();
            }
            if (toClientWriter != null) {
                toClientWriter.close();
            }
            if (connectionToClient != null && !connectionToClient.isClosed()) {
                connectionToClient.close();
            }
        } catch (IOException e) {
            // Fehlerbehandlung, wenn beim Schließen der Ressourcen ein Problem auftritt
            System.err.println("Error closing resources for client " + name + ": " + e.getMessage());
        }
    }

    // Methode, um eine Nachricht an den Client zu senden
    public void sendMessage(String message) {
        if (toClientWriter != null) {
            toClientWriter.println(message);
            toClientWriter.flush(); // Sicherstellen, dass die Nachricht sofort gesendet wird
        }
    }
}

