package upa.db.queries;

import oracle.jdbc.OracleResultSet;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpatialOperators {
  private static final String SQL_SELECT_NN_OF_OBJECT_BEGIN =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id, SDO_NN_DISTANCE(1) dist "
          + "FROM Village v1, Village v2 "
          + "WHERE v1.o_id <> v2.o_id AND SDO_NN(v1.geometry, v2.geometry, %s, 1) = 'TRUE' AND v2.o_id = ?"
          + "AND v1.o_type IN (";
  private static final String SQL_SELECT_NN_OF_OBJECT_END = ") ORDER BY dist";
  private static final String SQL_SELECT_RELATED_OBJECTS_BEGIN =
      "SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id "
          + "FROM Village v1, Village v2 "
          + "WHERE v1.o_id <> v2.o_id AND SDO_RELATE(v1.geometry, v2.geometry, %s) = 'TRUE' AND v2.o_id = ? "
          + "AND v1.o_type IN (";
  private static final String SQL_SELECT_RELATED_OBJECTS_END = ") ";
  private static final String SQL_UNION_ALL = "UNION ALL ";

  private static String build_object_types_expr(
      String[] o_types, String sql_select_begin, String sql_select_end) {
    StringBuilder in_array_builder = new StringBuilder();
    for (int i = 0; i < o_types.length; i++) {
      in_array_builder.append("'").append(o_types[i]).append("'");
      if (i + 1 < o_types.length) {
        in_array_builder.append(",");
      }
    }
    return sql_select_begin + in_array_builder.toString() + sql_select_end;
  }

  public static double[] get_nearest_neighbours_of_object(
      Connection conn, int o_id, int num_res, int distance, String[] o_types) throws SQLException {
    String sql_select_nn_of_object =
        build_object_types_expr(
            o_types,
            String.format(
                SQL_SELECT_NN_OF_OBJECT_BEGIN,
                "'sdo_num_res=" + num_res + " distance=" + distance + "'"),
            SQL_SELECT_NN_OF_OBJECT_END);
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select_nn_of_object)) {
      prepared_statement.setInt(1, o_id);
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
              o_types,
              String.format(SQL_SELECT_RELATED_OBJECTS_BEGIN, "'mask=" + masks[i] + "'"),
              SQL_SELECT_RELATED_OBJECTS_END));
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
      for (int i = 1; i <= masks.length; i++) {
        prepared_statement.setInt(i, o_id);
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
