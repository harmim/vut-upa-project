package upa.openjfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upa.db.GeneralDB;
import upa.db.multimedia.DBImage;
import upa.db.spatial.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Vector;

public class SidePanelController {
  @FXML private ImageView image;
  private ConnectingWindowController connection;
  private CanvasController canvas;
  private Node actualNode;
  private Vector<Integer> images_id;
  private HashMap<Node, Integer> objects;

  public static String getTypeOfObjectFromGroup(Group g) {
    Node n = g.getChildren().get(0);
    if (n instanceof Circle) {
      if (((Circle) n).getRadius() == CanvasController.pointRadius)
        return Mode.workingMode.Multipoint.toString();
      else return Mode.workingMode.Collection.toString();
    } else return Mode.workingMode.Polyline.toString();
  }

  public void setCanvasController(CanvasController canvas) {
    this.canvas = canvas;
  }

  public void initialize() {
    images_id = new Vector<>();
    objects = new HashMap<>();
  }

  public void setConnectionController(ConnectingWindowController c) {
    connection = c;
  }

  @FXML
  public void loadInfoFromDb() {
    Image img = new Image("file:../../../pisomka/img.png");
    image.setImage(img);
  }

  @FXML
  public void SaveImageToDb() throws SQLException, GeneralDB.NotFoundException, IOException {
    Stage stage = new Stage();
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Resource File");
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
    File selectedFile = fileChooser.showOpenDialog(stage);
    Image img = new Image(selectedFile.toURI().toString());
    image.setImage(img);
    int id =
        DBImage.save_image_from_file(
            connection.ods.getConnection(), 0, selectedFile.getAbsolutePath());
    objects.put(actualNode, id);
    System.out.println(canvas.getObjectId(actualNode) + " = " + id);
    SpatialObject.update_image_id_of_object(
        connection.ods.getConnection(), canvas.getObjectId(actualNode), id);
  }

  @FXML
  public void DeleteImageFromDb() throws SQLException, GeneralDB.NotFoundException {
    DBImage.delete_image(connection.ods.getConnection(), objects.get(actualNode));
    objects.put(actualNode, 0);
    image.setImage(null);
  }

  @FXML
  public void RotateImage90() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(), objects.get(actualNode), "rotate", 90.0, 0.0, 0.0, 0.0);
    image.setImage(img);
  }

  @FXML
  public void RotateImage180() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(),
            objects.get(actualNode),
            "rotate",
            180.0,
            0.0,
            0.0,
            0.0);
    image.setImage(img);
  }

  @FXML
  public void RotateImage270() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(),
            objects.get(actualNode),
            "rotate",
            270.0,
            0.0,
            0.0,
            0.0);
    image.setImage(img);
  }

  @FXML
  public void MirrorImage() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(), objects.get(actualNode), "mirror", 0.0, 0.0, 0.0, 0.0);
    image.setImage(img);
  }

  @FXML
  public void CutImage() {

    Dialog<String[]> dialog = new Dialog<>();
    dialog.setTitle("Cut Image");
    dialog.setHeaderText("Please specify coordinates, width and height.");
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    TextField x = new TextField();
    TextField y = new TextField();
    TextField width = new TextField();
    TextField height = new TextField();

    x.setPromptText("x");
    y.setPromptText("y");
    width.setPromptText("width");
    height.setPromptText("height");

    dialogPane.setContent(new VBox(8, x, y, width, height));
    //    Platform.runLater(x::requestFocus);
    dialog.setResultConverter(
        (ButtonType button) -> {
          if (button == ButtonType.OK) {
            return new String[] {x.getText(), y.getText(), width.getText(), height.getText()};
          }
          return null;
        });
    Optional<String[]> optionalResult = dialog.showAndWait();
    optionalResult.ifPresent(
        (String[] results) -> {
          int n_x = 0, n_y = 0, n_width = 0, n_height = 0;
          try {
            n_x = Integer.parseInt(results[0]);
            n_y = Integer.parseInt(results[1]);
            n_width = Integer.parseInt(results[2]);
            n_height = Integer.parseInt(results[3]);
          } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid parameters of cut operation");
            alert.setHeaderText("The given parameters must be numbers!");
            alert.setContentText("Try again!");
            alert.showAndWait();
            return;
          }

          if (n_x + n_width >= 0
              && n_x > 0
              && n_y > 0
              && n_x <= image.getImage().getWidth()
              && n_y <= image.getImage().getHeight()
              && n_x + n_width <= image.getImage().getWidth()
              && n_y + n_height >= 0
              && n_y + n_height <= image.getImage().getHeight()) {
            try {
              Image img =
                  DBImage.process_image(
                      connection.ods.getConnection(),
                      objects.get(actualNode),
                      "cut",
                      n_x,
                      n_y,
                      n_width,
                      n_height);
              image.setImage(img);
            } catch (SQLException | IOException | GeneralDB.NotFoundException ignored) {
              Alert alert = new Alert(Alert.AlertType.ERROR);
              alert.setTitle("Invalid parameters of cut operation");
              alert.setHeaderText("The cut operation was not successful!");
              alert.setContentText("Try again!");
              alert.showAndWait();
            }
          } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid parameters of cut operation");
            alert.setHeaderText("Incorrect coordinates, width or height!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }
        });
  }

  @FXML
  private void fixedOrMaxScale(String scale, boolean is_percentual) {
    Dialog<String[]> dialog = new Dialog<>();
    dialog.setTitle(scale + " Image");
    dialog.setHeaderText("Please specify scales.");
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    TextField xScale = new TextField();
    TextField yScale = null;
    xScale.setPromptText(is_percentual ? "percentualScale" : "xScale");
    if (!is_percentual) {
      yScale = new TextField();
      yScale.setPromptText("yScale");
      dialogPane.setContent(new VBox(8, xScale, yScale));
    } else {
      dialogPane.setContent(new VBox(8, xScale));
    }

    //    Platform.runLater(x::requestFocus);
    TextField finalYScale = yScale;
    dialog.setResultConverter(
        (ButtonType button) -> {
          if (button == ButtonType.OK) {
            return new String[] {xScale.getText(), !is_percentual ? finalYScale.getText() : ""};
          }
          return null;
        });
    Optional<String[]> optionalResult = dialog.showAndWait();
    optionalResult.ifPresent(
        (String[] results) -> {
          double n_xScale = 0, n_yScale = 0;
          try {
            if (!is_percentual) {
              n_xScale = Integer.parseInt(results[0]);
              n_yScale = Integer.parseInt(results[1]);
            } else {
              n_xScale = Double.parseDouble(results[0]);
            }
          } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid parameters of " + scale + " operation");
            alert.setHeaderText("The parameters xScale and yScale must be numbers!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }

          if ((n_xScale > 0
                  && n_xScale <= image.getImage().getWidth()
                  && n_yScale > 0
                  && n_yScale <= image.getImage().getHeight()
                  && !is_percentual)
              || (is_percentual && n_xScale > 0)) {
            try {
              Image img =
                  DBImage.process_image(
                      connection.ods.getConnection(),
                      objects.get(actualNode),
                      scale,
                      n_xScale,
                      n_yScale,
                      0.0,
                      0.0);
              image.setImage(img);
            } catch (GeneralDB.NotFoundException | SQLException | IOException e) {
              Alert alert = new Alert(Alert.AlertType.ERROR);
              alert.setTitle("Invalid " + scale + " operation");
              alert.setHeaderText("The " + scale + " operation was not successful!");
              alert.setContentText("Try again!");
              alert.showAndWait();
            }
          } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid parameters of " + scale + " operation");
            alert.setHeaderText("The given scales must be in the range of the image!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }
        });
  }

  @FXML
  public void fixedScale() {
    fixedOrMaxScale("fixedScale", false);
  }

  @FXML
  public void maxScale() {
    fixedOrMaxScale("maxScale", false);
  }

  @FXML
  public void percentualScale() {
    fixedOrMaxScale("scale", true);
  }

  @FXML
  public void grayScale() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(),
            objects.get(actualNode),
            "monochrome",
            0.0,
            0.0,
            0.0,
            0.0);
    image.setImage(img);
  }

  @FXML
  public void findMostSimilar() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.find_most_similar_image(
            connection.ods.getConnection(), objects.get(actualNode), 0.3, 0.3, 0.1, 0.3);
    image.setImage(img);
  }

  public void setActiveNode(Node obj, Integer id) throws SQLException, IOException {
    System.out.println(actualNode);
    actualNode = obj;
    if (objects.containsKey(actualNode)) {
      try {
        image.setImage(DBImage.load_image(connection.ods.getConnection(), objects.get(actualNode)));
      } catch (GeneralDB.NotFoundException e) {
        image.setImage(null);
      }
    } else {
      image.setImage(null);
    }

    //    loadInfoFromDb(obj, );
  }

  public void saveStateToDb(
      HashMap<Node, Integer> newObjects,
      HashMap<Node, Integer> editedObjects,
      HashMap<Node, Integer> deletedObjects,
      String mode)
      throws Exception {
    // save new objects to DB and set their dbIDs
    for (HashMap.Entry<Node, Integer> entry : newObjects.entrySet()) {
      System.out.println("New OBJ " + entry.getKey());
      String typeOfObject = getTypeOfObject(entry.getKey());
      int id;
      System.out.println("--------" + typeOfObject);
      int index = 0;
      double points[];
      switch (typeOfObject) {
        case "Rect":
          Rectangle r = ((Rectangle) (entry.getKey()));
          id =
              upa.db.spatial.Rectangle.insert_new_rectangle(
                  connection.ods.getConnection(),
                  "LodA",
                  "Lod",
                  new double[] {
                    r.getX(),
                    translateYCordForDb(r.getY()),
                    r.getX() + r.getWidth(),
                    translateYCordForDb(r.getY() + r.getHeight())
                  });
          entry.setValue(id);
          break;
        case "Circle":
          Circle c = ((Circle) (entry.getKey()));
          id =
              upa.db.spatial.Circle.insert_new_circle(
                  connection.ods.getConnection(),
                  "PlanetaA",
                  "Planeta",
                  new double[] {
                    c.getCenterX() - c.getRadius(),
                    translateYCordForDb(c.getCenterY() + c.getRadius()),
                    c.getRadius() * 2
                  });
          entry.setValue(id);
          break;
        case "Point":
          Circle p = ((Circle) (entry.getKey()));
          id =
              upa.db.spatial.Point.insert_new_point(
                  connection.ods.getConnection(),
                  "HviezdaA",
                  "Hviezda",
                  new double[] {p.getCenterX(), translateYCordForDb(p.getCenterY())});
          entry.setValue(id);
          break;
        case "Multipoint":
          Group m = ((Group) (entry.getKey()));
          points = new double[m.getChildren().size() * 2];
          index = 0;
          for (Node n : m.getChildren()) {
            points[index++] = ((Circle) n).getCenterX();
            points[index++] = translateYCordForDb(((Circle) n).getCenterY());
          }
          id =
              upa.db.spatial.MultiPoint.insert_new_multipoint(
                  connection.ods.getConnection(), "SuhvezdieA", "Suhvezdie", points);
          entry.setValue(id);
          break;
        case "Polyline":
          Group pl = ((Group) (entry.getKey()));
          points = new double[(pl.getChildren().size() - 1) * 2];
          index = 0;
          for (Node n : pl.getChildren()) {
            if (n instanceof Circle) {
              points[index++] = ((Circle) n).getCenterX();
              points[index++] = translateYCordForDb(((Circle) n).getCenterY());
            }
          }
          id =
              upa.db.spatial.StraightLineString.insert_new_line_string(
                  connection.ods.getConnection(), "SatelityA", "Satelity", points);
          entry.setValue(id);
          break;
        case "Collection":
          Group cc = ((Group) (entry.getKey()));
          Circle firstCircle = (Circle) (cc.getChildren().get(0));
          int n = cc.getChildren().size();
          id =
              upa.db.spatial.CircleCollection.insert_new_collection_to_db(
                  connection.ods.getConnection(),
                  "MeteorityA",
                  "Meteority",
                  new double[] {
                    firstCircle.getCenterX() - firstCircle.getRadius(),
                    translateYCordForDb(firstCircle.getCenterY() + firstCircle.getRadius()),
                    firstCircle.getRadius() * 2
                  },
                  n,
                  true);
          entry.setValue(id);
          break;
      }
    }

    for (HashMap.Entry<Node, Integer> entry : editedObjects.entrySet()) {
      System.out.println(mode);
      System.out.println("Edited OBJ " + entry);
      String typeOfObject = getTypeOfObject(entry.getKey());
      double[] points;
      int index = 0;
      switch (typeOfObject) {
        case "Point":
          // only MOVE
          Circle p = ((Circle) (entry.getKey()));
          Point.update_geometry_of_point(
              connection.ods.getConnection(),
              entry.getValue(),
              new double[] {p.getCenterX(), translateYCordForDb(p.getCenterY())});
          break;
        case "Circle":
          Circle c = ((Circle) (entry.getKey()));
          upa.db.spatial.Circle.update_geometry_of_circle(
              connection.ods.getConnection(),
              entry.getValue(),
              new double[] {
                c.getCenterX() - c.getRadius(),
                translateYCordForDb(c.getCenterY() + c.getRadius()),
                c.getRadius() * 2
              });
          break;
        case "Rect":
          Rectangle r = ((Rectangle) (entry.getKey()));
          upa.db.spatial.Rectangle.update_geometry_of_rectangle(
              connection.ods.getConnection(),
              entry.getValue(),
              new double[] {
                r.getX(),
                translateYCordForDb(r.getY()),
                r.getX() + r.getWidth(),
                translateYCordForDb(r.getY() + r.getHeight())
              });
          break;
        case "Multipoint":
          Group m = ((Group) (entry.getKey()));
          points = new double[m.getChildren().size() * 2];
          index = 0;
          for (Node n : m.getChildren()) {
            points[index++] = ((Circle) n).getCenterX();
            points[index++] = translateYCordForDb(((Circle) n).getCenterY());
          }
          upa.db.spatial.MultiPoint.update_geometry_of_multipoint(
              connection.ods.getConnection(), entry.getValue(), points);
          break;
        case "Polyline":
          Group pl = ((Group) (entry.getKey()));
          points = new double[(pl.getChildren().size() - 1) * 2];
          index = 0;
          for (Node n : pl.getChildren()) {
            if (n instanceof Circle) {
              points[index++] = ((Circle) n).getCenterX();
              points[index++] = translateYCordForDb(((Circle) n).getCenterY());
            }
          }
          upa.db.spatial.StraightLineString.update_geometry_of_line_string(
              connection.ods.getConnection(), entry.getValue(), points);
          break;
        case "Collection":
          Group cc = ((Group) (entry.getKey()));
          Circle firstCircle = (Circle) (cc.getChildren().get(0));
          int[] active_circles = new int[cc.getChildren().size()];
          int j = 0;
          for (int i = 0; i < cc.getChildren().size(); i++) {
            if (((Circle) cc.getChildren().get(i)).isVisible()) active_circles[j++] = i;
          }
          if (mode.equals("Move")) {
            System.out.println("Move Circle Collection");
            CircleCollection.update_coordinates_of_collection(
                connection.ods.getConnection(),
                entry.getValue(),
                firstCircle.getCenterX() - firstCircle.getRadius(),
                translateYCordForDb(firstCircle.getCenterY() + firstCircle.getRadius()),
                active_circles);
          } else {
            System.out.println("Resize Circle Collection");
            CircleCollection.update_diameter_of_circles_in_collection(
                connection.ods.getConnection(), entry.getValue(), firstCircle.getRadius() * 2);
          }
      }
    }

    for (HashMap.Entry<Node, Integer> entry : deletedObjects.entrySet()) {
      System.out.println("Removed OBJ " + entry);
      String typeOfObject = getTypeOfObject(entry.getKey());
      if (typeOfObject.equals(Mode.workingMode.Collection.toString())) {
        CircleCollection.delete_circle_collection(connection.ods.getConnection(), entry.getValue());
      } else SpatialObject.delete_object(connection.ods.getConnection(), entry.getValue());
      if (entry.getKey() == actualNode)
        image.setImage(null); // clear the image from the right panel if actual node is deleted
    }
  }

  // only for changes on existing special objects (Collection, PL, MP) in DB, contextMenu changes
  public void saveObjectToDb(String typeOfObject, int id, String mode, Vector<Double> changes)
      throws Exception {
    System.out.println("DB obtains object " + typeOfObject + "changed in mode " + mode);
    //    System.out.println(typeOfObject);
    //    System.out.println(object);
    switch (typeOfObject) {
      case "Multipoint":
        if (mode.equals(Mode.transactionMode.removePointFromMP.toString())) {
          MultiPoint.delete_points_from_multipoint(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        } else {
          System.out.println("ID: " + id);
          MultiPoint.add_points_to_multipoint(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        }
        break;
      case "Polyline":
        System.out.println(changes);
        if (mode.equals(Mode.transactionMode.removePointFromPL.toString())) {
          StraightLineString.delete_points_from_line_string(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        } else {
          StraightLineString.add_points_to_line_string(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        }
        break;
      case "Collection":
        if (mode.equals(Mode.transactionMode.removeCircleFromCollection.toString())) {
          CircleCollection.delete_object_from_collection(
              connection.ods.getConnection(), id, doubleArrayToIntArray(changes), 6);
        } else {
          CircleCollection.add_circles_to_collection(
              connection.ods.getConnection(), id, doubleArrayToIntArray(changes));
        }
    }
  }

  private String getTypeOfObject(Node n) {
    if (n instanceof Rectangle) return Mode.workingMode.Rect.toString();
    if (n instanceof Circle) {
      System.out.println("IT IS CIRCLE");
      if (((Circle) n).getRadius() == CanvasController.pointRadius)
        return Mode.workingMode.Point.toString();
      else return Mode.workingMode.Circle.toString();
    } else return getTypeOfObjectFromGroup((Group) (n));
  }

  private double translateYCordForDb(double y) {
    return 600.0 - y;
  }

  private int[] doubleArrayToIntArray(Vector arr) {
    int[] target = new int[arr.size()];
    for (int i = 0; i < target.length; i++) {
      target[i] = (int) (arr.get(i));
    }
    return target;
  }

  private double[] pointsDataToArrayWithTranslate(Vector points) {
    double[] target = new double[points.size()];
    for (int i = 0; i < target.length; i++) {
      target[i] =
          (i % 2 == 1) ? translateYCordForDb((double) points.get(i)) : (double) points.get(i);
    }
    return target;
  }
}
