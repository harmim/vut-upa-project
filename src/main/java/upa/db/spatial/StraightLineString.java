package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;

public class StraightLineString extends Collection {
  // line-string
  private static final int SDO_ETYPE = 2;
  // set the line-string's vertices as connected by straight line segments
  private static final int SDO_INTERPRETATION = 1;
  private static final int POINT_ELEM_SIZE = 2;

  private static JGeometry create_geometry(double[] points) {
    return new JGeometry(
        JGeometry.GTYPE_CURVE,
        SpatialObject.SDO_SRID,
        new int[] {SpatialObject.SDO_STARTING_OFFSET, SDO_ETYPE, SDO_INTERPRETATION},
        points);
  }

  public static void update_geometry_of_line_string(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(points));
    conn.close();
  }

  public static int insert_new_line_string(Connection conn, String o_name, String o_type, double[] points)
      throws Exception {
    int o_id = insert_new_object(conn, o_name, o_type, create_geometry(points));
    conn.close();
    return o_id;
  }

  public static void add_points_to_line_string(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_line_string(conn, o_id, add_points_to_collection(conn, o_id, points));
    conn.close();
  }

  public static void delete_points_from_line_string(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_line_string(conn, o_id, delete_points_from_collection(conn, o_id, points));
    conn.close();
  }
}
