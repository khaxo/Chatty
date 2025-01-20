package org.cool.chatty.server;

// Importiert Klassen, die für Netzwerkkommunikation, Eingabe/Ausgabe und Threads benötigt werden
import java.io.IOException; // Wird verwendet, um Eingabe- und Ausgabefehler zu behandeln
import java.net.InetAddress; // Repräsentiert eine IP-Adresse
import java.net.ServerSocket; // Erlaubt das Warten auf und Akzeptieren von Verbindungen von Clients
import java.net.Socket; // Repräsentiert eine einzelne Verbindung zu einem Client
import java.util.List; // Schnittstelle für Listen-Datenstrukturen
import java.util.Scanner; // Zum Einlesen von Benutzereingaben aus der Konsole
import java.util.concurrent.CopyOnWriteArrayList; // Threadsichere Liste, geeignet für parallelen Zugriff

public class ChatServer {
    private ServerSocket serverSocket; // ServerSocket wartet auf Verbindungsanfragen von Clients
    private List<ClientHandler> clients; // Liste, die alle verbundenen Clients speichert

    // Konstruktor, um einen neuen ChatServer zu erstellen
    public ChatServer(String ipAddress, int port) {
        clients = new CopyOnWriteArrayList<>();
        // CopyOnWriteArrayList ist eine thread-sichere Implementierung der Liste.
        // Sie wird verwendet, um gleichzeitige Änderungen durch mehrere Threads zu ermöglichen.

        try {
            // InetAddress repräsentiert eine IP-Adresse
            // Hier wird die vom Benutzer eingegebene IP-Adresse auf Gültigkeit geprüft und als Objekt erstellt
            InetAddress bindAddress = InetAddress.getByName(ipAddress);

            // ServerSocket wird erstellt. Es ist ein Netzwerkobjekt, das Verbindungsanfragen akzeptieren kann.
            // Parameter:
            // - port: Der Port, auf dem der Server lauscht (z. B. 8080)
            // - 50: Maximale Länge der Warteschlange für eingehende Verbindungen
            // - bindAddress: Die IP-Adresse, an die der Server gebunden ist
            serverSocket = new ServerSocket(port, 50, bindAddress);
            System.out.println("Server läuft auf IP-Adresse: " + bindAddress.getHostAddress() + " und Port: " + port);

            // Endlosschleife um kontinuierlich neue Verbindungen zu akzeptieren
            while (true) {
                System.out.println("Waiting for new client...");

                // Wartet, bis ein neuer Client eine Verbindung aufbaut!!!
                Socket connectionToClient = serverSocket.accept();
                // accept() blockiert, bis eine neue Verbindung eingeht, und gibt ein Socket-Objekt zurück,
                // das die Verbindung zu diesem Client repräsentiert.

                // Ein neuer ClientHandler wird erstellt, um die Kommunikation mit dem Client zu verwalten
                ClientHandler client = new ClientHandler(this, connectionToClient);

                // Der neue Client wird zur Liste der verbundenen Clients hinzugefügt
                clients.add(client);
                System.out.println("Accepted new client: " + connectionToClient.getRemoteSocketAddress());
                // getRemoteSocketAddress gibt die Adresse des verbundenen Clients zurück
            }
        } catch (IOException e) {
            // Behandelt Eingabe-/Ausgabefehler, z. B. beim Öffnen oder Schließen von Verbindungen
            e.printStackTrace();
        } finally {
            // Dieser Block wird ausgeführt, bevor das Programm beendet wird, um Ressourcen zu bereinigen
            if (serverSocket != null) {
                try {
                    serverSocket.close(); // Schließt den ServerSocket und stoppt den Server
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Methode zum Senden einer Nachricht an alle verbundenen Clients
    public synchronized void broadcastMessage(String message) {
        // synchronized stellt sicher, dass nur ein Thread diese Methode zur gleichen Zeit ausführen kann,
        // um parallelen Zugriff auf die Liste der Clients zu verhindern.
        System.out.println(message); // Gibt die Nachricht auf der Konsole des Servers aus
        for (ClientHandler client : clients) {
            client.sendMessage(message); // Sendet die Nachricht an jeden Client
        }
    }

    // Aktualisiert die Liste der Teilnehmer und sendet sie an alle Clients
    public synchronized void updateParticipantsList() {
        // StringBuilder wird verwendet, um effizient Strings zu erstellen und zu manipulieren
        StringBuilder participantsMessage = new StringBuilder("PARTICIPANTS: ");
        for (ClientHandler client : clients) {
            participantsMessage.append(client.getName()).append(", ");
            // Fügt die Namen aller verbundenen Clients zur Teilnehmerliste hinzu
        }
        broadcastMessage(participantsMessage.toString());
        // Sendet die aktualisierte Teilnehmerliste an alle Clients
    }

    // Entfernt einen Client aus der Liste und aktualisiert die Teilnehmerliste
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client); // Entfernt den angegebenen Client aus der Liste
        updateParticipantsList(); // Aktualisiert die Liste der Teilnehmer
    }

    // Einstiegspunkt des Programms
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Fragt den Benutzer nach der IP-Adresse, auf der der Server laufen soll
        System.out.println("Geben Sie die IP-Adresse ein, auf der der Server laufen soll (z.B. 0.0.0.0 für alle Interfaces):");
        String ipAddress = scanner.nextLine(); // Liest die IP-Adresse von der Konsole ein

        // Fragt den Benutzer nach dem Port, auf den der Server warten soll
        System.out.println("Legen Sie einen Port fest!:");
        int port = scanner.nextInt(); // Liest den Port von der Konsole ein

        // Erstellt eine neue Instanz des ChatServers mit den angegebenen Parametern
        new ChatServer(ipAddress, port);
    }
}

