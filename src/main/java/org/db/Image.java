package org.db;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class Image {
    private int image_id;
    private static final String SQL_SELECT_IMAGE_FOR_UPDATE = "SELECT image FROM Images WHERE image_id = ? FOR UPDATE ";
    private static final String SQL_INSERT_NEW_IMAGE =
            "INSERT INTO Images (image_id, image) VALUES (?, ordsys.ordimage.init())";
    private static final String SQL_UPDATE_IMAGE = "UPDATE Images SET image = ? WHERE image_id = ?";
    private static final String SQL_UPDATE_STILLIMAGE =
            "UPDATE Images i SET i.image_si = SI_StillImage(i.image.getContent()) WHERE i.image_id = ?";
    private static final String SQL_UPDATE_STILLIMAGE_META =
            "UPDATE Images SET " +
                    "image_ac = SI_AverageColor(image_si), " +
                    "image_ch = SI_ColorHistogram(image_si), " +
                    "image_pc = SI_PositionalColor(image_si), " +
                    "image_tx = SI_Texture(image_si) " +
            "WHERE image_id = ?";

    /**
     * Construct a new Image of the provided ID.
     *
     * @param image_id unique ID of image
     */
    Image(int image_id) {
        this.image_id = image_id;
    }

    void save_image_from_file_to_db(
            Connection conn, String filename) throws SQLException, NotFoundException, IOException
    {
        final boolean previous_auto_commit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            OrdImage ord_image;
            try {
                ord_image = select_ord_image_for_update(conn);
            } catch (SQLException | NotFoundException e) {
                try (PreparedStatement insert_prepared_statement = conn.prepareStatement(SQL_INSERT_NEW_IMAGE)) {
                    insert_prepared_statement.setInt(1, image_id);
                    insert_prepared_statement.executeUpdate();
                }
                ord_image = select_ord_image_for_update(conn);
            }
            ord_image.loadDataFromFile(filename);
            ord_image.setProperties();
            try (PreparedStatement update_prepared_statement = conn.prepareStatement(SQL_UPDATE_IMAGE)) {
                final OraclePreparedStatement oracle_prepared_statement =
                        (OraclePreparedStatement) update_prepared_statement;
                oracle_prepared_statement.setORAData(1, ord_image);
                update_prepared_statement.setInt(2, image_id);
                update_prepared_statement.executeUpdate();
            }
            recreate_still_image_data(conn);
        } finally {
            conn.setAutoCommit(previous_auto_commit);
        }
    }

    private OrdImage select_ord_image_for_update(Connection conn) throws NotFoundException, SQLException {
        try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_SELECT_IMAGE_FOR_UPDATE)) {
            prepared_statement.setInt(1, image_id);
            try (ResultSet result_set = prepared_statement.executeQuery()) {
                if (result_set.next()) {
                    final OracleResultSet oracle_result_set = (OracleResultSet) result_set;
                    return (OrdImage) oracle_result_set.getORAData(1, OrdImage.getORADataFactory());
                } else {
                    throw new NotFoundException();
                }
            }
        }
    }

    private void recreate_still_image_data(Connection conn) throws  SQLException {
        try (PreparedStatement si_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE)) {
            si_prepared_statement.setInt(1, image_id);
            si_prepared_statement.executeUpdate();
        }
        try (PreparedStatement si_meta_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE_META)) {
            si_meta_prepared_statement.setInt(1, image_id);
            si_meta_prepared_statement.executeUpdate();
        }
    }

    static class NotFoundException extends Exception {
        // nothing to extend
    }
}