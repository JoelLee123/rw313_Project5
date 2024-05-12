module org.example.p2p {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.p2p to javafx.fxml;
    exports org.example.p2p;
}