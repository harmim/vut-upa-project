package upa.openjfx;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import oracle.jdbc.pool.OracleDataSource;
import upa.db.InitDB;

public class ConnectingWindowController {
  private BorderPane parent1;
  private AnchorPane parent2;

  @FXML private TextField username;

  @FXML private TextField password;

  private CanvasController canvasController;

  private FXMLController fxmlController;

  protected OracleDataSource ods = null;

  public void setCanvasController(CanvasController c) {
    canvasController = c;
  }

  public void setFXMLController(FXMLController c) {
    fxmlController = c;
  }

  @FXML
  void ConnectToDB() {
    ods = InitDB.start(this.username.getText(), this.password.getText());
    if (ods != null) {
      if (!InitDB.schemaExists(ods)) {
        InitDB.initSchema(ods);
      }
      canvasController.fillCanvasFromDb();

      fxmlController.labelUsername.setText(InitDB.getCurrentUser(ods));

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
