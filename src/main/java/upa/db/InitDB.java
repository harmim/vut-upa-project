package upa.db;

import javafx.scene.image.Image;
import oracle.jdbc.pool.OracleDataSource;
import upa.db.multimedia.DBImage;
import upa.db.queries.Mask;
import upa.db.queries.SpatialOperators;
import upa.db.spatial.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class InitDB {

  public static void start() throws Exception {
    System.out.println("*** STARTING INIT DB ***");
    try {
      // create a OracleDataSource instance
      OracleDataSource ods = new OracleDataSource();
      ods.setURL("jdbc:oracle:thin:@//gort.fit.vutbr.cz:1521/orclpdb");
      /**
       * * To set System properties, run the Java VM with the following at its command line: ...
       * -Dlogin=LOGIN_TO_ORACLE_DB -Dpassword=PASSWORD_TO_ORACLE_DB ... or set the project
       * properties (in NetBeans: File / Project Properties / Run / VM Options)
       */
      ods.setUser(System.getProperty("login"));
      ods.setPassword(System.getProperty("password"));

      //            save_images_to_db(ods);
      save_object_to_db(ods);
      check_db_queries(ods);

    } catch (SQLException sqlEx) {
      System.err.println("SQLException: " + sqlEx.getMessage());
    }
  }

  private static void save_images_to_db(OracleDataSource ods) {
    // connect to the database
    File images_dir = new File("./images/");
    File[] image_name_list = images_dir.listFiles();
    if (image_name_list != null) {
      try (Connection conn = ods.getConnection()) {
        // save images to database
        for (File image_name : image_name_list) {
          DBImage.save_image_from_file(conn, 0, image_name.getPath());
        }
        System.out.println("*** SAVED IMAGES DONE ***");

        // delete images from database
        DBImage.delete_image(conn, 1);
        System.out.println("*** DELETE IMAGE DONE ***");

        // change images in db
        DBImage.process_image(conn, 3, "rotate", 90.0, 0.0, 0.0, 0.0);
        DBImage.process_image(conn, 3, "cut", 0.0, 0.0, 200.0, 200.0);
        DBImage.process_image(conn, 3, "mirror", 0.0, 0.0, 0.0, 0.0);
        DBImage.process_image(conn, 3, "scale", 2.25, 0.0, 0.0, 0.0);
        DBImage.process_image(conn, 3, "monochrome", 0.0, 0.0, 0.0, 0.0);

        // load most similar images from database
        int sim_image_id = DBImage.find_most_similar_image(conn, 3, 0.3, 0.3, 0.1, 0.3);
        Image load_image = DBImage.load_image(conn, sim_image_id);
//        load_image.getDataInFile("./src/load_image.gif");
        System.out.println("*** LOAD SIMILAR IMAGE DONE ***");

      } catch (SQLException | DBImage.NotFoundException | IOException sqlEx) {
        System.err.println("SQLException: " + sqlEx.getMessage());
      }
    }
  }

  private static void save_object_to_db(OracleDataSource ods) throws Exception {
    try (Connection conn = ods.getConnection()) {
      final boolean previous_auto_commit = conn.getAutoCommit();
      conn.setAutoCommit(false);
      // rectangle
      //            Rectangle.update_geometry_in_db(conn, 1, new double[]{200,200, 300,300});
      //            Rectangle.insert_new_to_db(conn, "Z", "House", new double[]{20,20, 120,120});
      //            SpatialObject.delete_spatial_object_from_db(conn, 3);
      //            Rectangle.insert_new_to_db(conn, "X", "Building", new double[]{20,20, 120,120});

      Circle.insert_new_circle(conn, "C", "Circle", new double[] {15.0, 15.0, 5.0});
      Circle.update_geometry_of_circle(conn, 3, new double[] {20.00, 20.00, 10.0});

      //            // line-string
      StraightLineString.insert_new_line_string(
          conn, "K", "Kine", new double[] {130, 65, 180, 75, 205, 55, 250, 85, 275, 45});
      StraightLineString.delete_points_from_line_string(conn, 4, new double[] {180, 75, 130, 65});
      StraightLineString.add_points_to_line_string(conn, 4, new double[] {300, 50, 250, 90});

      // circles
      CircleCollection.insert_new_collection_to_db(
          conn, "F-circles", "trees", new double[] {180.0, 150.0, 12.0}, 5, true);

      CircleCollection.insert_new_collection_to_db(
          conn, "G-circles", "trees", new double[] {50.0, 65.0, 8.0}, 5, false);

      Circle.insert_new_circle(conn, "B1", "bushes1", new double[] {0, 0, 5});
      Circle.insert_new_circle(conn, "B1", "bushes1", new double[] {5, 5, 5});
      Circle.insert_new_circle(conn, "B1", "bushes1", new double[] {10, 10, 5});

      CircleCollection.delete_object_from_collection(conn, 5, new int[] {0, 3}, 6);
      CircleCollection.update_coordinates_of_collection(conn, 5, 100, 130);
      CircleCollection.add_circles_to_collection(conn, 5, new int[] {0, 3});
      CircleCollection.update_diameter_of_circles_in_collection(conn, 5, 20);
      CircleCollection.update_coordinates_of_collection(conn, 5, 50, 150);

      CircleCollection.delete_object_from_collection(conn, 6, new int[] {0, 4}, 6);
      CircleCollection.update_coordinates_of_collection(conn, 6, 30, 55);
      CircleCollection.add_circles_to_collection(conn, 6, new int[] {0, 4});
      CircleCollection.update_diameter_of_circles_in_collection(conn, 6, 20);
      CircleCollection.update_coordinates_of_collection(conn, 6, 200, 50);
      // multipoint
      MultiPoint.insert_new_multipoint(
          conn,
          "M",
          "MultiPoint",
          new double[] {25.0, 35.0, 35.0, 35.0, 45.0, 35.0, 25.0, 60.0, 35.0, 60.0, 45.0, 60.0});
      MultiPoint.delete_object_from_collection(conn, 10, new int[] {1, 5}, 2);
      MultiPoint.add_points_to_multipoint(conn, 10, new double[] {35.0, 35.0, 45.0, 60.0});
      MultiPoint.add_points_to_multipoint(conn, 10, new double[] {55.0, 60.0, 55.0, 35.0});
      MultiPoint.delete_points_from_multipoint(conn, 10, new double[] {25.0, 35.0, 35.0, 60.0});

      Point.insert_new_point(conn, "PPP", "PPPoint", new double[] {200, 200});
      Point.update_geometry_of_point(conn, 11, new double[] {150, 175});

      // rectangle
      Rectangle.insert_new_rectangle(conn, "Z", "T1", new double[] {5, 5, 120, 120});
      Circle.insert_new_circle(conn, "X", "T2", new double[] {30, 30, 20});

      conn.commit();
      conn.setAutoCommit(previous_auto_commit);
      conn.close();
    } catch (SQLException | IOException sqlEx) {
      System.err.println("SQLException: " + sqlEx.getMessage());
    }
  }

  private static void check_db_queries(OracleDataSource ods) {
    try (Connection conn = ods.getConnection()) {
      final boolean previous_auto_commit = conn.getAutoCommit();
      conn.setAutoCommit(false);

      int[] o_ids_by_id =
          SpatialOperators.get_nearest_neighbours_of_object_by_id(
              conn, 2, 8, 200, new String[] {"trees", "bushes1", "Kine"});
      System.out.println(Arrays.toString(o_ids_by_id));

      int[] o_ids_by_types =
          SpatialOperators.get_nearest_neighbours_of_object_by_type(
              conn,
              new String[] {"Line", "Kine"},
              8,
              200,
              new String[] {"trees", "bushes1", "Kine"});
      System.out.println(Arrays.toString(o_ids_by_types));

      int[] o_ids_by_id_r =
          SpatialOperators.get_related_objects_of_object_by_id(
              conn,
              12,
              new Mask[] {Mask.INSIDE, Mask.OVERLAPBDYINTERSECT},
              new String[] {"House", "bushes1", "Line", "T2"});
      System.out.println(Arrays.toString(o_ids_by_id_r));

      int[] o_ids_by_types_r =
          SpatialOperators.get_related_objects_of_object_by_type(
              conn,
              new String[] {"House", "T2"},
              new Mask[] {Mask.INSIDE, Mask.OVERLAPBDYINTERSECT},
              new String[] {"House", "T2"});
      System.out.println(Arrays.toString(o_ids_by_types_r));

      double area = SpatialOperators.get_area_of_object_by_id(conn, 6);
      System.out.printf("AREA = %g\n", area);
      double length = SpatialOperators.get_length_of_object_by_id(conn, 7);
      System.out.printf("LENGTH = %g\n", length);
      double diameter = SpatialOperators.get_diameter_of_object_by_id(conn, 4);
      System.out.printf("DIAMETER = %g\n", diameter);
      double distance = SpatialOperators.get_distance_between_obejcts(conn, 4, 7);
      System.out.printf("DIAMETER = %g\n", distance);

      conn.commit();
      conn.setAutoCommit(previous_auto_commit);
      conn.close();
    } catch (SQLException | GeneralDB.NotFoundException sqlEx) {
      System.err.println("SQLException: " + sqlEx.getMessage());
    }
  }
}
