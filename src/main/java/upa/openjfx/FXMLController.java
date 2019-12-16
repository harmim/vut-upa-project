package upa.openjfx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class FXMLController {
  @FXML public Label labelUsername;
  @FXML private BorderPane mainPane;

  @FXML private ConnectingWindowController ConnectController;

  @FXML private CanvasController canvasController;

  @FXML private SidePanelController SideController;

  @FXML private AnchorPane ConnectDialog;

  @FXML
  public void initialize() {
    ConnectController.setParentController(mainPane, ConnectDialog);
    canvasController.setSidePanel(SideController);
    canvasController.setConnectionController(ConnectController);
    SideController.setConnectionController(ConnectController);
    SideController.setCanvasController(canvasController);
    ConnectController.setCanvasController(canvasController);
    ConnectController.setFXMLController(this);
  }

  @FXML
  public void LogoutActionClicked() {
    mainPane.setVisible(false);
    ConnectDialog.setVisible(true);
  }

  @FXML
  public void AboutActionClicked() throws IOException {
    Stage stage = new Stage();
    Parent root = FXMLLoader.load(
      FXMLController.class.getResource("About.fxml")
    );
    stage.setScene(new Scene(root));
    stage.setTitle("About");
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setResizable(false);
    stage.show();
  }
}
