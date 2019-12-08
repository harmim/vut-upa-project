package upa.openjfx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

public class CanvasController {
  private static int prefSize = 20;
  private static int pointRadius = 4;
  private static int groupCount = 4;
  private static Color prefColor = Color.GOLD;
  private double xCord, yCord;
  private String mode = Mode.Move.toString();
  private Vector<Double> polylinePoints;
  private Vector<Double> multipointPoints;
  private Group oldPolyline;
  private Group oldMultipoint;
  private Node selectedObject;
  private HashMap<Object, Integer> objects; // ObjectID, dbID
  private HashMap<Object, Integer> newObjects; // ObjectID, dbID
  private HashMap<Object, Integer> editedObjects; // ObjectID, dbID
  private HashMap<Object, Integer> deletedObjects; // ObjectID, dbID

  private SidePanelController sidePanel;
  @FXML private AnchorPane Canvas;
  @FXML private ToggleGroup ToggleGroup1;

  private void flush_temporal_data() {
    if (!this.mode.equals(Mode.Polyline.toString())) {
      polylinePoints = new Vector<Double>();
      oldPolyline = null;
    }
    if (!this.mode.equals(Mode.Multipoint.toString())) {
      multipointPoints = new Vector<Double>();
      oldMultipoint = null;
    }
  }

  public void setSidePanel(SidePanelController controller) {
    sidePanel = controller;
  }

  public ArrayList getObjects() {

    ArrayList maps = new ArrayList<HashMap<Object, Integer>>();
    maps.add(newObjects);
    maps.add(editedObjects);
    maps.add(deletedObjects);
    return maps;
  }

  public void initialize() {
    polylinePoints = new Vector<Double>();
    multipointPoints = new Vector<Double>();
    objects = new HashMap<Object, Integer>();
    newObjects = new HashMap<Object, Integer>();
    editedObjects = new HashMap<Object, Integer>();
    deletedObjects = new HashMap<Object, Integer>();
  }

  private boolean validOperation(double minX, double minY, double maxX, double maxY) {
    if (minY > 0 && maxY < Canvas.getHeight() && minX > 0 && maxX < Canvas.getWidth()) return true;
    else return false;
  }

  private void selectObject(Node object) {
    if (selectedObject != null) selectedObject.setEffect(null);

    if (this.mode.equals(Mode.None.toString())) {
      DropShadow ds = new DropShadow();
      ds.setOffsetY(4.0f);
      ds.setOffsetX(4.0f);
      ds.setColor(Color.BLACK);

      object.setEffect(ds);
      selectedObject = object;
    }
  }

  @FXML
  public void CanvasClicked(MouseEvent event) {

    Shape s = null;
    Integer DB_DEFAULT_ID = -1;
    if (event.getButton() != MouseButton.PRIMARY) return;
    switch (this.mode) {
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
          Group g = createCollection(event.getX(), event.getY());
          Canvas.getChildren().add(g);
          newObjects.put(g, DB_DEFAULT_ID);
        }
        break;
      case "Multipoint":
        flush_temporal_data();
        if (oldMultipoint != null) {
          Canvas.getChildren().remove(oldMultipoint);
          objects.remove(oldMultipoint);
        }
        if (validOperation(
            event.getX() - (double) (pointRadius),
            event.getY() - (double) (pointRadius),
            event.getX() + (double) (pointRadius),
            event.getY() + (double) (pointRadius))) {
          this.multipointPoints.add(event.getX());
          this.multipointPoints.add(event.getY());
          Group g = createMultipoint();
          Canvas.getChildren().add(g);
          newObjects.put(g, DB_DEFAULT_ID);
        }
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
          this.polylinePoints.add(event.getX());
          this.polylinePoints.add(event.getY());
          Group g = createPolyline();
          Canvas.getChildren().add(g);
          newObjects.put(g, DB_DEFAULT_ID);
        }
        break;

      default:
        flush_temporal_data();
        break;
    }
  }

  private Rectangle createRect(double sceneX, double sceneY) {
    Rectangle rect =
        new Rectangle(sceneX - prefSize / 2, sceneY - prefSize / 2, prefSize, prefSize);
    rect.setFill(prefColor);
    rect.setStroke(Color.BLACK);
    rect.setCursor(Cursor.HAND);
    rect.setOnMousePressed(
        (t) -> {
          // REMOVING RECTANGLE
          if (this.mode.equals(Mode.Delete.toString())) {
            Canvas.getChildren().remove(rect);
            // check wheater object is in DB
            if (objects.containsKey(rect)) deletedObjects.put(rect, objects.get(rect));
            // object was newly created, not in DB
            objects.remove(rect);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Rectangle c = (Rectangle) (t.getSource());
          c.toFront();
//          System.out.println(c);
          selectObject(c);
          sidePanel.setActiveNode(c, objects.get(c));
        });
    rect.setOnMouseDragged(
        (t) -> {
          if (this.mode.equals(Mode.Move.toString())) {
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
          } else if (this.mode.equals(Mode.Resize.toString())) {
            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Rectangle c = (Rectangle) (t.getSource());
            if (validOperation(
                c.getX() + offsetX,
                c.getY() + offsetY,
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
    double radius;
    if (isPoint) {
      radius = pointRadius;
    } else {
      radius = prefSize;
    }

    Circle circle = new Circle(sceneX, sceneY, radius, prefColor);
    circle.setStroke(Color.BLACK);
    circle.setCursor(Cursor.HAND);

    circle.setOnMousePressed(
        (t) -> {
          if (this.mode.equals(Mode.Delete.toString())) {
            Canvas.getChildren().remove(circle);
            // check wheater object is in DB
            if (objects.containsKey(circle)) deletedObjects.put(circle, objects.get(circle));
            // object was newly created, not in DB
            objects.remove(circle);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Circle c = (Circle) (t.getSource());
          c.toFront();
          selectObject(c);
          sidePanel.setActiveNode(c, objects.get(c));
        });
    circle.setOnMouseDragged(
        (t) -> {
          if (this.mode.equals(Mode.Move.toString())) {

            double offsetX = t.getSceneX() - xCord;
            double offsetY = t.getSceneY() - yCord;

            Circle c = (Circle) (t.getSource());
            if (validOperation(
                c.getCenterX() + offsetX - c.getRadius(),
                c.getCenterY() + offsetY - c.getRadius(),
                c.getCenterX() + offsetX + c.getRadius(),
                c.getCenterY() + offsetY + c.getRadius())) {
              // check wheater object is in DB
              if (objects.containsKey(circle)) editedObjects.put(circle, objects.get(circle));

              c.setCenterX(c.getCenterX() + offsetX);
              c.setCenterY(c.getCenterY() + offsetY);

              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          } else if (this.mode.equals(Mode.Resize.toString())) {
            if (isPoint) return;
            double offset = t.getSceneX() - xCord;

            Circle c = (Circle) (t.getSource());
            if (validOperation(
                c.getCenterX() - offset - c.getRadius(),
                c.getCenterY() - offset - c.getRadius(),
                c.getCenterX() + offset + c.getRadius(),
                c.getCenterY() + offset + c.getRadius())) {
              // check wheater object is in DB
              if (objects.containsKey(circle)) editedObjects.put(circle, objects.get(circle));

              c.setRadius(c.getRadius() + offset);
              xCord = t.getSceneX();
              yCord = t.getSceneY();
            }
          }
        });
    return circle;
  }

  private Group createCollection(double sceneX, double sceneY) {

    Group group = new Group();

    for (int i = 0; i < groupCount; i++) {

      Circle circle = new Circle(sceneX + prefSize * 3 * i, sceneY, prefSize, prefColor);
      circle.setStroke(Color.BLACK);
      circle.setCursor(Cursor.HAND);

      final ContextMenu contextMenu = new ContextMenu();
      MenuItem delete = new MenuItem("Delete circle");

      contextMenu.getItems().add(delete);
      delete.setOnAction(
          event -> {
            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));
            circle.setVisible(false);

            for (Node n : group.getChildren()) {
              if (n.isVisible()) {
                return;
              }
              // last circle has been removed, so the whole group will be removed too
              Canvas.getChildren().remove(group);
              if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
              objects.remove(group);
            }
          });

      circle.setOnContextMenuRequested(
          event -> {
            int index = 0;
            Vector<Integer> invisible = new Vector<Integer>();
            Vector<Integer> visible = new Vector<Integer>();
            for (Node n : group.getChildren()) {
              if (!n.isVisible()) {
                invisible.add(index);
              } else visible.add(index);
              index++;
            }
            //            toFront.setOnAction(
            //                t -> {
            //                  if (group.getChildren().size() == invisible.size()) { // vsetky su
            // zmazane
            //
            // group.getChildren().get(invisible.firstElement()).setVisible(true);
            //                  } else if (group.getChildren().size() == visible.size()) { // nic
            // neni zmazane
            //                    // TD:pridaj na zaciatok
            //                  } else {
            //                    if (invisible.firstElement() < visible.firstElement()) {
            //                      group.getChildren().get(visible.firstElement() -
            // 1).setVisible(true);
            //                    }
            //                  }
            //                });
            //            toBack.setOnAction(
            //                t -> {
            //                  if (group.getChildren().size() == invisible.size()) { // vsetky su
            // zmazane
            //                    group.getChildren().get(invisible.lastElement()).setVisible(true);
            //                  } else if (group.getChildren().size() == visible.size()) { // nic
            // neni zmazane
            //                    // TD:pridaj na koniec
            //
            //                  } else {
            //                    if (invisible.lastElement() > visible.lastElement()) {
            //                      group.getChildren().get(visible.lastElement() +
            // 1).setVisible(true);
            //                    }
            //                  }
            //                });
            if (invisible.size() > 0) {
              Menu add = new Menu("Add circle");
              if (add.getItems().size() < 3) {
                for (Integer a : invisible) {
                  //                if( a== 0 || a==group.getChildren().size())
                  //                  continue;
                  MenuItem item = new MenuItem("On index " + a.toString());
                  item.setOnAction(
                      t -> {
                        if (objects.containsKey(group))
                          editedObjects.put(group, objects.get(group));
                        group.getChildren().get(a).setVisible(true);
                      });
                  add.getItems().add(item);
//                  System.out.println(a);
                }
              }
              contextMenu.getItems().add(add);
            }
            contextMenu.show(circle, event.getScreenX(), event.getScreenY());
          });
      group.getChildren().add(circle);
    }
    group.setOnMousePressed(
        (t) -> {
          if (this.mode.equals(Mode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            objects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          sidePanel.setActiveNode(c, objects.get(c));
        });
    group.setOnMouseDragged(
        (t) -> {
          if (this.mode.equals(Mode.Move.toString())) {

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
          } else if (this.mode.equals(Mode.Resize.toString())) {

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
              int i = 0;
              for (Node node : c.getChildren()) {
                //                ((Circle) node).setCenterX(((Circle) node).getCenterX() + offset);
                ((Circle) node).setRadius(((Circle) node).getRadius() + offset / 2);

                i++;
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
    group.getChildren().add(p);

    double[] arr = pointsDataToArray(polylinePoints);
    for (int i = 0; i < arr.length; i += 2) {
      Circle c = new Circle(arr[i], arr[i + 1], pointRadius, prefColor);
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
              objects.remove(group);
              return;
            }

            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

            //            System.out.println("index =" + (group.getChildren().indexOf(c) - 1));
            int index = group.getChildren().indexOf(c) - 1;

            //            System.out.println("Suradnice bodu: " + c.getCenterX() + ", "
            // +c.getCenterY());
            //            System.out.println(((Polyline)
            // (group.getChildren().get(0))).getPoints().get(index * 2) + ", " +((Polyline)
            // (group.getChildren().get(0))).getPoints().get(index * 2+1) );

            //            System.out.println("Mazem" + ((Polyline)
            // (group.getChildren().get(0))).getPoints().get(index * 2));
            ((Polyline) (group.getChildren().get(0)))
                .getPoints()
                .remove(index * 2); // get(index * 2);
            //            System.out.println("Mazem" + ((Polyline)
            // (group.getChildren().get(0))).getPoints().get(index * 2));
            ((Polyline) (group.getChildren().get(0)))
                .getPoints()
                .remove(index * 2); // get(index * 2);
            group.getChildren().remove(c);
            //
            //            Polyline p12 = new Polyline();
            //            for (Node n : group.getChildren()) {
            //              if (n instanceof Polyline) p12 = (Polyline) (n);
            //            }
            //            int pointSize = p12.getPoints().size();
            //            for (int i1 = 0; i1 < pointSize; i1 += 2) {
            //              if (p12.getPoints().get(i1).equals(c.getCenterX())
            //                  && p12.getPoints().get(i1 + 1).equals(c.getCenterY())) {
            //                p12.getPoints().remove(i1);
            //                p12.getPoints().remove(i1);
            //                polylinePoints.remove(i1);
            //                polylinePoints.remove(i1);
            //                break;
            //              }
            //            }
            //            //

          });

      add.setOnAction(
          event -> {
            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

            Polyline p1 = new Polyline();
            for (Node n : group.getChildren()) {
              if (n instanceof Polyline) p1 = (Polyline) (n);
              break;
            }
            oldPolyline = group;
            mode = Mode.Polyline.toString();
            polylinePoints = new Vector<>();
            for (Double n : p1.getPoints()) {
              polylinePoints.add(n);
            }
          });

      c.setOnContextMenuRequested(
          new EventHandler<ContextMenuEvent>() {

            @Override
            public void handle(ContextMenuEvent event) {
              contextMenu.show(c, event.getScreenX(), event.getScreenY());
            }
          });

      group.getChildren().add(c);
    }

    group.setOnMousePressed(
        (t) -> {
          if (this.mode.equals(Mode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            if (objects.containsKey(group)) deletedObjects.put(group, objects.get(group));
            objects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          sidePanel.setActiveNode(c, objects.get(c));
//          System.out.println(c.getChildren().get(1));
        });
    group.setOnMouseDragged(
        (t) -> {
          if (this.mode.equals(Mode.Move.toString())) {

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
                  ((Polyline) n).setLayoutX(((Polyline) n).getLayoutX() + offsetX);
                  ((Polyline) n).setLayoutY(((Polyline) n).getLayoutY() + offsetY);
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

  private Group createMultipoint() {

    Group group = new Group();
    double[] arr = pointsDataToArray(multipointPoints);
    for (int i = 0; i < arr.length; i += 2) {
      Circle c = new Circle(arr[i], arr[i + 1], pointRadius, prefColor);
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
              objects.remove(group);
              return;
            }

            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

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
            if (objects.containsKey(group)) editedObjects.put(group, objects.get(group));

            oldMultipoint = group;
            mode = Mode.Multipoint.toString();
            multipointPoints = new Vector<>();
            for (Node n : group.getChildren()) {
              if (n instanceof Circle) {
                multipointPoints.add(((Circle) n).getCenterX());
                multipointPoints.add(((Circle) n).getCenterY());
              }
            }
//            System.out.println(multipointPoints);
          });

      c.setOnContextMenuRequested(
          event -> contextMenu.show(c, event.getScreenX(), event.getScreenY()));

      group.getChildren().add(c);
    }

    group.setOnMousePressed(
        (t) -> {
          if (this.mode.equals(Mode.Delete.toString())) {
            Canvas.getChildren().remove(group);
            objects.remove(group);
            return;
          }
          xCord = t.getSceneX();
          yCord = t.getSceneY();

          Group c = (Group) (t.getSource());
          c.toFront();
          selectObject(c);
          sidePanel.setActiveNode(c, objects.get(c));
        });
    group.setOnMouseDragged(
        (t) -> {
          if (this.mode.equals(Mode.Move.toString())) {

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

  public void changeMode() {

    if (newObjects.size() > 0 || editedObjects.size() > 0 || deletedObjects.size() > 0) {
      // deleted objects wont be edited / added into the DB
      for (HashMap.Entry<Object, Integer> entry : deletedObjects.entrySet()) {
        if (newObjects.containsKey(entry.getKey())) {
          newObjects.remove(entry.getKey());
        }
        if (editedObjects.containsKey(entry.getKey())) {
          editedObjects.remove(entry.getKey());
        }
        // remove also from local map of saved DB objects
        objects.remove(entry.getKey());
      }

      // Saving changes to DB
      sidePanel.saveStateToDb(newObjects, editedObjects, deletedObjects);
      // adding new objects to local map of saved DB objects
      for (HashMap.Entry<Object, Integer> entry : newObjects.entrySet()) {
        objects.put(entry.getKey(), entry.getValue());

        // create new instances of maps
        newObjects = new HashMap<Object, Integer>();
        editedObjects = new HashMap<Object, Integer>();
        deletedObjects = new HashMap<Object, Integer>();
      }
    }

    ToggleButton selected = (ToggleButton) this.ToggleGroup1.getSelectedToggle();
    flush_temporal_data();

    if (selected == null) {
      this.mode = Mode.None.toString();
    } else this.mode = selected.getText();

//    // PRINTING
//    System.out.println("------------------------------------");
//    System.out.println("Button clicked, actual mode: " + this.mode);
//
//    if (selected != null) System.out.println("Selected button = " + selected.getText());
//    else System.out.println("Selected button = " + null);
//    System.out.println("------------------------------------");
  }

  protected double[] pointsDataToArray(Vector points) {
    double[] target = new double[points.size()];
    for (int i = 0; i < target.length; i++) {
      target[i] = (double) points.get(i);
    }
    return target;
  }

  public enum Mode {
    Rect,
    Circle,
    Collection,
    Polyline,
    Multipoint,
    Point,
    Delete,
    Move,
    Resize,
    None
  }
}
