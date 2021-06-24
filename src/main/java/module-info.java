module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.logging.log4j;

    opens chat to javafx.fxml;
    opens network_chat to javafx.fxml;
    exports chat;
    exports network_chat;
}