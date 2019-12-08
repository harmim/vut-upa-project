package upa.db.queries;

import oracle.jdbc.OracleResultSet;
import org.apache.commons.lang3.ArrayUtils;
import upa.db.GeneralDB.NotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpatialOperators {
  private static final String OBJECT_ID_EQ = "o_id=";
  private static final String V2_OBJECT_ID_EQ = "v2.o_id=";
  private static final String V2_OBJECT_TYPES_IN = "v2.o_type IN (%s)";
  private static final String SQL_SELECT_NN_OF_OBJECT =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id, SDO_NN_DISTANCE(1) dist "
          + "FROM Village v1, Village v2 "
          + "WHERE v1.o_id <> v2.o_id AND SDO_NN(v1.geometry, v2.geometry, %s, 1) = 'TRUE' AND %s"
          + "AND v1.o_type IN (%s) ORDER BY dist";
  private static final String SQL_SELECT_RELATED_OBJECTS =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id "
          + "FROM Village v1, Village v2 "
          + "WHERE v1.o_id <> v2.o_id AND SDO_RELATE(v1.geometry, v2.geometry, %s) = 'TRUE' AND %s "
          + "AND v1.o_type IN (%s) ";
  private static final String SQL_UNION_ALL = "UNION ALL ";
  private static final String SQL_SELECT_AREA_OF_OBJECT =
      "SELECT o_id, SDO_GEOM.SDO_AREA(geometry, 0.005) FROM Village WHERE %s";
  private static final String SQL_SELECT_LENGTH_OF_OBJECT =
      "SELECT o_id, SDO_GEOM.SDO_LENGTH(geometry, 0.005) FROM Village WHERE %s";

  private static String build_object_types_expr(String[] o_types) {
    StringBuilder in_array_builder = new StringBuilder();
    for (int i = 0; i < o_types.length; i++) {
      in_array_builder.append("'").append(o_types[i]).append("'");
      if (i + 1 < o_types.length) {
        in_array_builder.append(",");
      }
    }
    return in_array_builder.toString();
  }

  public static double get_area_of_object_by_id(Connection conn, int o_id)
      throws SQLException, NotFoundException {
    return execute_sql_query_get_value(
        conn, String.format(SQL_SELECT_AREA_OF_OBJECT, OBJECT_ID_EQ + o_id));
  }

  public static double get_length_of_object_by_id(Connection conn, int o_id)
      throws SQLException, NotFoundException {
    return execute_sql_query_get_value(
        conn, String.format(SQL_SELECT_LENGTH_OF_OBJECT, OBJECT_ID_EQ + o_id));
  }

  public static int[] get_nearest_neighbours_of_object_by_id(
      Connection conn, int o_id, int num_res, int distance, String[] o_types) throws SQLException {
    return get_nearest_neighbours_of_object(
        conn, num_res, distance, o_types, V2_OBJECT_ID_EQ + o_id);
  }

  public static int[] get_nearest_neighbours_of_object_by_type(
      Connection conn, String[] v2_o_types, int num_res, int distance, String[] v1_o_types)
      throws SQLException {
    return get_nearest_neighbours_of_object(
        conn,
        num_res,
        distance,
        v1_o_types,
        String.format(V2_OBJECT_TYPES_IN, build_object_types_expr(v2_o_types)));
  }

  private static int[] get_nearest_neighbours_of_object(
      Connection conn, int num_res, int distance, String[] v1_o_types, String query_format)
      throws SQLException {
    String v1_o_type_str = build_object_types_expr(v1_o_types);
    String sdo_nn_param = "'sdo_num_res=" + num_res + " distance=" + distance + "'";
    String sql_select_nn_of_object =
        String.format(SQL_SELECT_NN_OF_OBJECT, sdo_nn_param, query_format, v1_o_type_str);
    return execute_sql_query_get_ids(conn, sql_select_nn_of_object);
  }

  public static int[] get_related_objects_of_object_by_id(
      Connection conn, int o_id, Mask[] masks, String[] v1_o_types) throws SQLException {
    StringBuilder sql_select_related_objects = new StringBuilder();
    for (int i = 0; i < masks.length; i++) {
      get_related_objects_of_object(
          masks, v1_o_types, sql_select_related_objects, i, V2_OBJECT_ID_EQ + o_id);
    }
    return execute_sql_query_get_ids(conn, sql_select_related_objects.toString());
  }

  public static int[] get_related_objects_of_object_by_type(
      Connection conn, String[] v2_o_types, Mask[] masks, String[] v1_o_types) throws SQLException {
    StringBuilder sql_select_related_objects = new StringBuilder();
    for (int i = 0; i < masks.length; i++) {
      get_related_objects_of_object(
          masks,
          v1_o_types,
          sql_select_related_objects,
          i,
          String.format(V2_OBJECT_TYPES_IN, build_object_types_expr(v2_o_types)));
    }
    return execute_sql_query_get_ids(conn, sql_select_related_objects.toString());
  }

  private static void get_related_objects_of_object(
      Mask[] masks,
      String[] v1_o_types,
      StringBuilder sql_select_related_objects,
      int i,
      String format) {
    String v1_o_type_str = build_object_types_expr(v1_o_types);
    String sdo_relate_param = "'mask=" + masks[i] + "'";
    sql_select_related_objects.append(
        String.format(SQL_SELECT_RELATED_OBJECTS, sdo_relate_param, format, v1_o_type_str));
    if (i + 1 < masks.length) {
      sql_select_related_objects.append(SQL_UNION_ALL);
    }
  }

  public static double execute_sql_query_get_value(Connection conn, String sql_select)
      throws SQLException, NotFoundException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select)) {
      try (ResultSet result_set = prepared_statement.executeQuery()) {
        final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
        if (oracle_result_set.next()) {
          return oracle_result_set.getDouble(2);
        } else {
          throw new NotFoundException();
        }
      }
    }
  }

  public static int[] execute_sql_query_get_ids(Connection conn, String sql_select)
      throws SQLException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select)) {
      int[] o_ids = new int[0];
      try (ResultSet result_set = prepared_statement.executeQuery()) {
        final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
        while (oracle_result_set.next()) {
          o_ids = ArrayUtils.addAll(o_ids, oracle_result_set.getInt(1));
        }
        return o_ids;
      }
    }
  }
}
