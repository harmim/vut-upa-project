package upa.db.spatial;

import oracle.jdbc.OracleResultSet;
import oracle.spatial.geometry.JGeometry;
import upa.db.GeneralDB;
import upa.db.multimedia.DBImage;

import javax.print.attribute.standard.NumberOfDocuments;
import java.sql.*;

public class SpatialObject extends GeneralDB {
  // the geometry is in Cartesian (local) coordinates
  protected static final int SDO_SRID = 0;
  // the offset within the SDO_ORDINATES array where the first ordinate for this element is stored
  protected static final int SDO_STARTING_OFFSET = 1;

  private static final String SQL_UPDATE_GEOMETRY_OF_OBJECT =
      "UPDATE Village set geometry = ? WHERE o_id = ?";
  private static final String SQL_INSERT_NEW_OBJECT =
      "INSERT INTO Village (o_name, o_type, geometry) VALUES (?, ?, ?)";
  private static final String SQL_SELECT_LAST_OBJECT_ID = "SELECT MAX(o_id) FROM Village";
  private static final String SQL_DELETE_OBJECT = "DELETE FROM Village WHERE o_id = ?";
  private static final String SQL_UPDATE_IMAGE_ID = "UPDATE Village SET image_id = ? WHERE o_id = ?";
  private static final String SQL_SELECT_IMAGE_ID_BY_OBJECT_ID = "SELECT image_id FROM Village WHERE o_id = ?";
  private static final String SQL_SELECT_OBJECT_ID_BY_IMAGE_ID = "SELECT o_id FROM Village WHERE image_id = ?";

  protected static void update_geometry_of_object(Connection conn, int o_id, JGeometry j_geom)
      throws Exception {
    try (PreparedStatement prepare_statement =
        conn.prepareStatement(SQL_UPDATE_GEOMETRY_OF_OBJECT)) {
      Struct obj = JGeometry.storeJS(conn, j_geom);
      prepare_statement.setObject(1, obj);
      prepare_statement.setInt(2, o_id);
      prepare_statement.executeUpdate();
    }
  }

  protected static int insert_new_object(
      Connection conn, String o_name, String o_type, JGeometry j_geom) throws Exception {
    try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_INSERT_NEW_OBJECT)) {
      Struct obj = JGeometry.storeJS(conn, j_geom);
      prepared_statement.setString(1, o_name);
      prepared_statement.setString(2, o_type);
      prepared_statement.setObject(3, obj);
      prepared_statement.executeUpdate();
    }
    return get_last_inserted_id(conn, SQL_SELECT_LAST_OBJECT_ID);
  }

  public static void update_image_id_of_object(Connection conn, int o_id, int image_id) throws SQLException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_UPDATE_IMAGE_ID)) {
      prepared_statement.setInt(1, image_id);
      prepared_statement.setInt(2, o_id);
      prepared_statement.executeUpdate();
    }
  }

  public static int select_image_id_by_object(Connection conn, int o_id) throws SQLException, NotFoundException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_SELECT_IMAGE_ID_BY_OBJECT_ID)) {
      prepared_statement.setInt(1, o_id);
      try (ResultSet result_set = prepared_statement.executeQuery()) {
        if (result_set.next()) {
          final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
          return oracle_result_set.getInt(1);
        } else {
          throw new NotFoundException();
        }
      }
    }
  }

  public static int select_object_id_by_image(Connection conn, int image_id) throws SQLException, NotFoundException {
    try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_SELECT_OBJECT_ID_BY_IMAGE_ID)) {
      prepared_statement.setInt(1, image_id);
      try (ResultSet result_set = prepared_statement.executeQuery()) {
        if (result_set.next()) {
          final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
          return oracle_result_set.getInt(1);
        } else {
          throw new NotFoundException();
        }
      }
    }
  }

  public static void delete_object(Connection conn, int o_id) throws SQLException, NotFoundException {
    DBImage.delete_image(conn, select_image_id_by_object(conn, o_id));
    delete_object(conn, SQL_DELETE_OBJECT, o_id);
    conn.close();
  }
}
