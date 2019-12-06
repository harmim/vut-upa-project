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

  private static String build_query(String[] o_types) {
    StringBuilder in_array_builder = new StringBuilder();
    for (int i = 0; i < o_types.length; i++) {
      in_array_builder.append("?");
      if (i + 1 < o_types.length) {
        in_array_builder.append(",");
      }
    }
    return SQL_SELECT_NN_OF_OBJECT_BEGIN
        + in_array_builder.toString()
        + SQL_SELECT_NN_OF_OBJECT_END;
  }

  private static void set_param_of_statement(
      PreparedStatement prepared_statement, int o_id, int num_res, int distance, String[] o_types)
      throws SQLException {
    prepared_statement.setInt(1, o_id);
    prepared_statement.setString(2, "sdo_num_res=" + num_res + " distance=" + distance);
    for (int i = 0; i < o_types.length; i++) {
      prepared_statement.setString(i + 3, o_types[i]);
    }
  }

  public static double[] get_nearest_neighbours_of_object(
      Connection conn, int o_id, int num_res, int distance, String[] o_types) throws SQLException {
    String sql_select_nn_of_object = build_query(o_types);
    try (PreparedStatement prepared_statement = conn.prepareStatement(sql_select_nn_of_object)) {
      set_param_of_statement(prepared_statement, o_id, num_res, distance, o_types);
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
}
