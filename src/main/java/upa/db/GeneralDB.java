package upa.db;

import oracle.jdbc.OracleResultSet;

import java.sql.*;

public class GeneralDB {

    static int get_last_inserted_id(
            Connection conn, String sql_select) throws SQLException, Image.NotFoundException
    {
        Statement statement = conn.createStatement();
        try (ResultSet result_set = statement.executeQuery(sql_select)) {
            if (result_set.next()) {
                final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
                return oracle_result_set.getInt(1);
            } else {
                throw new Image.NotFoundException();
            }
        }
    }

    static void delete_object_from_db(Connection conn, String sql_delete_object, int obj_id) throws SQLException {
        try (PreparedStatement delete_prepared_statement = conn.prepareStatement(sql_delete_object)) {
            delete_prepared_statement.setInt(1, obj_id);
            delete_prepared_statement.executeUpdate();
        }
    }
}
