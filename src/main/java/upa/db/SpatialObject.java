package upa.db;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;

class Rectangle extends SpatialObject {
    // the geometry is in Cartesian (local) coordinates
    private static final int SDO_SRID = 0;
    // exterior polygon ring
    private static final int SDO_ETYPE = 1003;
    // the offset within the SDO_ORDINATES array where the first ordinate for this element is stored
    private static final int SDO_STARTING_OFFSET = 1;
    // rectangle given by given by its lower-left corner and the upper-right corner
    private static final int SDO_INTERPRETATION = 3;

    private static JGeometry create_geometry( double[] points) {
        return new JGeometry(
                JGeometry.GTYPE_POLYGON, SDO_SRID, new int[]{SDO_STARTING_OFFSET, SDO_ETYPE, SDO_INTERPRETATION}, points
        );
    }

    public static void update_geometry_in_db(Connection conn, int o_id, double[] points) throws Exception {
        update_geometry_of_object(conn, o_id, create_geometry(points));
    }

    public static int insert_new_to_db(Connection conn, String o_name, String o_type, double[] points) throws Exception {
        return insert_new_object_to_db(conn, o_name, o_type, create_geometry(points));
    }
}

public class SpatialObject extends GeneralDB {
    private static final String SQL_UPDATE_GEOMETRY_OF_OBJECT = "UPDATE Village set geometry = ? WHERE o_id = ?";
    private static final String SQL_INSERT_NEW_OBJECT =
            "INSERT INTO Village (o_name, o_type, geometry) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_LAST_OBJECT_ID = "SELECT MAX(o_id) FROM Village";
    private static final String SQL_DELETE_OBJECT = "DELETE FROM Village WHERE o_id = ?";

    protected static void update_geometry_of_object(
            Connection conn, int o_id, JGeometry j_geom) throws Exception
    {
        try (PreparedStatement prepare_statement = conn.prepareStatement(SQL_UPDATE_GEOMETRY_OF_OBJECT)) {
            Struct obj = JGeometry.storeJS(conn, j_geom);
            prepare_statement.setObject(1, obj);
            prepare_statement.setInt(2, o_id);
            prepare_statement.executeUpdate();
        }
    }

    protected static int insert_new_object_to_db(
            Connection conn, String o_name, String o_type, JGeometry j_geom) throws Exception
    {
        try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_INSERT_NEW_OBJECT)) {
            Struct obj = JGeometry.storeJS(conn, j_geom);
            prepared_statement.setString(1, o_name);
            prepared_statement.setString(2, o_type);
            prepared_statement.setObject(3, obj);
            prepared_statement.executeUpdate();
        }
        return get_last_inserted_id(conn, SQL_SELECT_LAST_OBJECT_ID);
    }

    static void delete_spatial_object_from_db(Connection conn, int o_id) throws SQLException {
        delete_object_from_db(conn, SQL_DELETE_OBJECT, o_id);
    }
}
