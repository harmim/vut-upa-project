package upa.openjfx;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.commons.lang3.ArrayUtils;
import upa.db.GeneralDB;
import upa.db.multimedia.DBImage;
import upa.db.queries.Mask;
import upa.db.queries.SpatialOperators;
import upa.db.spatial.*;
import upa.db.spatial.Point;

import javax.print.DocFlavor;
import java.awt.image.AreaAveragingScaleFilter;
import java.io.File;
import java.io.IOException;

import java.sql.SQLException;

import java.util.*;
import java.util.stream.Collectors;

public class SidePanelController {

  private ConnectingWindowController connection;
  private CanvasController canvas;
  private Node actualNode;
  private Vector<Integer> images_id;
  private HashMap<Node, Integer> objects;
  private ArrayList<Integer> objects_with_effect;
  private String[] node_types =
      new String[] {"planets", "stars", "spaceships", "constellations", "meteorites", "satellites"};

  @FXML private ImageView image;
  @FXML private Label nameOfObject;
  @FXML private Label typeOfObject;
  @FXML private Label areaOfObject;
  @FXML private Label diameterOfObject;
  @FXML private Label lengthOfObject;
  @FXML private MenuButton spatialMenu;
  @FXML private MenuButton multimediaMenu;

  public static String getTypeOfObjectFromGroup(Group g) {
    Node n = g.getChildren().get(0);
    if (n instanceof Circle) {
      if (((Circle) n).getRadius() == CanvasController.pointRadius)
        return Mode.workingMode.Multipoint.toString();
      else return Mode.workingMode.Collection.toString();
    } else return Mode.workingMode.Polyline.toString();
  }

  public OracleDataSource getOds() {
    return connection.ods;
  }

  public void setCanvasController(CanvasController canvas) {
    this.canvas = canvas;
  }

  public void initialize() {
    images_id = new Vector<>();
    objects = new HashMap<>();
    objects_with_effect = new ArrayList<>();
  }

  public void setConnectionController(ConnectingWindowController c) {
    connection = c;
  }

  @FXML
  public void refreshAction() {
    for (int id : objects_with_effect) {
      Node node = canvas.getObjectById(id);
      if (node != null) {
        node.setEffect(null);
      }
    }
    objects_with_effect.clear();
  }

  @FXML
  public void checkCurrentNode() {
    if (actualNode == null) {
      for (MenuItem item : multimediaMenu.getItems()) {
        item.setDisable(true);
      }
      for (MenuItem item : spatialMenu.getItems()) {
        item.setDisable(true);
      }
    } else {
      for (MenuItem item : multimediaMenu.getItems()) {
        item.setDisable(false);
      }
      for (MenuItem item : spatialMenu.getItems()) {
        item.setDisable(false);
      }
    }
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
    if (selectedFile != null) {
      Image img = new Image(selectedFile.toURI().toString());
      image.setImage(img);
      int id =
          DBImage.save_image_from_file(
              connection.ods.getConnection(), 0, selectedFile.getAbsolutePath());
      objects.put(actualNode, id);
      SpatialObject.update_image_id_of_object(
          connection.ods.getConnection(), canvas.getObjectId(actualNode), id, true);
    }
  }

  @FXML
  public void DeleteImageFromDb() throws SQLException, GeneralDB.NotFoundException {
    DBImage.delete_image(connection.ods.getConnection(), objects.getOrDefault(actualNode, 0), true);
    objects.put(actualNode, 0);
    image.setImage(null);
  }

  @FXML
  public void RotateImage90() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(),
            objects.getOrDefault(actualNode, 0),
            "rotate",
            90.0,
            0.0,
            0.0,
            0.0);
    image.setImage(img);
  }

  @FXML
  public void RotateImage180() throws SQLException, GeneralDB.NotFoundException, IOException {
    Image img =
        DBImage.process_image(
            connection.ods.getConnection(),
            objects.getOrDefault(actualNode, 0),
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
            objects.getOrDefault(actualNode, 0),
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
            connection.ods.getConnection(),
            objects.getOrDefault(actualNode, 0),
            "mirror",
            0.0,
            0.0,
            0.0,
            0.0);
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

          if (image.getImage() != null
              && n_x + n_width >= 0
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
                      objects.getOrDefault(actualNode, 0),
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

          if (image.getImage() != null
              && ((n_xScale > 0
                      && n_xScale <= image.getImage().getWidth()
                      && n_yScale > 0
                      && n_yScale <= image.getImage().getHeight()
                      && !is_percentual)
                  || (is_percentual && n_xScale > 0))) {
            try {
              Image img =
                  DBImage.process_image(
                      connection.ods.getConnection(),
                      objects.getOrDefault(actualNode, 0),
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
            objects.getOrDefault(actualNode, 0),
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
            connection.ods.getConnection(),
            objects.getOrDefault(actualNode, 0),
            0.3,
            0.3,
            0.1,
            0.3);
    image.setImage(img);
  }

  private String[] get_types_of_selected_nodes(
      int start_idx, int end_idx, int[] results, String[] node_types) {
    String[] selected_node_types = new String[node_types.length];
    int idx = 0;
    int node_idx = 0;
    for (int i = start_idx; i < end_idx; i++, node_idx++) {
      if (results[i] == 1) selected_node_types[idx++] = node_types[node_idx];
    }
    for (int i = idx; i < node_types.length; i++) {
      selected_node_types = ArrayUtils.removeElement(selected_node_types, null);
    }
    return selected_node_types;
  }

  private void findNearestNodes(String mode) {
    Dialog<int[]> dialog = new Dialog<>();
    dialog.setTitle("Find N nearest nodes");
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    TextField n = new TextField();
    TextField d = new TextField();
    CheckBox p = new CheckBox("planets");
    CheckBox s = new CheckBox("stars");
    CheckBox ss = new CheckBox("spaceships");
    CheckBox c = new CheckBox("constellations");
    CheckBox m = new CheckBox("meteorites");
    CheckBox t = new CheckBox("satellites");
    CheckBox p_o = new CheckBox("planets");
    CheckBox s_o = new CheckBox("stars");
    CheckBox ss_o = new CheckBox("spaceships");
    CheckBox c_o = new CheckBox("constellations");
    CheckBox m_o = new CheckBox("meteorites");
    CheckBox t_o = new CheckBox("satellites");
    if (mode.equals("by_id")) {
      dialog.setHeaderText("Please specify N, maximal distance and node types to find.");
      dialogPane.setContent(new VBox(8, n, d, new HBox(4, p, s, ss, c, m, t)));
    } else {
      dialog.setHeaderText(
          "Please specify set of nodes types to which will be finding the N nearest nodes of given types in distance d.");
      dialogPane.setContent(
          new VBox(
              8, new HBox(4, p_o, s_o, ss_o, c_o, m_o, t_o), n, d, new HBox(4, p, s, ss, c, m, t)));
    }
    n.setPromptText("n");
    d.setPromptText("d");

    dialog.setResultConverter(
        (ButtonType button) -> {
          if (button == ButtonType.OK) {
            refreshAction();
            try {
              int[] results = {
                Integer.parseInt(n.getText()),
                Integer.parseInt(d.getText()),
                p.isSelected() ? 1 : 0,
                s.isSelected() ? 1 : 0,
                ss.isSelected() ? 1 : 0,
                c.isSelected() ? 1 : 0,
                m.isSelected() ? 1 : 0,
                t.isSelected() ? 1 : 0,
              };
              if (mode.equals("by_id")) {
                return results;
              } else {
                results =
                    ArrayUtils.addAll(
                        results,
                        p_o.isSelected() ? 1 : 0,
                        s_o.isSelected() ? 1 : 0,
                        ss_o.isSelected() ? 1 : 0,
                        c_o.isSelected() ? 1 : 0,
                        m_o.isSelected() ? 1 : 0,
                        t_o.isSelected() ? 1 : 0);
                return results;
              }
            } catch (NumberFormatException e) {
              Alert alert = new Alert(Alert.AlertType.ERROR);
              alert.setTitle("Invalid parameters of operation");
              alert.setHeaderText("The parameters count (n) and distance (d) must be numbers!");
              alert.setContentText("Try again!");
              alert.showAndWait();
            }
          }
          return null;
        });
    Optional<int[]> optionalResult = dialog.showAndWait();
    optionalResult.ifPresent(
        (int[] results) -> {
          String[] selected_node_types;
          selected_node_types =
              get_types_of_selected_nodes(2, 2 + node_types.length, results, node_types);
          String[] selected_object_types = new String[0];
          if (mode.equals("by_types")) {
            selected_object_types =
                get_types_of_selected_nodes(
                    2 + node_types.length, 2 + 2 * node_types.length, results, node_types);
          }
          try {
            if (mode.equals("by_id")) {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_nearest_neighbours_of_object_by_id(
                                  connection.ods.getConnection(),
                                  canvas.getObjectId(actualNode),
                                  results[0],
                                  results[1],
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            } else {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_nearest_neighbours_of_object_by_type(
                                  connection.ods.getConnection(),
                                  selected_object_types,
                                  results[0],
                                  results[1],
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            }
          } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation was not successful.");
            alert.setHeaderText("The finding of N nearest nodes was not successful!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }
          for (int id : objects_with_effect) {
            Node node = canvas.getObjectById(id);
            if (node != null) {
              InnerShadow innerShadow = new InnerShadow();

              // Setting the offset values of the inner shadow
              innerShadow.setOffsetX(800);
              innerShadow.setOffsetY(600);

              // Setting the color of the inner shadow
              innerShadow.setColor(Color.LIGHTBLUE);

              // Applying inner shadow effect to the circle
              node.setEffect(innerShadow);
              node.toFront();
            }
          }
        });
  }

  @FXML
  public void nearestOfCurrentNode() {
    findNearestNodes("by_id");
  }

  @FXML
  public void nearestOfSet() {
    findNearestNodes("by_types");
  }

  private void getrelationOfCurrentNode(String mode) {
    Dialog<int[]> dialog = new Dialog<>();
    dialog.setTitle("Find relation between nodes");
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    CheckBox p = new CheckBox("planets");
    CheckBox s = new CheckBox("stars");
    CheckBox ss = new CheckBox("spaceships");
    CheckBox c = new CheckBox("constellations");
    CheckBox m = new CheckBox("meteorites");
    CheckBox t = new CheckBox("satellites");
    CheckBox p_o = new CheckBox("planets");
    CheckBox s_o = new CheckBox("stars");
    CheckBox ss_o = new CheckBox("spaceships");
    CheckBox c_o = new CheckBox("constellations");
    CheckBox m_o = new CheckBox("meteorites");
    CheckBox t_o = new CheckBox("satellites");
    CheckBox touch = new CheckBox("touch");
    CheckBox equal = new CheckBox("equal to");
    CheckBox inside = new CheckBox("are inside");
    CheckBox coverdby = new CheckBox("are covered by");
    CheckBox contains = new CheckBox("contain");
    CheckBox covers = new CheckBox("cover");
    CheckBox anyinteract = new CheckBox("interact");
    CheckBox on = new CheckBox("are on");
    CheckBox disjoint = new CheckBox("disjoint");
    CheckBox intersect = new CheckBox("intersect");
    Label subtitle_1 = new Label("I want to get all the:");
    subtitle_1.setStyle("-fx-font-weight: bold");
    Label subtitle_2 = new Label("that:");
    subtitle_2.setStyle("-fx-font-weight: bold");
    Label subtitle_3 = new Label("the current node.");
    subtitle_3.setStyle("-fx-font-weight: bold");
    dialog.setHeaderText("Please specify properties to find relations between nodes.");
    if (mode.equals("of_node")) {
      dialogPane.setContent(
          new VBox(
              8,
              subtitle_1,
              new HBox(4, p, s, ss, c, m, t),
              subtitle_2,
              new HBox(
                  4,
                  contains,
                  touch,
                  equal,
                  inside,
                  coverdby,
                  covers,
                  anyinteract,
                  on,
                  disjoint,
                  intersect),
              subtitle_3));
    } else {
      subtitle_3.setText("to all:");
      dialogPane.setContent(
          new VBox(
              8,
              subtitle_1,
              new HBox(4, p, s, ss, c, m, t),
              subtitle_2,
              new HBox(
                  4,
                  contains,
                  touch,
                  equal,
                  inside,
                  coverdby,
                  covers,
                  anyinteract,
                  on,
                  disjoint,
                  intersect),
              subtitle_3,
              new HBox(4, p_o, s_o, ss_o, c_o, m_o, t_o)));
    }

    dialog.setResultConverter(
        (ButtonType button) -> {
          if (button == ButtonType.OK) {
            refreshAction();
            int[] results =
                new int[] {
                  p.isSelected() ? 1 : 0,
                  s.isSelected() ? 1 : 0,
                  ss.isSelected() ? 1 : 0,
                  c.isSelected() ? 1 : 0,
                  m.isSelected() ? 1 : 0,
                  t.isSelected() ? 1 : 0,
                  touch.isSelected() ? 1 : 0,
                  disjoint.isSelected() ? 1 : 0,
                  intersect.isSelected() ? 1 : 0,
                  equal.isSelected() ? 1 : 0,
                  inside.isSelected() ? 1 : 0,
                  coverdby.isSelected() ? 1 : 0,
                  contains.isSelected() ? 1 : 0,
                  covers.isSelected() ? 1 : 0,
                  anyinteract.isSelected() ? 1 : 0,
                  on.isSelected() ? 1 : 0
                };
            if (mode.equals("of_node")) {
              return results;
            } else {
              return ArrayUtils.addAll(
                  results,
                  p_o.isSelected() ? 1 : 0,
                  s_o.isSelected() ? 1 : 0,
                  ss_o.isSelected() ? 1 : 0,
                  c_o.isSelected() ? 1 : 0,
                  m_o.isSelected() ? 1 : 0,
                  t_o.isSelected() ? 1 : 0);
            }
          }
          return null;
        });
    Optional<int[]> optionalResult = dialog.showAndWait();
    optionalResult.ifPresent(
        (int[] results) -> {
          String[] selected_node_types;
          selected_node_types =
              get_types_of_selected_nodes(0, node_types.length, results, node_types);

          ArrayList<Mask> new_masks = new ArrayList<>();
          int idx = 0;
          for (int i = node_types.length; i < node_types.length + Mask.values().length; i++) {
            if (results[i] == 1) new_masks.add(Mask.values()[idx]);
            idx++;
          }

          Mask[] selected_mask = new Mask[new_masks.size()];
          for (int i = 0; i < new_masks.size(); i++) {
            selected_mask[i] = new_masks.get(i);
          }

          String[] selected_object_types = new String[0];
          if (mode.equals("of_type")) {
            selected_object_types =
                get_types_of_selected_nodes(
                    node_types.length + Mask.values().length, results.length, results, node_types);
          }

          try {
            if (mode.equals("of_node")) {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_related_objects_of_object_by_id(
                                  connection.ods.getConnection(),
                                  canvas.getObjectId(actualNode),
                                  selected_mask,
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            } else {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_related_objects_of_object_by_type(
                                  connection.ods.getConnection(),
                                  selected_object_types,
                                  selected_mask,
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            }
          } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation was not successful.");
            alert.setHeaderText("The finding relation between nodes was not successful!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }
          for (int id : objects_with_effect) {
            Node node = canvas.getObjectById(id);
            if (node != null) {
              InnerShadow innerShadow = new InnerShadow();

              // Setting the offset values of the inner shadow
              innerShadow.setOffsetX(800);
              innerShadow.setOffsetY(600);

              // Setting the color of the inner shadow
              innerShadow.setColor(Color.LIGHTBLUE);

              // Applying inner shadow effect to the circle
              node.setEffect(innerShadow);
              node.toFront();
            }
          }
        });
  }

  @FXML
  public void relationOfCurrentNode() {
    getrelationOfCurrentNode("of_node");
  }

  @FXML
  public void relationOfSet() {
    getrelationOfCurrentNode("of_type");
  }

  private void findInteractObject(boolean of_node) {
    Dialog<int[]> dialog = new Dialog<>();
    dialog.setTitle("Find related nodes");
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    CheckBox p = new CheckBox("planets");
    CheckBox s = new CheckBox("stars");
    CheckBox ss = new CheckBox("spaceships");
    CheckBox c = new CheckBox("constellations");
    CheckBox m = new CheckBox("meteorites");
    CheckBox t = new CheckBox("satellites");
    CheckBox p_o = new CheckBox("planets");
    CheckBox s_o = new CheckBox("stars");
    CheckBox ss_o = new CheckBox("spaceships");
    CheckBox c_o = new CheckBox("constellations");
    CheckBox m_o = new CheckBox("meteorites");
    CheckBox t_o = new CheckBox("satellites");
    Label subtitle_1 = new Label("I want to get all the:");
    subtitle_1.setStyle("-fx-font-weight: bold");
    Label subtitle_2 = new Label("that have some interaction with:");
    subtitle_2.setStyle("-fx-font-weight: bold");
    dialog.setHeaderText("Please specify properties to test interaction between nodes.");
    if (of_node) {
      subtitle_1.setText("I want to get all the:");
      subtitle_2.setText("that have some interaction with current node.");
      dialogPane.setContent(new VBox(8, subtitle_1, new HBox(4, p, s, ss, c, m, t), subtitle_2));
    } else {
      dialog.setHeaderText("Please specify set of nodes types to test interaction.");
      dialogPane.setContent(
          new VBox(
              8,
              subtitle_1,
              new HBox(4, p, s, ss, c, m, t),
              subtitle_2,
              new HBox(4, p_o, s_o, ss_o, c_o, m_o, t_o)));
    }

    dialog.setResultConverter(
        (ButtonType button) -> {
          if (button == ButtonType.OK) {
            refreshAction();
            int[] results =
                new int[] {
                  p.isSelected() ? 1 : 0,
                  s.isSelected() ? 1 : 0,
                  ss.isSelected() ? 1 : 0,
                  c.isSelected() ? 1 : 0,
                  m.isSelected() ? 1 : 0,
                  t.isSelected() ? 1 : 0,
                };
            if (of_node) {
              return results;
            } else {
              return ArrayUtils.addAll(
                  results,
                  p_o.isSelected() ? 1 : 0,
                  s_o.isSelected() ? 1 : 0,
                  ss_o.isSelected() ? 1 : 0,
                  c_o.isSelected() ? 1 : 0,
                  m_o.isSelected() ? 1 : 0,
                  t_o.isSelected() ? 1 : 0);
            }
          }
          return null;
        });

    Optional<int[]> optionalResult = dialog.showAndWait();
    optionalResult.ifPresent(
        (int[] results) -> {
          String[] selected_node_types;
          selected_node_types =
              get_types_of_selected_nodes(0, node_types.length, results, node_types);

          String[] selected_object_types = new String[0];
          if (!of_node) {
            selected_object_types =
                get_types_of_selected_nodes(
                    node_types.length, 2 * node_types.length, results, node_types);
          }
          try {
            if (of_node) {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_interacted_objects_with_object_by_id(
                                  connection.ods.getConnection(),
                                  canvas.getObjectId(actualNode),
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            } else {
              objects_with_effect =
                  (ArrayList<Integer>)
                      Arrays.stream(
                              SpatialOperators.get_interacted_objects_with_object_by_type(
                                  connection.ods.getConnection(),
                                  selected_object_types,
                                  selected_node_types))
                          .boxed()
                          .collect(Collectors.toList());
            }
          } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation was not successful.");
            alert.setHeaderText("The finding of interacted objects was not successful!");
            alert.setContentText("Try again!");
            alert.showAndWait();
          }
          for (int id : objects_with_effect) {
            Node node = canvas.getObjectById(id);
            if (node != null) {
              InnerShadow innerShadow = new InnerShadow();

              // Setting the offset values of the inner shadow
              innerShadow.setOffsetX(800);
              innerShadow.setOffsetY(600);

              // Setting the color of the inner shadow
              innerShadow.setColor(Color.LIGHTBLUE);

              // Applying inner shadow effect to the circle
              node.setEffect(innerShadow);
              node.toFront();
            }
          }
        });
  }

  @FXML
  public void findInteractObjectOfTypes() {
    findInteractObject(false);
  }

  @FXML
  public void findInteractObjectOfNode() {
    findInteractObject(true);
  }

  public void setActiveNode(Node obj, Integer id)
      throws SQLException, IOException, GeneralDB.NotFoundException {
    actualNode = obj;
    if (objects.containsKey(actualNode)) {
      try {
        image.setImage(
            DBImage.load_image(
                connection.ods.getConnection(), objects.getOrDefault(actualNode, 0)));
      } catch (GeneralDB.NotFoundException e) {
        image.setImage(null);
      }
    } else {
      image.setImage(null);
    }
    String[] name_and_type =
        SpatialObject.select_name_and_type_of_object(
            connection.ods.getConnection(), canvas.getObjectId(actualNode));
    double area =
        SpatialOperators.get_area_of_object_by_id(
            connection.ods.getConnection(), canvas.getObjectId(actualNode));
    double diameter =
        SpatialOperators.get_diameter_of_object_by_id(
            connection.ods.getConnection(), canvas.getObjectId(actualNode));
    double length =
        SpatialOperators.get_length_of_object_by_id(
            connection.ods.getConnection(), canvas.getObjectId(actualNode));
    nameOfObject.setText(name_and_type[0]);
    typeOfObject.setText(name_and_type[1]);
    areaOfObject.setText(Double.toString(area));
    diameterOfObject.setText(Double.toString(diameter));
    lengthOfObject.setText(Double.toString(length));
    //    loadInfoFr
    //    omDb(obj, );
  }

  public void saveStateToDb(
      HashMap<Node, Integer> newObjects,
      HashMap<Node, Integer> editedObjects,
      HashMap<Node, Integer> deletedObjects,
      String mode)
      throws Exception {
    // save new objects to DB and set their dbIDs
    for (HashMap.Entry<Node, Integer> entry : newObjects.entrySet()) {
      String typeOfObject = getTypeOfObject(entry.getKey());
      int id;
      int index = 0;
      double points[];
      switch (typeOfObject) {
        case "Rect":
          Rectangle r = ((Rectangle) (entry.getKey()));
          id =
              upa.db.spatial.Rectangle.insert_new_rectangle(
                  connection.ods.getConnection(),
                  "LodA",
                  "spaceships",
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
                  "planets",
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
                  "stars",
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
                  connection.ods.getConnection(), "SuhvezdieA", "constellations", points);
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
                  connection.ods.getConnection(), "SatelityA", "satellites", points);
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
                  "meteorites",
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
            CircleCollection.update_coordinates_of_collection(
                connection.ods.getConnection(),
                entry.getValue(),
                firstCircle.getCenterX() - firstCircle.getRadius(),
                translateYCordForDb(firstCircle.getCenterY() + firstCircle.getRadius()),
                active_circles);
          } else {
            CircleCollection.update_diameter_of_circles_in_collection(
                connection.ods.getConnection(), entry.getValue(), firstCircle.getRadius() * 2);
          }
      }
    }

    for (HashMap.Entry<Node, Integer> entry : deletedObjects.entrySet()) {
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
    switch (typeOfObject) {
      case "Multipoint":
        if (mode.equals(Mode.transactionMode.removePointFromMP.toString())) {
          MultiPoint.delete_points_from_multipoint(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        } else {
          MultiPoint.add_points_to_multipoint(
              connection.ods.getConnection(), id, pointsDataToArrayWithTranslate(changes));
        }
        break;
      case "Polyline":
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
