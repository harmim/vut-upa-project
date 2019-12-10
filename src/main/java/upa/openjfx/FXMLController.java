package upa.openjfx;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import javax.sql.ConnectionPoolDataSource;
import java.sql.Connection;
import java.sql.SQLException;

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

  //  @FXML
  //  public void saveState() {
  //    ArrayList<HashMap<Object, Integer>> maps = canvasController.getObjects();
  //    HashMap<Object, Integer> newObjects = maps.get(0);
  //    HashMap<Object, Integer> editedObjects = maps.get(1);
  //    HashMap<Object, Integer> deletedObjects = maps.get(2);
  //    SideController.saveStateToDb(newObjects, editedObjects, deletedObjects);
  //  }
}
