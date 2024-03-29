package upa.db.multimedia;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;
import upa.db.GeneralDB;
import upa.db.spatial.SpatialObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBImage extends GeneralDB {
  private static final String SQL_SELECT_LAST_IMAGE_ID = "SELECT MAX(image_id) FROM Images";
  private static final String SQL_SELECT_IMAGE = "SELECT image FROM Images WHERE image_id = ?";
  private static final String SQL_SELECT_IMAGE_FOR_UPDATE =
      "SELECT image FROM Images WHERE image_id = ? FOR UPDATE";
  private static final String SQL_INSERT_NEW_IMAGE =
      "INSERT INTO Images (image) VALUES (ordsys.ordimage.init())";
  private static final String SQL_UPDATE_IMAGE = "UPDATE Images SET image = ? WHERE image_id = ?";
  private static final String SQL_UPDATE_STILLIMAGE =
      "UPDATE Images i SET i.image_si = SI_StillImage(i.image.getContent()) WHERE i.image_id = ?";
  private static final String SQL_UPDATE_STILLIMAGE_META =
      "UPDATE Images SET "
          + "image_ac = SI_AverageColor(image_si), "
          + "image_ch = SI_ColorHistogram(image_si), "
          + "image_pc = SI_PositionalColor(image_si), "
          + "image_tx = SI_Texture(image_si) "
          + "WHERE image_id = ?";
  private static final String SQL_DELETE_IMAGE = "DELETE FROM Images WHERE image_id = ?";
  private static final String SQL_SELECT_SIMILAR_IMAGE =
      "SELECT dst.image_id, SI_ScoreByFtrList(new SI_FeatureList("
          + "src.image_ac,?,src.image_ch,?,src.image_pc,?,src.image_tx,?),dst.image_si) AS similarity "
          + "FROM Images src, Images dst "
          + "WHERE (src.image_id = ?) AND (src.image_id <> dst.image_id) "
          + "ORDER BY similarity ASC";

  public static int save_image_from_file(Connection conn, int image_id, String filename)
      throws SQLException, NotFoundException, IOException {
    final boolean previous_auto_commit = conn.getAutoCommit();
    conn.setAutoCommit(false);
    try {
      OrdImage ord_image;
      if (image_id == 0) { // new image
        try (PreparedStatement insert_prepared_statement =
            conn.prepareStatement(SQL_INSERT_NEW_IMAGE)) {
          insert_prepared_statement.executeUpdate();
          image_id = get_last_inserted_id(conn, SQL_SELECT_LAST_IMAGE_ID);
        }
      }
      ord_image = select_ord_image_for_update(conn, image_id);
      ord_image.loadDataFromFile(filename);
      save_ord_image(conn, image_id, ord_image);
    } finally {
      conn.setAutoCommit(previous_auto_commit);
      conn.close();
    }
    return image_id;
  }

  private static void save_ord_image(Connection conn, int image_id, OrdImage ord_image)
      throws SQLException {
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

  private static OrdImage select_ord_image_for_update(Connection conn, int image_id)
      throws NotFoundException, SQLException {
    return get_ord_image(conn, image_id, SQL_SELECT_IMAGE_FOR_UPDATE);
  }

  private static void recreate_still_image_data(Connection conn, int image_id) throws SQLException {
    try (PreparedStatement si_prepared_statement = conn.prepareStatement(SQL_UPDATE_STILLIMAGE)) {
      si_prepared_statement.setInt(1, image_id);
      si_prepared_statement.executeUpdate();
    }
    try (PreparedStatement si_meta_prepared_statement =
        conn.prepareStatement(SQL_UPDATE_STILLIMAGE_META)) {
      si_meta_prepared_statement.setInt(1, image_id);
      si_meta_prepared_statement.executeUpdate();
    }
  }

  public static void delete_image(Connection conn, int image_id, boolean close_conn)
      throws SQLException, NotFoundException {
    SpatialObject.update_image_id_of_object(
        conn, SpatialObject.select_object_id_by_image(conn, image_id), 0, false);
    delete_object(conn, SQL_DELETE_IMAGE, image_id);
    if (close_conn) conn.close();
  }

  private static Image convert_ord_image_to_image(OrdImage ord_image)
      throws IOException, SQLException {
    BufferedImage buffered_image =
        ImageIO.read(new ByteArrayInputStream(ord_image.getDataInByteArray()));
    return SwingFXUtils.toFXImage(buffered_image, null);
  }

  public static Image load_image(Connection conn, int image_id)
      throws SQLException, NotFoundException, IOException {
    Image img = convert_ord_image_to_image(get_ord_image(conn, image_id, SQL_SELECT_IMAGE));
    conn.close();
    return img;
  }

  private static OrdImage get_ord_image(Connection conn, int image_id, String sqlSelectImage)
      throws SQLException, NotFoundException {
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

  public static Image process_image(
      Connection conn,
      int image_id,
      String op_code,
      double param1,
      double param2,
      double param3,
      double param4)
      throws NotFoundException, SQLException, IOException {
    if (image_id != 0) {
      final boolean previous_auto_commit = conn.getAutoCommit();
      conn.setAutoCommit(false);
      try {
        OrdImage ord_image = select_ord_image_for_update(conn, image_id);
        execute_processing_of_image(ord_image, op_code, param1, param2, param3, param4);
        save_ord_image(conn, image_id, ord_image);
        return convert_ord_image_to_image(ord_image);
      } finally {
        conn.setAutoCommit(previous_auto_commit);
        conn.close();
      }
    } else {
      return null;
    }
  }

  static void execute_processing_of_image(
      OrdImage ord_image,
      String op_code,
      double param1,
      double param2,
      double param3,
      double param4)
      throws SQLException {
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
              "cut=" + (int) param1 + " " + (int) param2 + " " + (int) param3 + " " + (int) param4);
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

  public static Image find_most_similar_image(
      Connection conn,
      int image_id,
      double ac_weight,
      double ch_weight,
      double pc_weight,
      double tx_weight)
      throws SQLException, NotFoundException, IOException {
    if (image_id != 0) {
      try (PreparedStatement prepared_statement = conn.prepareStatement(SQL_SELECT_SIMILAR_IMAGE)) {
        prepared_statement.setDouble(1, ac_weight);
        prepared_statement.setDouble(2, ch_weight);
        prepared_statement.setDouble(3, pc_weight);
        prepared_statement.setDouble(4, tx_weight);
        prepared_statement.setInt(5, image_id);
        Image img;
        try (conn;
            ResultSet result_set = prepared_statement.executeQuery()) {
          if (result_set.next()) {
            image_id = result_set.getInt(1);
            img = load_image(conn, image_id);
          } else {
            throw new NotFoundException();
          }
        }
        conn.close();
        return img;
      }
    } else {
      return null;
    }
  }
}
