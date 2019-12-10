package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;

public class Circle extends SpatialObject {
  // exterior polygon ring
  private static final int SDO_ETYPE = 1003;
  // circle type described by three distinct non-colinear points, all on the circumference of the
  protected static final int SDO_INTERPRETATION = 4;

  public static double[] get_sdo_points(double x_start, double y_start, double r) {
    return new double[] {
      x_start + r / 2, y_start,
      x_start + r, y_start + r / 2,
      x_start + r / 2, y_start + r
    };
  }

  private static JGeometry create_geometry(double[] circle_data) {
    return new JGeometry(
        JGeometry.GTYPE_POLYGON,
        SpatialObject.SDO_SRID,
        new int[] {SpatialObject.SDO_STARTING_OFFSET, SDO_ETYPE, SDO_INTERPRETATION},
        get_sdo_points(circle_data[0], circle_data[1], circle_data[2]));
  }

  public static void update_geometry_of_circle(Connection conn, int o_id, double[] circle_data)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(circle_data));
    conn.close();
  }

  public static int insert_new_circle(
      Connection conn, String o_name, String o_type, double[] circle_data) throws Exception {
    int o_id = insert_new_object(conn, o_name, o_type, create_geometry(circle_data));
    conn.close();
    return o_id;
  }
}
