package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;
import org.apache.commons.lang3.ArrayUtils;

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

  public static void update_geometry_in_db(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(points));
  }

  public static int insert_new_to_db(Connection conn, String o_name, String o_type, double[] points)
      throws Exception {
    return insert_new_object_to_db(conn, o_name, o_type, create_geometry(points));
  }

  public static void add_points_to_line_string(Connection conn, int o_id, double[] points)
      throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    double[] sdo_ord_array = geometry.getOrdinatesArray();
    for (int i = 0; i < points.length; ) {
      sdo_ord_array = ArrayUtils.addAll(sdo_ord_array, points[i], points[i + 1]);
      i += POINT_ELEM_SIZE;
    }
    update_geometry_in_db(conn, o_id, sdo_ord_array);
  }

  public static void delete_points_from_line_string(Connection conn, int o_id, double[] points)
      throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    double[] sdo_ord_array = geometry.getOrdinatesArray();
    for (int i = 0; i < points.length; ) {
      for (int j = 0; j < sdo_ord_array.length; ) {
        if (sdo_ord_array[j] == points[i] && sdo_ord_array[j + 1] == points[i + 1]) {
          sdo_ord_array = ArrayUtils.removeAll(sdo_ord_array, j, j + 1);
          break;
        }
        j += POINT_ELEM_SIZE;
      }
      i += POINT_ELEM_SIZE;
    }
    update_geometry_in_db(conn, o_id, sdo_ord_array);
  }
}
