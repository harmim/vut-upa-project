package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.*;
import java.util.Arrays;

public class Collection extends SpatialObject {
  private static final int ELEMENT_INFO_LENGTH = 3;
  private static final String SQL_SELECT_GEOMETRY_FOR_UPDATE =
      "SELECT geometry FROM Village WHERE o_id = ?";
  private static final int POINT_ELEM_SIZE = 2;

  public static void delete_object_from_collection(
      Connection conn, int o_id, int[] o_idxs, int o_length) throws Exception {
    Arrays.sort(o_idxs);
    int removed_objects = 0;
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    int[] elem_info = geometry.getElemInfo();
    double[] ord_array = geometry.getOrdinatesArray();
    for (int object_idx : o_idxs) {
      elem_info = delete_elements_from_elem_info(elem_info, object_idx - removed_objects, o_length);
      ord_array = delete_elements_from_array(ord_array, object_idx - removed_objects, o_length);
      removed_objects += 1;
    }
    JGeometry new_geometry =
        new JGeometry(geometry.getType(), geometry.getSRID(), elem_info, ord_array);
    update_geometry_of_object(conn, o_id, new_geometry);
    conn.close();
  }

  private static int[] delete_elements_from_elem_info(int[] elem_info, int o_idxs, int o_length) {
    for (int i = 0; i < ELEMENT_INFO_LENGTH; i++) {
      elem_info = ArrayUtils.remove(elem_info, o_idxs * ELEMENT_INFO_LENGTH);
    }
    for (int i = o_idxs * ELEMENT_INFO_LENGTH; i < elem_info.length; i += ELEMENT_INFO_LENGTH) {
      elem_info[i] -= o_length;
    }
    return elem_info;
  }

  private static double[] delete_elements_from_array(double[] elem_info, int o_idxs, int o_length) {
    for (int i = 0; i < o_length; i++) {
      elem_info = ArrayUtils.remove(elem_info, o_idxs * o_length);
    }
    return elem_info;
  }

  protected static JGeometry select_geometry_for_update(Connection conn, int o_id)
      throws SQLException, NotFoundException {
    try (PreparedStatement prepare_statement =
        conn.prepareStatement(SQL_SELECT_GEOMETRY_FOR_UPDATE)) {
      prepare_statement.setInt(1, o_id);
      try (ResultSet result_set = prepare_statement.executeQuery()) {
        if (result_set.next()) {
          Struct obj = (Struct) result_set.getObject(1);
          return JGeometry.loadJS(obj);
        } else {
          throw new NotFoundException();
        }
      }
    }
  }

  public static double[] add_points_to_collection(Connection conn, int o_id, double[] points)
      throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    double[] sdo_ord_array = geometry.getOrdinatesArray();
    for (int i = 0; i < points.length; i += POINT_ELEM_SIZE) {
      sdo_ord_array = ArrayUtils.addAll(sdo_ord_array, points[i], points[i + 1]);
    }
    return sdo_ord_array;
  }

  public static double[] delete_points_from_collection(Connection conn, int o_id, double[] points)
      throws Exception {
    JGeometry geometry = select_geometry_for_update(conn, o_id);
    double[] sdo_ord_array = geometry.getOrdinatesArray();
    int[] sdo_elem_info = geometry.getElemInfo();
    for (int i = 0; i < points.length; i += POINT_ELEM_SIZE) {
      for (int j = 0; j < sdo_ord_array.length; j += POINT_ELEM_SIZE) {
        if (sdo_ord_array[j] == points[i] && sdo_ord_array[j + 1] == points[i + 1]) {
          sdo_ord_array = ArrayUtils.removeAll(sdo_ord_array, j, j + 1);
          break;
        }
      }
    }
    return sdo_ord_array;
  }
}
