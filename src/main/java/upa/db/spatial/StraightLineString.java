package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;

import java.sql.Connection;

public class StraightLineString extends SpatialObject {
  // line-string
  private static final int SDO_ETYPE = 2;
  // set the line-string's vertices as connected by straight line segments
  private static final int SDO_INTERPRETATION = 1;

  private static JGeometry create_geometry(double[] points) {
    return new JGeometry(
        JGeometry.GTYPE_CURVE,
        SpatialObject.SDO_SRID,
        new int[] {SpatialObject.SDO_STARTING_OFFSET, SDO_ETYPE, SDO_INTERPRETATION},
        points);
  }

  public static void update_geometry_in_db(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(points));
  }

  public static int insert_new_to_db(Connection conn, String o_name, String o_type, double[] points)
      throws Exception {
    return insert_new_object_to_db(conn, o_name, o_type, create_geometry(points));
  }
}
