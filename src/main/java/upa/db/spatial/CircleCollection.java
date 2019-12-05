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
  private static final double CIRCLE_SEGMENT_LENGTH = 1.5;
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

  public static void fulfill_sdo_points(double x_start, double y_start, double r, int arr_idx) {
    sdo_points.addAll(
        arr_idx,
        List.of(
            x_start + r / 2, y_start,
            x_start + r, y_start + r / 2,
            x_start + r / 2, y_start + r));
  }

  private static double[] compute_circle_coordinates(
      double x_start, double y_start, double r, int idx, boolean is_horizontal) {
    if (is_horizontal) {
      x_start += idx * CIRCLE_SEGMENT_LENGTH * r;
    } else {
      y_start += idx * CIRCLE_SEGMENT_LENGTH * r;
    }
    return new double[] {x_start, y_start};
  }

  public static void compute_coordinates_of_disjoint_circles(
      int n, double x_start, double y_start, double r, boolean is_horizontal) {
    int current_offset = 1;
    for (int i = 0; i < n; i++) {
      double[] coordinates = compute_circle_coordinates(x_start, y_start, r, i, is_horizontal);
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
    compute_coordinates_of_disjoint_circles(
        n, circles_data[0], circles_data[1], circles_data[2], horizontal);
    int o_id = insert_new_object_to_db(conn, o_name, o_type, create_geometry());
    CircleCollectionDB.update_data_collection(
        conn, o_id, circles_data[0], circles_data[1], n, CircleCollectionDB.SQL_INSERT_COLLECTION);
    flush_arrays();
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
    is_horizontal = get_distribution_of_collection(geometry.getOrdinatesArray());
  }

  private static void update_geometry_of_collection(Connection conn, int o_id) throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry());
  }

  public static void update_geometry_of_collection(
      Connection conn, int o_id, double r, double x_start, double y_start) throws Exception {
    set_geometry_properties(conn, o_id);
    update_object_coordinates(x_start, y_start, r);
    update_geometry_of_collection(conn, o_id);
    CircleCollectionDB.update_data_collection(
        conn,
        o_id,
        x_start,
        y_start,
        sdo_elem_info.size() / SDO_ELEM_SIZE,
        CircleCollectionDB.SQL_UPDATE_COLLECTION);
    flush_arrays();
  }

  public static void add_circles_to_collection(Connection conn, int o_id, int[] idxs)
      throws Exception {
    set_geometry_properties(conn, o_id);
    for (int idx : idxs) {
      if (idx == -1) {
        add_circle_to_begin(conn, o_id);
      } else {
        add_circle_to_free_place(Math.min(idx, sdo_elem_info.size() / SDO_ELEM_SIZE));
      }
      if (idx == -1 || idx == (sdo_elem_info.size() / SDO_ELEM_SIZE) - 1) {
        CircleCollectionDB.update_data_collection(
            conn,
            o_id,
            collection_data.get(0),
            collection_data.get(1),
            sdo_elem_info.size() / SDO_ELEM_SIZE,
            CircleCollectionDB.SQL_UPDATE_COLLECTION);
      }
    }
    update_geometry_of_collection(conn, o_id);
    flush_arrays();
  }

  private static void add_circle_to_begin(Connection conn, int o_id) throws SQLException {
    double r = (sdo_points.get(2) - sdo_points.get(0)) * 2;
    collection_data.set(
        is_horizontal ? 0 : 1,
        collection_data.get(is_horizontal ? 0 : 1) - CIRCLE_SEGMENT_LENGTH * r);
    add_circle_to_free_place(0);
  }

  private static void add_circle_to_free_place(int idx) {
    double[] coordinates =
        compute_circle_coordinates(
            collection_data.get(0),
            collection_data.get(1),
            (sdo_points.get(2) - sdo_points.get(0)) * 2,
            idx,
            is_horizontal);
    fulfill_sdo_points(
        coordinates[0],
        coordinates[1],
        (sdo_points.get(2) - sdo_points.get(0)) * 2,
        Math.min(sdo_points.size(), idx * SDO_ORD_SIZE));
    for (int i = idx * SDO_ELEM_SIZE; i < sdo_elem_info.size(); ) {
      sdo_elem_info.set(i, sdo_elem_info.get(i) + SDO_ORD_SIZE);
      i += SDO_ELEM_SIZE;
    }
    sdo_elem_info.addAll(
        Math.min(sdo_elem_info.size(), idx * SDO_ELEM_SIZE),
        List.of(idx * SDO_ORD_SIZE + 1, SDO_ETYPE, SDO_INTERPRETATION));
  }

  private static void update_object_coordinates(double x_start, double y_start, double r) {
    double old_r = (sdo_points.get(2) - sdo_points.get(0)) * 2;
    ArrayList<Double> copy_sdo_points = new ArrayList<>(sdo_points);
    sdo_points.clear();
    for (int i = 0; i < copy_sdo_points.size(); ) {
      int old_idx;
      if (is_horizontal) {
        old_idx =
            (int)
                ((copy_sdo_points.get(i) - old_r / 2 - collection_data.get(0))
                    / (CIRCLE_SEGMENT_LENGTH * old_r));
      } else {
        old_idx =
            (int)
                ((copy_sdo_points.get(i + 1) - collection_data.get(1))
                    / (CIRCLE_SEGMENT_LENGTH * old_r));
      }
      double[] coordinates =
          compute_circle_coordinates(x_start, y_start, r, old_idx, is_horizontal);
      fulfill_sdo_points(coordinates[0], coordinates[1], r, sdo_points.size());
      i += SDO_ORD_SIZE;
    }
  }

  private static boolean get_distribution_of_collection(double[] ord_array) {
    return ord_array[0] != ord_array[SDO_ORD_SIZE];
  }
}

class CircleCollectionDB extends Collection {
  public static final String SQL_INSERT_COLLECTION =
      "INSERT INTO CircleCollection (x_start, y_start, n, c_id) VALUES (?, ?, ?, ?)";
  public static final String SQL_UPDATE_COLLECTION =
      "UPDATE CircleCollection SET x_start = ?, y_start = ?, n = ? WHERE c_id = ?";
  private static final String SQL_SELECT_COLLECTION_FOR_UPDATE =
      "SELECT x_start, y_start, n FROM CircleCollection WHERE c_id = ?";

  public static void update_data_collection(
      Connection conn, int c_id, double x_start, double y_start, int n, String sql_query)
      throws SQLException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_query)) {
      prepared_statement.setInt(4, c_id);
      prepared_statement.setDouble(1, x_start);
      prepared_statement.setDouble(2, y_start);
      prepared_statement.setInt(3, n);
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
            result_set.getDouble(1), result_set.getDouble(2), result_set.getDouble(3)
          };
        } else {
          throw new NotFoundException();
        }
      }
    }
  }
}
