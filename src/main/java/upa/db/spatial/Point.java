package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;

public class Point extends SpatialObject {

  private static JGeometry create_geometry(double[] coordinates) {
    return JGeometry.createPoint(coordinates, 2, SpatialObject.SDO_SRID);
  }

  public static void update_geometry_of_point(Connection conn, int o_id, double[] coordinates)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(coordinates));
    conn.close();
  }

  public static int insert_new_point(
      Connection conn, String o_name, String o_type, double[] coordinates) throws Exception {
    int o_id = insert_new_object(conn, o_name, o_type, create_geometry(coordinates));
    conn.close();
    return o_id;
  }
}
