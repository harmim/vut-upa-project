package upa.openjfx;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import oracle.jdbc.OracleResultSet;
import oracle.spatial.geometry.JGeometry;
import upa.db.GeneralDB;
import upa.db.InitDB;
import upa.db.spatial.SpatialObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.*;


public class CanvasController {
  public static final int pointRadius = 4;
  private static final int prefSize = 20;
  private static final int groupCount = 4;
  private static final Color COLOR_SPACESHIP = Color.LIGHTGRAY;
  private static final Color COLOR_PLANET = Color.SKYBLUE;
  private static final Color COLOR_STAR = Color.YELLOW;
  private static final Color COLOR_CONSTELLATION = Color.DARKGOLDENROD;
  private static final Color COLOR_METEORIT = Color.INDIANRED;
  private static final Color COLOR_SATELLITE = Color.SEAGREEN;
  private static final Color COLOR_SATELLITE_EDGE = Color.WHITESMOKE;
  private double xCord, yCord;
  private String mode = Mode.workingMode.None.toString();
  private String transactionMode = Mode.transactionMode.None.toString();
  private Vector<Double> polylinePoints;
  private Vector<Double> multipointPoints;
  private Group oldPolyline;
  private Group oldMultipoint;
  private Node selectedObject;
  private Group currentlyEditedSpecialSpatialObject;
  private HashMap<Node, Integer> objects; // ObjectID, dbID
  private HashMap<Node, Integer> newObjects; // ObjectID, dbID
  private HashMap<Node, Integer> editedObjects; // ObjectID, dbID
  private HashMap<Node, Integer> deletedObjects; // ObjectID, dbID
  private Vector<Double> currentlyEditedDataPLData;
  private Vector<Double> currentlyEditedDataMPData;
  private Vector<Integer> currentlyEditedDataCollectionData;

  private SidePanelController sidePanel;
  @FXML private AnchorPane Canvas;
  @FXML private ToggleGroup ToggleGroup1;

  private ConnectingWindowController connection;

  public void setConnectionController(ConnectingWindowController c) {
    connection = c;
  }

  private static double[] pointsDataToArray(Vector<Double> points) {
    double[] target = new double[points.size()];
    for (int i = 0; i < target.length; i++) {
      target[i] = points.get(i);
    }
    return target;
  }

  private void flush_temporal_data() {
    if (!mode.equals(Mode.workingMode.Polyline.toString())
        && !mode.equals(Mode.workingMode.addPointToPL.toString())) {
      polylinePoints = new Vector<>();
      oldPolyline = null;
    }
    if (!mode.equals(Mode.workingMode.Multipoint.toString())
        && !mode.equals(Mode.workingMode.addPointToMP.toString())) {
      multipointPoints = new Vector<>();
      oldMultipoint = null;
    }
  }

  void setSidePanel(SidePanelController controller) {
    sidePanel = controller;
  }

  @FXML
  public void initialize() {
    polylinePoints = new Vector<>();
    multipointPoints = new Vector<>();
    objects = new HashMap<>();
    newObjects = new HashMap<>();
    editedObjects = new HashMap<>();
    deletedObjects = new HashMap<>();
    currentlyEditedDataMPData = new Vector<>();
    currentlyEditedDataPLData = new Vector<>();
    currentlyEditedDataCollectionData = new Vector<>();
    selectedObject = null;
    currentlyEditedSpecialSpatialObject = null;
    oldPolyline = null;
    oldMultipoint = null;
    Canvas.getChildren().clear();
  }

  private boolean validOperation(double minX, double minY, double maxX, double maxY) {
    return minY > 0 && maxY < Canvas.getHeight() && minX > 0 && maxX < Canvas.getWidth();
  }

  private void selectObject(Node object) {
    if (selectedObject != null) selectedObject.setEffect(null);

    if (mode.equals(Mode.workingMode.None.toString())) {
      DropShadow ds = new DropShadow(16, Color.RED);
      object.setEffect(ds);
      selectedObject = object;
    }

    sidePanel.refreshAction();
  }

  public int getObjectId(Node object) {
    return objects.get(object);
  }

  public Node getObjectById(int id) {
    for (HashMap.Entry<Node, Integer> entry : objects.entrySet()) {
      if (entry.getValue() == id) return entry.getKey();
    }
    return null;
  }

  @FXML
  public void CanvasClicked(MouseEvent event) {
    if (transactionMode.equals(Mode.transactionMode.removePointFromMP.toString())
        || transactionMode.equals(Mode.transactionMode.removePointFromPL.toString())) return;
    System.out.println("mode: " + mode);
    Integer DB_DEFAULT_ID = -1;
    if (event.getButton() != MouseButton.PRIMARY) return;
    switch (mode) {
      case "Rect":
        flush_temporal_data();
        if (validOperation(
            event.getX() - (double) (prefSize / 2),
            event.getY() - (double) (prefSize / 2),
            event.getX() + (double) (prefSize / 2),
            event.getY() + (double) (prefSize / 2))) {
          Rectangle r = createRect(event.getX(), event.getY());
          Canvas.getChildren().add(r);
          newObjects.put(r, DB_DEFAULT_ID);
        }
        break;
      case "Circle":
        flush_temporal_data();
        if (validOperation(
            event.getX() - (double) (prefSize),
            event.getY() - (double) (prefSize),
            event.getX() + (double) (prefSize),
            event.getY() + (double) (prefSize))) {
          Circle c = createCircle(event.getX(), event.getY(), false);
          Canvas.getChildren().add(c);
          newObjects.put(c, DB_DEFAULT_ID);
        }
        break;
      case "Point":
        flush_temporal_data();
        if (validOperation(
            event.getX() - (double) (pointRadius),
            event.getY() - (double) (pointRadius),
            event.getX() + (double) (pointRadius),
            event.getY() + (double) (pointRadius))) {
          Circle c = createCircle(event.getX(), event.getY(), true);
          Canvas.getChildren().add(c);
          newObjects.put(c, DB_DEFAULT_ID);
        }
        break;
      case "Collection":
        flush_temporal_data();
        if (validOperation(
            event.getX() - (double) (prefSize),
            event.getY() - (double) (prefSize),
            event.getX() + (double) (prefSize * 3 * (groupCount - 1)) + prefSize,
            event.getY() + (double) (prefSize))) {
          Group g = createCollection(event.getX(), event.getY(), null);
          Canvas.getChildren().add(g);
          newObjects.put(g, DB_DEFAULT_ID);
        }
        break;
      case "addPointToMP":
        currentlyEditedDataMPData.add(event.getX());
        currentlyEditedDataMPData.add(event.getY());
        Circle c = createNewMCircle(event.getX(), event.getY(), oldMultipoint);
        oldMultipoint.getChildren().add(c);
        break;
      case "Multipoint":
        flush_temporal_data();
        if (oldMultipoint != null) {
          Canvas.getChildren().remove(oldMultipoint);
          newObjects.remove(oldMultipoint);
        }
        if (validOperation(
            event.getX() - (double) (pointRadius),
            event.getY() - (double) (pointRadius),
            event.getX() + (double) (pointRadius),
            event.getY() + (double) (pointRadius))) {
          multipointPoints.add(event.getX());
          multipointPoints.add(event.getY());
          Group g = createMultipoint();
          Canvas.getChildren().add(g);
          if (!mode.equals(Mode.transactionMode.addPointToMP.toString()))
            newObjects.put(g, DB_DEFAULT_ID);
        }
        break;

      case "addPointToPL":
        currentlyEditedDataPLData.add(event.getX());
        currentlyEditedDataPLData.add(event.getY());
        ((Polyline) oldPolyline.getChildren().get(0)).getPoints().add(event.getX());
        ((Polyline) oldPolyline.getChildren().get(0)).getPoints().add(event.getY());
        Circle m = createNewCircle(event.getX(), event.getY(), oldPolyline);
        oldPolyline.getChildren().add(m);
        break;
      case "Polyline":
        flush_temporal_data();
        if (oldPolyline != null) {
          Canvas.getChildren().remove(oldPolyline);
          newObjects.remove(oldPolyline);
        }
        if (validOperation(
            event.getX() - (double) (pointRadius),
            event.getY() - (double) (pointRadius),
            event.getX() + (double) (pointRadius),
            event.getY() + (double) (pointRadius))) {
          polylinePoints.add(event.getX());
          polylinePoints.add(event.getY());
          Group g = createPolyline();

          Canvas.getChildren().add(g);
          if (!mode.equals(Mode.transactionMode.addPointToPL.toString()))
            newObjects.put(g, DB_DEFAULT_ID);
        }
        break;

      default:
        flush_temporal_data();
        break;
    }
  }

  private Rectangle createRect(double sceneX, double sceneY) {
    double prefSizeD = prefSize;
    return createRect(sceneX - (prefSizeD / 2), sceneY - (prefSizeD / 2), prefSizeD);
  }

  private Rectangle createRect(double sceneX, double sceneY, double width) {
    Rectangle rect = new Rectangle(sceneX, sceneY, width, width);
    rect.setFill(COLOR_SPACESHIP);
    rect.setStroke(Color.BLACK);
    rect.setCursor(Cursor.HAND);
    rect.setOnMousePressed(
        (t) -> {
          // REMOVING RECTANGLE
          if (mode.equals(Mode.workingMode.Delete.toString())) {
            Canvas.getChildren().remove(rect);
            // check whether object is in DB
            if (objects.containsKey(rect)) deletedObjects.put(rect, objects.get(rect));
            // object was newly created, not in DB
            else newObjects.remove(rect);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Rectangle c = (Rectangle) (t.getSource());
          c.toFront();
          selectObject(c);
          if (mode.equals(Mode.workingMode.None.toString())) {
            try {
              sidePanel.setActiveNode(c);
            } catch (SQLException | IOException | GeneralDB.NotFoundException e) {
              e.printStackTrace();
            }
          }
        });
    rect.setOnMouseDragged(
        (t) -> {
          if (mode.equals(Mode.workingMode.Move.toString())) {
            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Rectangle c = (Rectangle) (t.getSource());

            if (validOperation(
                c.getX() + offsetX,
                c.getY() + offsetY,
                c.getX() + offsetX + c.getWidth(),
                c.getY() + offsetY + c.getHeight())) {
              // object is already in DB
              if (objects.containsKey(rect)) editedObjects.put(rect, objects.get(rect));
              c.setX(c.getX() + offsetX);
              c.setY(c.getY() + offsetY);

              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          } else if (mode.equals(Mode.workingMode.Resize.toString())) {
            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Rectangle c = (Rectangle) (t.getSource());
            if (validOperation(
                c.getX(),
                c.getY(),
                c.getX() + offsetX + c.getWidth(),
                c.getY() + offsetY + c.getHeight())) {
              if (objects.containsKey(rect)) editedObjects.put(rect, objects.get(rect));

              c.setWidth(c.getWidth() + offsetX);
              c.setHeight(c.getHeight() + offsetY);

              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });
    return rect;
  }

  private Circle createCircle(double sceneX, double sceneY, boolean isPoint) {
    return createCircle(sceneX, sceneY, isPoint, isPoint ? pointRadius : prefSize);
  }

  private Circle createCircle(double sceneX, double sceneY, boolean isPoint, double radius) {
    Circle circle = new Circle(sceneX, sceneY, radius, isPoint ? COLOR_STAR : COLOR_PLANET);
    circle.setStroke(Color.BLACK);
    circle.setCursor(Cursor.HAND);

    circle.setOnMousePressed(
        (t) -> {
          if (mode.equals(Mode.workingMode.Delete.toString())) {
            Canvas.getChildren().remove(circle);
            // check whether object is in DB
            if (objects.containsKey(circle)) deletedObjects.put(circle, objects.get(circle));
            // object was newly created, not in DB
            else newObjects.remove(circle);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Circle c = (Circle) (t.getSource());
          c.toFront();
          selectObject(c);
          if (mode.equals(Mode.workingMode.None.toString())) {
            try {
              sidePanel.setActiveNode(c);
            } catch (SQLException | IOException | GeneralDB.NotFoundException e) {
              e.printStackTrace();
            }
          }
        });
    circle.setOnMouseDragged(
        (t) -> {
          if (mode.equals(Mode.workingMode.Move.toString())) {

            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Circle c = (Circle) (t.getSource());
            if (validOperation(
                c.getCenterX() + offsetX - c.getRadius(),
                c.getCenterY() + offsetY - c.getRadius(),
                c.getCenterX() + offsetX + c.getRadius(),
                c.getCenterY() + offsetY + c.getRadius())) {
              // check whether object is in DB
              if (objects.containsKey(circle)) editedObjects.put(circle, objects.get(circle));

              c.setCenterX(c.getCenterX() + offsetX);
              c.setCenterY(c.getCenterY() + offsetY);

              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          } else if (mode.equals(Mode.workingMode.Resize.toString())) {
            if (isPoint) return;
            double offset = t.getSceneX() - xCord;

            Circle c = (Circle) (t.getSource());
            if (validOperation(
                c.getCenterX() - offset - c.getRadius(),
                c.getCenterY() - offset - c.getRadius(),
                c.getCenterX() + offset + c.getRadius(),
                c.getCenterY() + offset + c.getRadius())) {
              // check whether object is in DB
              if (objects.containsKey(circle)) editedObjects.put(circle, objects.get(circle));

              c.setRadius(c.getRadius() + offset);
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });
    return circle;
  }

  private Group createCollection(Double sceneX, Double sceneY, Circle[] circles) {
    Group group = new Group();
    double prefSizeD = prefSize;
    double radius = prefSizeD / 2;
    double stride = prefSizeD / 2 * 3;

    for (int i = 0; i < groupCount; i++) {
      Circle circle;
      if (circles != null) {
        if (circles.length > i) {
          circle = circles[i];
        } else {
          break;
        }
      } else {
        circle = new Circle(sceneX + stride * i, sceneY, radius);
      }
      circle.setFill(COLOR_METEORIT);
      circle.setStroke(Color.BLACK);
      circle.setCursor(Cursor.HAND);

      final ContextMenu contextMenu = new ContextMenu();
      MenuItem delete = new MenuItem("Delete circle");
      contextMenu.getItems().add(delete);
      delete.setOnAction(
          event -> {
            boolean empty = true;
            int index = group.getChildren().indexOf(circle);
            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));
            circle.setVisible(false);

            for (Node n : group.getChildren()) {
              if (n.isVisible()) {
                empty = false;
                try {
                  checkAndSetTransactionMode(
                      Mode.transactionMode.removeCircleFromCollection.toString(), group);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              } else {
                if (group.getChildren().indexOf(n) < group.getChildren().indexOf(circle)) {
                  index--;
                }
              }
            }
            if (!empty) {
              currentlyEditedDataCollectionData.add(index);
              return;
            }
            //            this.transactionMode = Mode.transactionMode.None.toString();
            //            currentlyEditedSpecialSpatialObject = null;

            // last circle has been removed, so the whole group will be removed too
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
          });

      circle.setOnContextMenuRequested(
          event -> {
            int index = 0;
            Vector<Integer> invisible = new Vector<>();
            for (Node n : group.getChildren()) {
              if (!n.isVisible()) invisible.add(index);
              index++;
            }
            if (invisible.size() > 0) {
              Menu add;
              if (contextMenu.getItems().size() == 1) {
                // create new ADD menu with items
                add = new Menu("Add circle");
                contextMenu.getItems().add(add);

              } else {
                // add Menu exists, just update the indices
                add = (Menu) contextMenu.getItems().get(1);
                add.getItems().clear();
              }
              for (Integer invisibleCircleIndex : invisible) {
                MenuItem item = new MenuItem("On index " + invisibleCircleIndex.toString());
                item.setOnAction(
                    t -> {
                      if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));
                      group.getChildren().get(invisibleCircleIndex).setVisible(true);
                      try {
                        checkAndSetTransactionMode(
                            Mode.transactionMode.addCircleToCollection.toString(), group);
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                      currentlyEditedDataCollectionData.add(invisibleCircleIndex);
                    });
                add.getItems().add(item);
              }
            } else {
              if (contextMenu.getItems().size() > 1) contextMenu.getItems().remove(1);
            }
            if (mode.equals(Mode.workingMode.Collection.toString()))
              contextMenu.show(circle, event.getScreenX(), event.getScreenY());
          });
      group.getChildren().add(circle);
    }
    group.setOnMousePressed(
        (t) -> {
          if (mode.equals(Mode.workingMode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          if (mode.equals(Mode.workingMode.None.toString())) {
            try {
              sidePanel.setActiveNode(c);
            } catch (SQLException | IOException | GeneralDB.NotFoundException e) {
              e.printStackTrace();
            }
          }
        });
    group.setOnMouseDragged(
        (t) -> {
          if (mode.equals(Mode.workingMode.Move.toString())) {

            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Group c = (Group) (t.getSource());
            Circle firstChild = (Circle) (c.getChildren().get(0));
            Circle lastChild = (Circle) (c.getChildren().get(c.getChildren().size() - 1));
            if (validOperation(
                firstChild.getCenterX() + offsetX - firstChild.getRadius(),
                firstChild.getCenterY() + offsetY - firstChild.getRadius(),
                lastChild.getCenterX() + offsetX + lastChild.getRadius(),
                lastChild.getCenterY() + offsetY + lastChild.getRadius())) {

              if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

              for (Node node : c.getChildren()) {
                ((Circle) node).setCenterX(((Circle) node).getCenterX() + offsetX);
                ((Circle) node).setCenterY(((Circle) node).getCenterY() + offsetY);
              }
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          } else if (mode.equals(Mode.workingMode.Resize.toString())) {

            double offset = t.getSceneX() - xCord;

            Group c = (Group) (t.getSource());
            Circle firstChild = (Circle) (c.getChildren().get(0));
            Circle lastChild = (Circle) (c.getChildren().get(c.getChildren().size() - 1));
            if (validOperation(
                firstChild.getCenterX() - offset - firstChild.getRadius(),
                firstChild.getCenterY() - offset - firstChild.getRadius(),
                lastChild.getCenterX() + offset + lastChild.getRadius(),
                lastChild.getCenterY() + offset + lastChild.getRadius())) {
              if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));
              for (Node node : c.getChildren()) {
                ((Circle) node).setRadius(((Circle) node).getRadius() + offset / 2);
              }
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });
    return group;
  }

  private Group createPolyline() {
    Group group = new Group();
    Polyline p = new Polyline(pointsDataToArray(polylinePoints));
    p.setCursor(Cursor.HAND);
    p.setStrokeWidth(2.0);
    p.setStroke(COLOR_SATELLITE_EDGE);
    group.getChildren().add(p);

    double[] arr = pointsDataToArray(polylinePoints);
    for (int i = 0; i < arr.length; i += 2) {
      Circle c = createNewCircle(arr[i], arr[i + 1], group);
      group.getChildren().add(c);
    }

    group.setOnMousePressed(
        (t) -> {
          if (mode.equals(Mode.workingMode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          if (mode.equals(Mode.workingMode.None.toString())) {
            try {
              sidePanel.setActiveNode(c);
            } catch (SQLException | IOException | GeneralDB.NotFoundException e) {
              e.printStackTrace();
            }
          }
        });
    group.setOnMouseDragged(
        (t) -> {
          if (mode.equals(Mode.workingMode.Move.toString())) {

            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Group c = (Group) (t.getSource());
            if (validOperation(
                c.getLayoutBounds().getMinX() + offsetX,
                c.getLayoutBounds().getMinY() + offsetY,
                c.getLayoutBounds().getMaxX() + offsetX,
                c.getLayoutBounds().getMaxY() + offsetY)) {
              if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

              for (Node n : c.getChildren()) {
                if (n instanceof Polyline) {
                  //                  ((Polyline) n).setLayoutX(((Polyline) n).getLayoutX() +
                  // offsetX);
                  //                  ((Polyline) n).setLayoutY(((Polyline) n).getLayoutY() +
                  // offsetY);
                  for (int i = 0; i < ((Polyline) n).getPoints().size(); i += 2) {
                    ((Polyline) n).getPoints().set(i, ((Polyline) n).getPoints().get(i) + offsetX);
                    ((Polyline) n)
                        .getPoints()
                        .set(i + 1, ((Polyline) n).getPoints().get(i + 1) + offsetY);
                  }
                } else {
                  ((Circle) n).setCenterX(((Circle) n).getCenterX() + offsetX);
                  ((Circle) n).setCenterY(((Circle) n).getCenterY() + offsetY);
                }
              }
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });

    oldPolyline = group;

    return group;
  }

  private Circle createNewCircle(double x, double y, Group group) {
    Circle c = new Circle(x, y, pointRadius, COLOR_SATELLITE);
    c.setStroke(Color.BLACK);
    c.setCursor(Cursor.HAND);

    final ContextMenu contextMenu = new ContextMenu();
    MenuItem delete = new MenuItem("Delete point");
    MenuItem add = new MenuItem("Add point");
    contextMenu.getItems().addAll(delete, add);
    delete.setOnAction(
        event -> {
          flush_temporal_data();
          // if there is only one polyline with 2 points, remove whole group
          if (group.getChildren().size() < 4) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
            //              this.transactionMode = Mode.transactionMode.None.toString();
            //              currentlyEditedSpecialSpatialObject = null;
            return;
          }
          //            if (objects.containsKey(group)) editedObjects.put(group,
          // objects.get(group));
          try {
            checkAndSetTransactionMode(Mode.transactionMode.removePointFromPL.toString(), group);
          } catch (Exception e) {
            e.printStackTrace();
          }
          currentlyEditedDataPLData.add(c.getCenterX());
          currentlyEditedDataPLData.add(c.getCenterY());
          int index = group.getChildren().indexOf(c) - 1;

          ((Polyline) (group.getChildren().get(0)))
              .getPoints()
              .remove(index * 2); // get(index * 2);
          ((Polyline) (group.getChildren().get(0)))
              .getPoints()
              .remove(index * 2); // get(index * 2);
          //            this.polylinePoints.remove(index * 2);
          //            this.polylinePoints.remove(index * 2);

          group.getChildren().remove(c);
        });

    add.setOnAction(
        event -> {
          try {
            checkAndSetTransactionMode(Mode.transactionMode.addPointToPL.toString(), group);
          } catch (Exception e) {
            e.printStackTrace();
          }

          Polyline p1 = null;
          for (Node n : group.getChildren()) {
            if (n instanceof Polyline) p1 = (Polyline) (n);
            break;
          }
          oldPolyline = group;
          mode = Mode.workingMode.addPointToPL.toString();
          polylinePoints = new Vector<>();
          polylinePoints.addAll(Objects.requireNonNull(p1).getPoints());
        });

    c.setOnContextMenuRequested(
        event -> {
          if (mode.equals(Mode.workingMode.Polyline.toString())
              || mode.equals(Mode.workingMode.addPointToPL.toString()))
            contextMenu.show(c, event.getScreenX(), event.getScreenY());
        });
    return c;
  }

  private Group createMultipoint() {
    Group group = new Group();
    double[] arr = pointsDataToArray(multipointPoints);
    for (int i = 0; i < arr.length; i += 2) {
      Circle c = createNewMCircle(arr[i], arr[i + 1], group);
      group.getChildren().add(c);
    }

    group.setOnMousePressed(
        (t) -> {
          if (mode.equals(Mode.workingMode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          if (mode.equals(Mode.workingMode.None.toString())) {
            try {
              sidePanel.setActiveNode(c);
            } catch (SQLException | IOException | GeneralDB.NotFoundException e) {
              e.printStackTrace();
            }
          }
        });
    group.setOnMouseDragged(
        (t) -> {
          if (mode.equals(Mode.workingMode.Move.toString())) {

            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Group c = (Group) (t.getSource());
            if (validOperation(
                c.getLayoutBounds().getMinX() + offsetX,
                c.getLayoutBounds().getMinY() + offsetY,
                c.getLayoutBounds().getMaxX() + offsetX,
                c.getLayoutBounds().getMaxY() + offsetY)) {
              if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

              for (Node n : c.getChildren()) {
                ((Circle) n).setCenterX(((Circle) n).getCenterX() + offsetX);
                ((Circle) n).setCenterY(((Circle) n).getCenterY() + offsetY);
              }
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });

    oldMultipoint = group;

    return group;
  }

  private Circle createNewMCircle(double x, double y, Group group) {
    Circle c = new Circle(x, y, pointRadius, COLOR_CONSTELLATION);
    c.setCursor(Cursor.HAND);
    c.setStroke(Color.BLACK);

    final ContextMenu contextMenu = new ContextMenu();
    MenuItem delete = new MenuItem("Delete point");
    MenuItem add = new MenuItem("Add point");
    //      MenuItem paste = new MenuItem("Paste");
    contextMenu.getItems().addAll(delete, add);
    delete.setOnAction(
        event -> {
          flush_temporal_data();
          // if there is only one polyline with 2 points, remove whole group
          if (group.getChildren().size() == 1) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            else newObjects.remove(group);
            return;
          }
          // DB is actualized immediately, so this is not necessary
          //            if (objects.containsKey(group)) editedObjects.put(group,
          // objects.get(group));

          try {
            checkAndSetTransactionMode(Mode.transactionMode.removePointFromMP.toString(), group);
          } catch (Exception e) {
            e.printStackTrace();
          }
          currentlyEditedDataMPData.add(c.getCenterX());
          currentlyEditedDataMPData.add(c.getCenterY());

          group.getChildren().remove(c);
          multipointPoints = new Vector<>();
          for (Node n : group.getChildren()) {
            if (n instanceof Circle) {
              multipointPoints.add(((Circle) n).getCenterX());
              multipointPoints.add(((Circle) n).getCenterY());
            }
          }
        });

    add.setOnAction(
        event -> {
          //            if (objects.containsKey(group)) editedObjects.put(group,
          // objects.get(group));
          //          if (!objects.containsKey(group)) // aktualne vytvoreny objekt
          //          return;
          try {
            checkAndSetTransactionMode(Mode.transactionMode.addPointToMP.toString(), group);
          } catch (Exception e) {
            e.printStackTrace();
          }
          oldMultipoint = group;
          mode = Mode.workingMode.addPointToMP.toString();
          multipointPoints = new Vector<>();
          for (Node n : group.getChildren()) {
            if (n instanceof Circle) {
              multipointPoints.add(((Circle) n).getCenterX());
              multipointPoints.add(((Circle) n).getCenterY());
            }
          }
        });

    c.setOnContextMenuRequested(
        event -> {
          if (mode.equals(Mode.workingMode.Multipoint.toString())
              || mode.equals(Mode.workingMode.addPointToMP.toString()))
            contextMenu.show(c, event.getScreenX(), event.getScreenY());
        });

    return c;
  }

  private void checkAndSetTransactionMode(String newMode, Group object) throws Exception {
    if (transactionMode.equals(newMode) && object == currentlyEditedSpecialSpatialObject) return;
    saveState();
    if (currentlyEditedSpecialSpatialObject != null) {

      // saving global changes to the DB
      saveChange(); // saving local changes to the DB
    }
    transactionMode = newMode;
    currentlyEditedSpecialSpatialObject = object;
  }

  private void saveChange() throws Exception {
    String objectType;
    Vector changes;

    if (currentlyEditedSpecialSpatialObject == null) {
      return;
    } else if (SidePanelController.getTypeOfObjectFromGroup(currentlyEditedSpecialSpatialObject)
        .equals(Mode.workingMode.Collection.toString())) {
      objectType = Mode.workingMode.Collection.toString();
      changes = currentlyEditedDataCollectionData;
    } else if (SidePanelController.getTypeOfObjectFromGroup(currentlyEditedSpecialSpatialObject)
        .equals(Mode.workingMode.Polyline.toString())) {
      objectType = Mode.workingMode.Polyline.toString();
      changes = currentlyEditedDataPLData;
    } else {
      objectType = Mode.workingMode.Multipoint.toString();
      changes = currentlyEditedDataMPData;
    }
    sidePanel.saveObjectToDb(
        objectType, objects.get(currentlyEditedSpecialSpatialObject), transactionMode, changes);
    currentlyEditedDataCollectionData = new Vector<>();
    currentlyEditedDataPLData = new Vector<>();
    currentlyEditedDataMPData = new Vector<>();
  }

  private void saveState() throws Exception {
    if (newObjects.size() > 0 || editedObjects.size() > 0 || deletedObjects.size() > 0) {
      // deleted objects wont be edited / added into the DB
      for (HashMap.Entry<Node, Integer> entry : deletedObjects.entrySet()) {
        newObjects.remove(entry.getKey());
        editedObjects.remove(entry.getKey());
        // remove also from local map of saved DB objects
        objects.remove(entry.getKey());
      }

      // Saving changes to DB
      sidePanel.saveStateToDb(newObjects, editedObjects, deletedObjects, mode);
      // adding new objects to local map of saved DB objects
      for (HashMap.Entry<Node, Integer> entry : newObjects.entrySet()) {
        objects.put(entry.getKey(), entry.getValue());
      }
      // create new instances of maps
      newObjects = new HashMap<>();
      editedObjects = new HashMap<>();
      deletedObjects = new HashMap<>();
    }
  }

  @FXML
  public void changeMode() throws Exception {
    saveChange();
    saveState();
    currentlyEditedSpecialSpatialObject = null;
    sidePanel.setActiveNode(null);

    ToggleButton selected = (ToggleButton) ToggleGroup1.getSelectedToggle();
    flush_temporal_data();

    if (selected == null) {
      mode = Mode.workingMode.None.toString();
    } else{
      mode = selected.getId();
      selectObject(null);
    }

    transactionMode = Mode.transactionMode.None.toString();

    sidePanel.refreshAction();
  }

  public void fillCanvasFromDb() {
    try (Connection conn = connection.ods.getConnection()) {
      try (PreparedStatement ps = conn.prepareStatement(
          "SELECT O_ID, O_TYPE, IMAGE_ID, GEOMETRY FROM VILLAGE"
      )) {
        try (OracleResultSet rs = (OracleResultSet) ps.executeQuery()) {
          while (rs.next()) {
            int id = rs.getInt("O_ID");
            JGeometry geometry = JGeometry.loadJS((Struct) rs.getObject("GEOMETRY"));
            double[] ords;
            double x, y, p1y, p2x, p3y, r2, r, x2;
            Node node = null;

            switch (rs.getString("O_TYPE")) {
              case "spaceships":
                ords = geometry.getOrdinatesArray();
                x = ords[0];
                y = SpatialObject.translateYCordForDb(ords[1]);
                x2 = ords[2];
                double width = Math.abs(x2 - x);

                Rectangle spaceship = createRect(x, y, width);
                node = spaceship;
                Canvas.getChildren().add(spaceship);
                objects.put(spaceship, id);
                editedObjects.put(spaceship, id);
                break;

              case "planets":
                ords = geometry.getOrdinatesArray();
                p1y = ords[1];
                p2x = ords[2];
                p3y = ords[5];
                r2 = Math.abs(p3y - p1y);
                r = r2 / 2;
                x = Math.abs(p2x - r2) + r;
                y = Math.abs(SpatialObject.translateYCordForDb(p1y) - r);

                Circle planet = createCircle(x, y, false, r);
                node = planet;
                Canvas.getChildren().add(planet);
                objects.put(planet, id);
                editedObjects.put(planet, id);
                break;

              case "stars":
                double[] point = geometry.getPoint();
                x = point[0];
                y = SpatialObject.translateYCordForDb(point[1]);

                Circle star = createCircle(x, y, true);
                node = star;
                Canvas.getChildren().add(star);
                objects.put(star, id);
                editedObjects.put(star, id);
                break;

              case "meteorites":
                ords = geometry.getOrdinatesArray();
                Circle[] circles = new Circle[ords.length / 6];
                for (int i = 0; i < ords.length; i += 6) {
                  p1y = ords[i + 1];
                  p2x = ords[i + 2];
                  p3y = ords[i + 5];
                  r2 = Math.abs(p3y - p1y);
                  r = r2 / 2;
                  x = Math.abs(p2x - r2) + r;
                  y = Math.abs(SpatialObject.translateYCordForDb(p1y) - r);

                  circles[i / 6] = new Circle(x, y, r);
                }

                Group meteorites = createCollection(null, null, circles);
                node = meteorites;
                Canvas.getChildren().add(meteorites);
                objects.put(meteorites, id);
                editedObjects.put(meteorites, id);
                break;

              case "constellations":
                multipointPoints = new Vector<>();
                oldMultipoint = null;

                ords = geometry.getOrdinatesArray();
                for (int i = 0; i < ords.length; i += 2) {
                  multipointPoints.add(ords[i]);
                  multipointPoints.add(SpatialObject.translateYCordForDb(ords[i + 1]));
                }

                Group constellation = createMultipoint();
                node = constellation;
                Canvas.getChildren().add(constellation);
                objects.put(constellation, id);
                editedObjects.put(constellation, id);
                break;

              case "satellites":
                polylinePoints = new Vector<>();
                oldPolyline = null;

                ords = geometry.getOrdinatesArray();
                for (int i = 0; i < ords.length; i += 2) {
                  polylinePoints.add(ords[i]);
                  polylinePoints.add(SpatialObject.translateYCordForDb(ords[i + 1]));
                }

                Group satellite = createPolyline();
                node = satellite;
                Canvas.getChildren().add(satellite);
                objects.put(satellite, id);
                editedObjects.put(satellite, id);
                break;
            }

            if (node != null) {
              int imageId = rs.getInt("IMAGE_ID");
              if (imageId != 0) {
                sidePanel.addObject(node, imageId);
              }
            }
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      flush_temporal_data();
    }
  }

  @FXML
  public void refresh() {
    Toggle toggle = ToggleGroup1.getSelectedToggle();
    if (toggle != null) {
      toggle.setSelected(false);
    }

    try {
      changeMode();
    } catch (Exception e) {
      e.printStackTrace();
    }

    initialize();
    InitDB.initSchema(connection.ods);

    fillCanvasFromDb();
  }
}
