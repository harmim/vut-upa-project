package upa.db.spatial;

import oracle.spatial.geometry.JGeometry;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CircleCollection extends SpatialObject {
    // geometry is a heterogeneous collection of elements
    private static final int SDO_GTYPE = 1003;
    // circle type described by three distinct non-colinear points, all on the circumference of the circle
    private static final int SDO_INTERPRETATION = 4;
    private static ArrayList<Double> sdo_points;
    private static ArrayList<Integer> sdo_elem_info;
    private static final String SQL_SELECT_GEOMETRY_FOR_UPDATE = "SELECT geometry FROM Village WHERE o_id = ?";


    public CircleCollection() {
        sdo_points = new ArrayList<>();
        sdo_elem_info = new ArrayList<>();
    }

    public void fulfill_sdo_points(double x_start, double y_start, double r) {
        sdo_points.add(x_start + r / 2);
        sdo_points.add(y_start);
        sdo_points.add(x_start + r);
        sdo_points.add(y_start + r / 2);
        sdo_points.add(x_start + r / 2);
        sdo_points.add(y_start + r);
    }

    public void compute_coordinates_of_disjoint_circles(
            double r, int n, double x_start, double y_start, boolean horizontal) {
        int current_offset = 1;
        int num_of_points_in_level = 3;
        for (int i = 0; i < n; i++) {
            fulfill_sdo_points(x_start, y_start, r);
            sdo_elem_info.addAll(List.of(current_offset, SDO_GTYPE, SDO_INTERPRETATION));
            current_offset += num_of_points_in_level * 2;
            if (horizontal) {
                x_start += r + r / 2;
            } else {
                y_start += r + r / 2;
            }
        }
    }

    public void compute_coordinates_of_various_circles(double[][] circle_info) {
        int current_offset = 1;
        int num_of_points_in_level = 3;
        for (double[] circle : circle_info) {
            fulfill_sdo_points(circle[0], circle[1], circle[2]);
            sdo_elem_info.addAll(List.of(current_offset, SDO_GTYPE, SDO_INTERPRETATION));
            current_offset += num_of_points_in_level * 2;
        }
    }

    private static JGeometry create_geometry() {
        double[] sdo_points_arr = ArrayUtils.toPrimitive(sdo_points.toArray(new Double[0]));
        int[] sdo_elem_info_arr = ArrayUtils.toPrimitive(sdo_elem_info.toArray(new Integer[0]));
        return new JGeometry(
                JGeometry.GTYPE_COLLECTION, SpatialObject.SDO_SRID, sdo_elem_info_arr, sdo_points_arr
        );
    }

    public int insert_new_to_db(Connection conn, String o_name, String o_type) throws Exception {
        return insert_new_object_to_db(conn, o_name, o_type, create_geometry());
    }

    public static void delete_circle_from_collection(
            Connection conn, int o_id, int[] image_ids) throws Exception {
        Arrays.sort(image_ids);
        int removed_images = 0;
        JGeometry geometry = select_geometry_for_update(conn, o_id);
        int[] elem_info = geometry.getElemInfo();
        double[] ord_array = geometry.getOrdinatesArray();
        for (int image_id : image_ids) {
            elem_info = delete_elements_from_array(elem_info, image_id - removed_images);
            ord_array = delete_elements_from_array(ord_array, image_id - removed_images);
            removed_images += 1;
        }
        JGeometry new_geometry = new JGeometry(geometry.getType(), geometry.getSRID(), elem_info, ord_array);
        update_geometry_of_object(conn, o_id, new_geometry);
    }

    private static int[] delete_elements_from_array(int[] elem_info, int image_id) {
        for (int i = 0; i < 3; i++) {
            elem_info = ArrayUtils.remove(elem_info, image_id * 3);
        }
        int i = image_id * 3;
        while (i < elem_info.length) {
            elem_info[i] -= 6;
            i += 3;
        }
        return elem_info;
    }

    private static double[] delete_elements_from_array(double[] elem_info, int image_id) {
        for (int i = 0; i < 6; i++) {
            elem_info = ArrayUtils.remove(elem_info, image_id * 6);
        }
        return elem_info;
    }

    private void update_geometry_of_collection(Connection conn, int o_id) throws Exception {
        update_geometry_of_object(conn, o_id, create_geometry());
    }

    public static void update_geometry_of_collection(
            Connection conn, int o_id, double r, double x_start, double y_start) throws Exception {
        JGeometry geometry = select_geometry_for_update(conn, o_id);
        CircleCollection circles = new CircleCollection();
        boolean horizontal = get_distribution_of_collection(geometry.getOrdinatesArray());
        circles.compute_coordinates_of_disjoint_circles(r, geometry.getElemInfo().length / 3, x_start, y_start, horizontal);
        circles.update_geometry_of_collection(conn, o_id);
    }

    private static boolean get_distribution_of_collection(double[] ord_array) {
        return ord_array[0] != ord_array[6];
    }

    private static JGeometry select_geometry_for_update(
            Connection conn, int o_id) throws SQLException, NotFoundException {
        try (PreparedStatement prepare_statement = conn.prepareStatement(SQL_SELECT_GEOMETRY_FOR_UPDATE)) {
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
}
