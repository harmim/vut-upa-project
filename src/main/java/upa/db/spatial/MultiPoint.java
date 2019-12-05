package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MultiPoint extends Collection {
  // point
  private static final int SDO_ETYPE = 1;
  // common point
  protected static final int SDO_INTERPRETATION = 1;

  private static JGeometry create_geometry(double[] sdo_points_arr) {
    return new JGeometry(
        JGeometry.GTYPE_MULTIPOINT,
        SpatialObject.SDO_SRID,
        create_sdo_elem_info_arr(sdo_points_arr.length / 2),
        sdo_points_arr);
  }

  private static int[] create_sdo_elem_info_arr(int num_of_pts) {
    ArrayList<Integer> sdo_elem_info = new ArrayList<>();
    for (int i = 1; i <= num_of_pts; i++) {
      sdo_elem_info.addAll(List.of(i * 2 - 1, SDO_ETYPE, SDO_INTERPRETATION));
    }
    return ArrayUtils.toPrimitive(sdo_elem_info.toArray(new Integer[0]));
  }

  public static int insert_new_multipoint_to_db(
      Connection conn, String o_name, String o_type, double[] points) throws Exception {
    return insert_new_object_to_db(conn, o_name, o_type, create_geometry(points));
  }

  private static void update_geometry_of_multipoint(Connection conn, int o_id, double[] points)
      throws Exception {
    update_geometry_of_object(conn, o_id, create_geometry(points));
  }

  public static void add_point_to_multipoint(Connection conn, int o_id, double[][] new_points)
      throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    ArrayList<Double> sdo_points =
        (ArrayList<Double>)
            Arrays.stream(geometry.getOrdinatesArray()).boxed().collect(Collectors.toList());
    for (double[] new_point : new_points) {
      sdo_points.addAll(List.of(new_point[0], new_point[1]));
    }
    update_geometry_of_multipoint(
        conn, o_id, ArrayUtils.toPrimitive(sdo_points.toArray(new Double[0])));
  }
}
