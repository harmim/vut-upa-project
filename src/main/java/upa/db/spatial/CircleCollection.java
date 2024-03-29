package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CircleCollection extends Collection {
  // geometry is a heterogeneous collection of elements
  private static final int SDO_ETYPE = 1003;
  // circle type described by three distinct non-colinear points, all on the circumference of the
  private static final int SDO_INTERPRETATION = 4;
  public static final double CIRCLE_SEGMENT_LENGTH = 1.5;
  private static final int SDO_ELEM_SIZE = 3;
  private static final int SDO_ORD_SIZE = 6;
  private static ArrayList<Double> sdo_points = new ArrayList<>();
  private static ArrayList<Integer> sdo_elem_info = new ArrayList<>();
  private static ArrayList<Double> collection_data = new ArrayList<>();
  private static boolean is_horizontal;

  private static void flush_arrays() {
    sdo_points.clear();
    sdo_elem_info.clear();
    collection_data.clear();
  }

  private static void fulfill_sdo_points(double x_start, double y_start, double r, int arr_idx) {
    sdo_points.addAll(
        arr_idx,
        List.of(
            x_start + r / 2, y_start,
            x_start + r, y_start + r / 2,
            x_start + r / 2, y_start + r));
  }

  private static void compute_circle_coordinats_after_change(double delta_r) {
    ArrayList<Double> copy_sdo_points = new ArrayList<>(sdo_points);
    sdo_points.clear();
    for (int i = 0; i < copy_sdo_points.size(); i += SDO_ORD_SIZE) {
      sdo_points.addAll(
          sdo_points.size(),
          List.of(
              copy_sdo_points.get(i),
              copy_sdo_points.get(i + 1) - delta_r / 2,
              copy_sdo_points.get(i + 2) + delta_r / 2,
              copy_sdo_points.get(i + 3),
              copy_sdo_points.get(i + 4),
              copy_sdo_points.get(i + 5) + delta_r / 2));
    }
  }

  private static double[] compute_circle_coordinates(
      double x_start, double y_start, double r, int idx) {
    if (is_horizontal) {
      x_start += idx * CIRCLE_SEGMENT_LENGTH * r;
    } else {
      y_start += idx * CIRCLE_SEGMENT_LENGTH * r;
    }
    return new double[] {x_start, y_start};
  }

  private static double[] compute_centers_of_circle(double[] first_center, int idx) {
    double[] circle_center;
    if (is_horizontal) {
      circle_center =
          new double[] {
            first_center[0] + idx * CIRCLE_SEGMENT_LENGTH * collection_data.get(2), first_center[1]
          };
    } else {
      circle_center =
          new double[] {
            first_center[0], first_center[1] + idx * CIRCLE_SEGMENT_LENGTH * collection_data.get(2)
          };
    }
    return circle_center;
  }

  private static double[] compute_first_center_of_circle(
      double x_start, double y_start, double r, int idx) {
    if (is_horizontal) {
      x_start = x_start + (0.5 * r) + (idx * CIRCLE_SEGMENT_LENGTH * r);
      y_start = y_start + r / 2;
    } else {
      y_start = y_start + (0.5 * r) + (idx * CIRCLE_SEGMENT_LENGTH * r);
      x_start = x_start + r / 2;
    }
    return new double[] {x_start, y_start};
  }

  public static void compute_coordinates_of_disjoint_circles(
      int n, double x_start, double y_start, double r) {
    int current_offset = 1;
    for (int i = 0; i < n; i++) {
      double[] coordinates = compute_circle_coordinates(x_start, y_start, r, i);
      fulfill_sdo_points(coordinates[0], coordinates[1], r, sdo_points.size());
      sdo_elem_info.addAll(List.of(current_offset, SDO_ETYPE, SDO_INTERPRETATION));
      current_offset += SDO_ORD_SIZE;
    }
  }

  private static JGeometry create_geometry() {
    double[] sdo_points_arr = ArrayUtils.toPrimitive(sdo_points.toArray(new Double[0]));
    int[] sdo_elem_info_arr = ArrayUtils.toPrimitive(sdo_elem_info.toArray(new Integer[0]));
    return new JGeometry(
        JGeometry.GTYPE_COLLECTION, SpatialObject.SDO_SRID, sdo_elem_info_arr, sdo_points_arr);
  }

  public static int insert_new_collection_to_db(
      Connection conn,
      String o_name,
      String o_type,
      double[] circles_data,
      int n,
      boolean horizontal)
      throws Exception {
    is_horizontal = horizontal;
    compute_coordinates_of_disjoint_circles(n, circles_data[0], circles_data[1], circles_data[2]);
    int o_id = insert_new_object(conn, o_name, o_type, create_geometry());
    CircleCollectionDB.update_data_collection(
        conn,
        o_id,
        circles_data[0],
        circles_data[1],
        circles_data[2],
        n,
        CircleCollectionDB.SQL_INSERT_COLLECTION);
    flush_arrays();
    conn.close();
    return o_id;
  }

  private static void set_geometry_properties(Connection conn, int o_id) throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    collection_data =
        (ArrayList<Double>)
            Arrays.stream(CircleCollectionDB.select_collection_data_for_update(conn, o_id))
                .boxed()
                .collect(Collectors.toList());
    sdo_elem_info =
        (ArrayList<Integer>)
            Arrays.stream(geometry.getElemInfo()).boxed().collect(Collectors.toList());
    sdo_points =
        (ArrayList<Double>)
            Arrays.stream(geometry.getOrdinatesArray()).boxed().collect(Collectors.toList());
    is_horizontal = true;
  }

  private static void update_geometry_of_collection(Connection conn, int o_id) throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry());
  }

  public static void update_diameter_of_circles_in_collection(
      Connection conn, int o_id, double new_r) throws Exception {
    set_geometry_properties(conn, o_id);
    compute_circle_coordinats_after_change(new_r - (sdo_points.get(2) - sdo_points.get(0)) * 2);
    update_geometry_of_collection(conn, o_id);
    CircleCollectionDB.update_data_collection(
        conn,
        o_id,
        sdo_points.get(0) - new_r / 2,
        sdo_points.get(1),
        collection_data.get(2),
        sdo_elem_info.size() / SDO_ELEM_SIZE,
        CircleCollectionDB.SQL_UPDATE_COLLECTION);
    flush_arrays();
    conn.close();
  }

  public static void update_coordinates_of_collection(
      Connection conn, int o_id, double x_start, double y_start, int[] current_circles)
      throws Exception {
    set_geometry_properties(conn, o_id);
    update_object_coordinates(x_start, y_start, current_circles);
    update_geometry_of_collection(conn, o_id);
    CircleCollectionDB.update_data_collection(
        conn,
        o_id,
        x_start,
        y_start,
        collection_data.get(2),
        sdo_elem_info.size() / SDO_ELEM_SIZE,
        CircleCollectionDB.SQL_UPDATE_COLLECTION);
    flush_arrays();
    conn.close();
  }

  public static void add_circles_to_collection(Connection conn, int o_id, int[] idxs)
      throws Exception {
    set_geometry_properties(conn, o_id);
    double r = (sdo_points.get(2) - sdo_points.get(0)) * 2;
    double[] circle_center;
    double[] first_circle_center =
            compute_first_center_of_circle(collection_data.get(0), collection_data.get(1), r, 0);
    for (int idx : idxs) {
      if (idx == 0) {
        circle_center = first_circle_center;
      } else {
        circle_center = compute_centers_of_circle(first_circle_center, idx);
      }
      double circle_x_start = circle_center[0] - r / 2;
      double circle_y_start = circle_center[1] - r / 2;
      fulfill_sdo_points(circle_x_start, circle_y_start, r, sdo_points.size());
      for (int j = idx * SDO_ELEM_SIZE; j < sdo_elem_info.size(); j += SDO_ELEM_SIZE) {
        sdo_elem_info.set(j, sdo_elem_info.get(j) + SDO_ORD_SIZE);
      }
      sdo_elem_info.addAll(
          Math.min(idx * SDO_ELEM_SIZE, sdo_elem_info.size()),
          List.of(
              Math.min(
                  idx * SDO_ORD_SIZE + 1,
                  sdo_elem_info.get(sdo_elem_info.size() - 3) + SDO_ORD_SIZE),
              SDO_ETYPE,
              SDO_INTERPRETATION));
    }
    update_geometry_of_collection(conn, o_id);
    flush_arrays();
    conn.close();
  }

  private static void update_object_coordinates(
      double x_start, double y_start, int[] current_circles) {
    double r = (sdo_points.get(2) - sdo_points.get(0)) * 2;
    ArrayList<Double> copy_sdo_points = new ArrayList<>(sdo_points);
    sdo_points.clear();
    double[] first_circle_center = new double[0];
    double[] circle_center;
    int j = 0;
    for (int i = 0; i < copy_sdo_points.size(); i += SDO_ORD_SIZE) {
      int idx = current_circles[j++];
      if (i == 0) {
        first_circle_center = compute_first_center_of_circle(x_start, y_start, r, 0);
        circle_center = first_circle_center;
      } else {
        circle_center = compute_centers_of_circle(first_circle_center, idx);
      }
      double circle_x_start = circle_center[0] - r / 2;
      double circle_y_start = circle_center[1] - r / 2;
      fulfill_sdo_points(circle_x_start, circle_y_start, r, sdo_points.size());
    }
  }

  //  private static boolean get_distribution_of_collection(double[] ord_array) {
  //    return ord_array[0] != ord_array[SDO_ORD_SIZE];
  //  }

  public static void delete_circle_collection(Connection conn, int o_id) throws SQLException, NotFoundException {
    CircleCollectionDB.delete_circle_collection_data(conn, o_id);
    CircleCollection.delete_object(conn, o_id);
  }
}

class CircleCollectionDB extends Collection {
  public static final String SQL_INSERT_COLLECTION =
      "INSERT INTO CircleCollection (x_start, y_start, r0, n, c_id) VALUES (?, ?, ?, ?, ?)";
  public static final String SQL_UPDATE_COLLECTION =
      "UPDATE CircleCollection SET x_start = ?, y_start = ?, r0 = ?, n = ? WHERE c_id = ?";
  private static final String SQL_SELECT_COLLECTION_FOR_UPDATE =
      "SELECT x_start, y_start, r0, n FROM CircleCollection WHERE c_id = ?";
  private static final String SQL_DELETE_COLLECTION = "DELETE FROM CircleCollection WHERE c_id = ?";

  public static void update_data_collection(
      Connection conn,
      int c_id,
      double x_start,
      double y_start,
      double r_0,
      int n,
      String sql_query)
      throws SQLException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_query)) {
      prepared_statement.setInt(5, c_id);
      prepared_statement.setDouble(1, x_start);
      prepared_statement.setDouble(2, y_start);
      prepared_statement.setDouble(3, r_0);
      prepared_statement.setInt(4, n);
      prepared_statement.executeUpdate();
    }
  }

  public static double[] select_collection_data_for_update(Connection conn, int c_id)
      throws SQLException, NotFoundException {
    try (PreparedStatement prepare_statement =
        conn.prepareStatement(SQL_SELECT_COLLECTION_FOR_UPDATE)) {
      prepare_statement.setInt(1, c_id);
      try (ResultSet result_set = prepare_statement.executeQuery()) {
        if (result_set.next()) {
          return new double[] {
            result_set.getDouble(1),
            result_set.getDouble(2),
            result_set.getDouble(3),
            result_set.getDouble(4)
          };
        } else {
          throw new NotFoundException();
        }
      }
    }
  }

  public static void delete_circle_collection_data(Connection conn, int c_id) throws SQLException {
    delete_object(conn, SQL_DELETE_COLLECTION, c_id);
  }
}
