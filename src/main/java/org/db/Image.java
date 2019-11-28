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
            Connection conn, Integer image_id, String filename) throws SQLException, NotFoundException, IOException
    {
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

    private static void save_ord_image_to_db(Connection conn, Integer image_id, OrdImage ord_image) throws SQLException {
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

    private static int get_last_image_id(Connection conn) throws SQLException, NotFoundException{
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
            Connection conn, Integer image_id) throws NotFoundException, SQLException
    {
        return getOrdImage(conn, image_id, SQL_SELECT_IMAGE_FOR_UPDATE);
    }

    private static void recreate_still_image_data(Connection conn, Integer image_id) throws  SQLException {
        try (PreparedStatement si_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE)) {
            si_prepared_statement.setInt(1, image_id);
            si_prepared_statement.executeUpdate();
        }
        try (PreparedStatement si_meta_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE_META)) {
            si_meta_prepared_statement.setInt(1, image_id);
            si_meta_prepared_statement.executeUpdate();
        }
    }

    static void delete_image_from_db(Connection conn, Integer image_id) throws SQLException {
        try (PreparedStatement delete_prepared_statement = conn.prepareStatement(SQL_DELETE_IMAGE)) {
            delete_prepared_statement.setInt(1, image_id);
            delete_prepared_statement.executeUpdate();
        }
    }

    static OrdImage load_image_from_db(
            Connection conn, Integer image_id) throws SQLException, NotFoundException
    {
        return getOrdImage(conn, image_id, SQL_SELECT_IMAGE);
    }

    private static OrdImage getOrdImage(
            Connection conn, Integer image_id, String sqlSelectImage
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
    
    static class NotFoundException extends Exception {
        // nothing to extend
    }
}