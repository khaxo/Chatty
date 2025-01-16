module org.cool.chatty {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.cool.chatty.server to javafx.fxml;
    opens org.cool.chatty.client to javafx.fxml;

    exports org.cool.chatty.client;
    exports org.cool.chatty.server;
}