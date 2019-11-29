package org.db;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;

import java.io.IOException;
import java.sql.*;

class Image {
    private static final String SQL_SELECT_LAST_IMAGE_ID =
            "SELECT MAX(image_id) FROM Images";
    private static final String SQL_SELECT_IMAGE =
            "SELECT image FROM Images WHERE image_id = ?";
    private static final String SQL_SELECT_IMAGE_FOR_UPDATE =
            "SELECT image FROM Images WHERE image_id = ? FOR UPDATE";
    private static final String SQL_INSERT_NEW_IMAGE =
            "INSERT INTO Images (image) VALUES (ordsys.ordimage.init())";
    private static final String
            SQL_UPDATE_IMAGE = "UPDATE Images SET image = ? WHERE image_id = ?";
    private static final String SQL_UPDATE_STILLIMAGE =
            "UPDATE Images i SET i.image_si = SI_StillImage(i.image.getContent()) WHERE i.image_id = ?";
    private static final String SQL_UPDATE_STILLIMAGE_META =
            "UPDATE Images SET " +
                    "image_ac = SI_AverageColor(image_si), " +
                    "image_ch = SI_ColorHistogram(image_si), " +
                    "image_pc = SI_PositionalColor(image_si), " +
                    "image_tx = SI_Texture(image_si) " +
                    "WHERE image_id = ?";
    private static final String SQL_DELETE_IMAGE =
            "DELETE FROM Images WHERE image_id = ?";

    static int save_image_from_file_to_db(
            Connection conn, int image_id, String filename) throws SQLException, NotFoundException, IOException {
        final boolean previous_auto_commit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            OrdImage ord_image;
            if (image_id == 0) { // new image
                try (PreparedStatement insert_prepared_statement = conn.prepareStatement(SQL_INSERT_NEW_IMAGE)) {
                    insert_prepared_statement.executeUpdate();
                    image_id = get_last_image_id(conn);
                }
            }
            ord_image = select_ord_image_for_update(conn, image_id);
            ord_image.loadDataFromFile(filename);
            save_ord_image_to_db(conn, image_id, ord_image);
        } finally {
            conn.setAutoCommit(previous_auto_commit);
        }
        return image_id;
    }

    private static void save_ord_image_to_db(Connection conn, int image_id, OrdImage ord_image) throws SQLException {
        ord_image.setProperties();
        try (PreparedStatement update_prepared_statement = conn.prepareStatement(SQL_UPDATE_IMAGE)) {
            final OraclePreparedStatement oracle_prepared_statement =
                    (OraclePreparedStatement) update_prepared_statement;
            oracle_prepared_statement.setORAData(1, ord_image);
            update_prepared_statement.setInt(2, image_id);
            update_prepared_statement.executeUpdate();
        }
        recreate_still_image_data(conn, image_id);
    }

    private static int get_last_image_id(Connection conn) throws SQLException, NotFoundException {
        Statement statement = conn.createStatement();
        try (ResultSet result_set = statement.executeQuery(SQL_SELECT_LAST_IMAGE_ID)) {
            if (result_set.next()) {
                final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
                return oracle_result_set.getInt(1);
            } else {
                throw new NotFoundException();
            }
        }
    }

    private static OrdImage select_ord_image_for_update(
            Connection conn, int image_id) throws NotFoundException, SQLException {
        return getOrdImage(conn, image_id, SQL_SELECT_IMAGE_FOR_UPDATE);
    }

    private static void recreate_still_image_data(Connection conn, int image_id) throws SQLException {
        try (PreparedStatement si_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE)) {
            si_prepared_statement.setInt(1, image_id);
            si_prepared_statement.executeUpdate();
        }
        try (PreparedStatement si_meta_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE_META)) {
            si_meta_prepared_statement.setInt(1, image_id);
            si_meta_prepared_statement.executeUpdate();
        }
    }

    static void delete_image_from_db(Connection conn, int image_id) throws SQLException {
        try (PreparedStatement delete_prepared_statement = conn.prepareStatement(SQL_DELETE_IMAGE)) {
            delete_prepared_statement.setInt(1, image_id);
            delete_prepared_statement.executeUpdate();
        }
    }

    static OrdImage load_image_from_db(
            Connection conn, int image_id) throws SQLException, NotFoundException {
        return getOrdImage(conn, image_id, SQL_SELECT_IMAGE);
    }

    private static OrdImage getOrdImage(
            Connection conn, int image_id, String sqlSelectImage
    ) throws SQLException, NotFoundException {
        try (PreparedStatement load_prepared_statement = conn.prepareStatement(sqlSelectImage)) {
            load_prepared_statement.setInt(1, image_id);
            try (ResultSet result_set = load_prepared_statement.executeQuery()) {
                if (result_set.next()) {
                    final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
                    return (OrdImage) oracle_result_set.getORAData(1, OrdImage.getORADataFactory());
                } else {
                    throw new NotFoundException();
                }
            }
        }
    }

    static void process_image_in_db(
            Connection conn, int image_id, String op_code, double param1, double param2, double param3, double param4)
            throws NotFoundException, SQLException
    {
        final boolean previous_auto_commit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            OrdImage ord_image = select_ord_image_for_update(conn, image_id);
            execute_processing_of_image(ord_image, op_code, param1, param2, param3, param4);
            save_ord_image_to_db(conn, image_id, ord_image);
        } finally {
            conn.setAutoCommit(previous_auto_commit);
        }
    }

    static void execute_processing_of_image(
            OrdImage ord_image, String op_code, double param1,double param2, double param3, double param4)
            throws NotFoundException, SQLException
    {
        switch (op_code) {
            case "rotate":
                // param1 - angle (optimal values are 90, 180 and 270)
                ord_image.process("rotate=" + (int) param1);
                break;
            case "mirror":
                ord_image.process("mirror");
                break;
            case "cut":
                // param1 -> x, param2 -> y, param3 -> width, param4 -> height
                try {
                    ord_image.process(
                            "cut=" + (int) param1 + " " + (int) param2 + " " + (int) param3 + " " + (int) param4
                    );
                } catch (SQLException ex) {
                    System.err.println("!!! Wrong parameters to cut image. !!!");
                }
                break;
            case "fixedScale":
            case "maxScale":
                // param1 -> xScale, param2 -> yScale
                ord_image.process(op_code + "=" + (int) param1 + " " + (int) param2);
                break;
            case "scale":
                // param1 -> scale value
                ord_image.process(op_code + "=" + param1);
                break;
            case "monochrome":
                ord_image.process("contentFormat=4bitgray");
                break;
            default:
                System.err.println("!!! Unknown trasformations string !!!");
        }
    }

    static class NotFoundException extends Exception {
        // nothing to extend
    }
}