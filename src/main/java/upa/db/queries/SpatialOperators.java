package upa.db.queries;

import oracle.jdbc.OracleResultSet;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpatialOperators {
  private static final String SQL_SELECT_NN_OF_OBJECT_BEGIN =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v.o_id, SDO_NN_DISTANCE(1) dist "
          + "FROM Village v "
          + "WHERE SDO_NN(v.geometry, (SELECT geometry FROM Village WHERE o_id = ?), ?, 1) = 'TRUE' "
          + "AND v.o_type IN (";
  private static final String SQL_SELECT_NN_OF_OBJECT_END = ") ORDER BY dist";
  private static final String SQL_SELECT_RELATED_OBJECTS_BEGIN =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v.o_id "
          + "FROM Village v "
          + "WHERE SDO_RELATE(v.geometry, (SELECT geometry FROM Village WHERE o_id = ?), ?) = 'TRUE' "
          + "AND v.o_type IN (";
  private static final String SQL_SELECT_RELATED_OBJECTS_END = ") ";
  private static final String SQL_UNION_ALL = "UNION ALL ";

  private static String build_object_types_expr(
      String[] o_types, String sql_select_begin, String sql_select_end) {
    StringBuilder in_array_builder = new StringBuilder();
    for (int i = 0; i < o_types.length; i++) {
      in_array_builder.append("?");
      if (i + 1 < o_types.length) {
        in_array_builder.append(",");
      }
    }
    return sql_select_begin + in_array_builder.toString() + sql_select_end;
  }

  private static void set_param_of_statement(
      PreparedStatement prepared_statement,
      int o_id,
      String query_param,
      String[] o_types,
      int start_idx)
      throws SQLException {
    prepared_statement.setInt(start_idx, o_id);
    prepared_statement.setString(start_idx + 1, query_param);
    for (int i = 0; i < o_types.length; i++) {
      prepared_statement.setString(start_idx + i + 2, o_types[i]);
    }
  }

  public static double[] get_nearest_neighbours_of_object(
      Connection conn, int o_id, int num_res, int distance, String[] o_types) throws SQLException {
    String sql_select_nn_of_object =
        build_object_types_expr(
            o_types, SQL_SELECT_NN_OF_OBJECT_BEGIN, SQL_SELECT_NN_OF_OBJECT_END);
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select_nn_of_object)) {
      set_param_of_statement(
          prepared_statement, o_id, "sdo_num_res=" + num_res + " distance=" + distance, o_types, 1);
      double[] o_ids = new double[0];
      try (ResultSet result_set = prepared_statement.executeQuery()) {
        final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
        while (oracle_result_set.next()) {
          o_ids =
              ArrayUtils.addAll(o_ids, oracle_result_set.getInt(1), oracle_result_set.getDouble(2));
        }
        return o_ids;
      }
    }
  }

  private static String build_query(Mask[] masks, String[] o_types) {
    StringBuilder sql_select_related_objects = new StringBuilder();
    for (int i = 0; i < masks.length; i++) {
      sql_select_related_objects.append(
          build_object_types_expr(
              o_types, SQL_SELECT_RELATED_OBJECTS_BEGIN, SQL_SELECT_RELATED_OBJECTS_END));
      if (i + 1 < masks.length) {
        sql_select_related_objects.append(SQL_UNION_ALL);
      }
    }
    return sql_select_related_objects.toString();
  }

  public static int[] get_related_objects_of_object(
      Connection conn, int o_id, Mask[] masks, String[] o_types) throws SQLException {
    String sql_select_related_objects = build_query(masks, o_types);
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select_related_objects)) {
      for (int i = 0; i < masks.length; i++) {
        set_param_of_statement(
            prepared_statement,
            o_id,
            "mask=" + masks[i].name(),
            o_types,
            i * (2 + o_types.length) + 1);
      }
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
