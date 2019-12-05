package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;

public class Rectangle extends SpatialObject {
  // exterior polygon ring
  private static final int SDO_ETYPE = 1003;
  // rectangle given by given by its lower-left corner and the upper-right corner
  protected static final int SDO_INTERPRETATION = 3;

  private static JGeometry create_geometry(double[] points) {
    return new JGeometry(
        JGeometry.GTYPE_POLYGON,
        SpatialObject.SDO_SRID,
        new int[] {SpatialObject.SDO_STARTING_OFFSET, SDO_ETYPE, SDO_INTERPRETATION},
        points);
  }

  public static void update_geometry_of_rectangle(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(points));
  }

  public static int insert_new_rectangle(Connection conn, String o_name, String o_type, double[] points)
      throws Exception {
    return insert_new_object(conn, o_name, o_type, create_geometry(points));
  }
};
