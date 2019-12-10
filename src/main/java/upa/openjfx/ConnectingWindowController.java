package upa.openjfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.ImageCursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import oracle.jdbc.pool.OracleDataSource;
import upa.db.InitDB;

import javax.swing.text.html.ImageView;
import java.sql.SQLException;

public class ConnectingWindowController {

  private BorderPane parent1;
  private AnchorPane parent2;

  @FXML private TextField username;

  @FXML private TextField password;

  @FXML private AnchorPane ConnectDialog;

  @FXML private Button ConnectButton;

  protected OracleDataSource ods = null;

  @FXML
  void ConnectToDB() {
    System.out.println("asdashdklajsdkj");
    ods = InitDB.start(this.username.getText(), this.password.getText());
    if (ods != null) {
      this.parent1.setVisible(true);
      this.parent2.setVisible(false);
    } else {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Database connection error");
      alert.setHeaderText("Incorrect login or password!");
      alert.setContentText("Try again!");
      alert.showAndWait();
    }
  }

  public void setParentController(BorderPane parent1, AnchorPane parent2) {
    this.parent1 = parent1;
    this.parent2 = parent2;
  }
}
