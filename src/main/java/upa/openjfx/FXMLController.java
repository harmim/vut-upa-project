package upa.openjfx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class FXMLController {
  @FXML private BorderPane mainPane;

  @FXML private ConnectingWindowController ConnectController;

  @FXML private CanvasController canvasController;

  @FXML private SidePanelController SideController;

  @FXML private AnchorPane ConnectDialog;

  public void initialize() {
    ConnectController.setParentController(mainPane, ConnectDialog);
    canvasController.setSidePanel(SideController);
    SideController.setConnectionController(ConnectController);
    SideController.setCanvasController(canvasController);
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
    stage.show();
  }
}
