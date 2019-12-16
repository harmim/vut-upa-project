package upa.db;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import upa.db.multimedia.DBImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class InitDB {
  public static OracleDataSource start(String username, String password) {
    OracleDataSource ods;
    try {
      ods = new OracleDataSource();
      ods.setURL("jdbc:oracle:thin:@//gort.fit.vutbr.cz:1521/orclpdb");

      ods.setUser(username);
      ods.setPassword(password);

      try (Connection conn = ods.getConnection()) {
        try (Statement stmt = conn.createStatement()) {
          try (ResultSet ignored = stmt.executeQuery("SELECT 1 + 2 AS COL1, 'foo' AS COL2 FROM DUAL")) {
            return ods;
          }
        }
      }
    } catch (SQLException sqlEx) {
      System.err.println("SQLException: " + sqlEx.getMessage());
      return null;
    }
  }

  public static boolean schemaExists(OracleDataSource ods) {
    try (Connection conn = ods.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        try (ResultSet ignored = stmt.executeQuery("SELECT ID FROM EXCEL_AT_UPA_42")) {
          return true;
        }
      }
    } catch (SQLException e) {
      return false;
    }
  }

  private static Reader createReader(String fileName) throws FileNotFoundException {
    return new BufferedReader(
        new FileReader(
            URLDecoder.decode(
                InitDB.class.getResource(fileName).getFile(),
                StandardCharsets.UTF_8
            )
        )
    );
  }

  public static void initSchema(OracleDataSource ods) {
    try (Connection conn = ods.getConnection()) {
      ScriptRunner sr = new ScriptRunner(conn);
      sr.runScript(createReader("init-delete.sql"));
      sr.runScript(createReader("init-create.sql"));
      sr.runScript(createReader("init-insert.sql"));
    } catch (SQLException | IOException e) {
      e.printStackTrace();
    }

    int imageId = 0;

    try (Connection conn = ods.getConnection()) {
      imageId = DBImage.save_image_from_file(conn, 0, URLDecoder.decode(
          DBImage.class.getResource("images/rigel.gif").getFile(),
          StandardCharsets.UTF_8
      ));
    } catch (SQLException | GeneralDB.NotFoundException | IOException e) {
      e.printStackTrace();
    }
    if (imageId != 0) {
      try (Connection conn = ods.getConnection()) {
        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE VILLAGE SET IMAGE_ID = ? WHERE O_NAME = 'Rigel'"
        )) {
          ps.setInt(1, imageId);
          ps.executeUpdate();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    imageId = 0;
    try (Connection conn = ods.getConnection()) {
      imageId = DBImage.save_image_from_file(conn, 0, URLDecoder.decode(
          DBImage.class.getResource("images/jupiter.gif").getFile(),
          StandardCharsets.UTF_8
      ));
    } catch (SQLException | GeneralDB.NotFoundException | IOException e) {
      e.printStackTrace();
    }
    if (imageId != 0) {
      try (Connection conn = ods.getConnection()) {
        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE VILLAGE SET IMAGE_ID = ? WHERE O_NAME = 'Jupiter'"
        )) {
          ps.setInt(1, imageId);
          ps.executeUpdate();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    imageId = 0;
    try (Connection conn = ods.getConnection()) {
      imageId = DBImage.save_image_from_file(conn, 0, URLDecoder.decode(
          DBImage.class.getResource("images/prometheus.gif").getFile(),
          StandardCharsets.UTF_8
      ));
    } catch (SQLException | GeneralDB.NotFoundException | IOException e) {
      e.printStackTrace();
    }
    if (imageId != 0) {
      try (Connection conn = ods.getConnection()) {
        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE VILLAGE SET IMAGE_ID = ? WHERE O_NAME = 'Prometheus'"
        )) {
          ps.setInt(1, imageId);
          ps.executeUpdate();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public static String getCurrentUser(OracleDataSource ods) {
    String user = "";

    try (Connection conn = ods.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("SELECT USER FROM DUAL")) {
          if (rs.next()) {
            user = rs.getString("USER");
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return user;
  }
}
