module org.cool.chatty {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.cool.chatty to javafx.fxml;
    exports org.cool.chatty;
}