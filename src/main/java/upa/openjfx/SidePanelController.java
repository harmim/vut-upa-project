package upa.openjfx;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;

public class SidePanelController {
  @FXML private ImageView image;
  private Node actualNode;

  @FXML
  public void ShowImage() {
    Image img = new Image("file:../../../pisomka/img.png");
    image.setImage(img);
  }

  public void setActiveNode(Object obj, Integer id) {
    //
    //    int dbID = id.intValue();
    //    System.out.println(dbID);
    //    // load from DB

  }

  public void saveStateToDb(
      HashMap<Object, Integer> newObjects,
      HashMap<Object, Integer> editedObjects,
      HashMap<Object, Integer> deletedObjects) {

    // save new objects to DB and set their dbIDs
    for (HashMap.Entry<Object, Integer> entry : newObjects.entrySet()) {
      System.out.println("New OBJ " + entry);
    }

    for (HashMap.Entry<Object, Integer> entry : editedObjects.entrySet()) {
      System.out.println("Edited OBJ " + entry);
    }

    for (HashMap.Entry<Object, Integer> entry : deletedObjects.entrySet()) {
      System.out.println("Removed OBJ " + entry);
    }
  }
}
