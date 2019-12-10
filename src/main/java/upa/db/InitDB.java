package upa.db;

import oracle.jdbc.pool.OracleDataSource;
import upa.db.multimedia.DBImage;
import upa.db.queries.Mask;
import upa.db.queries.SpatialOperators;
import upa.db.spatial.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Arrays;
import java.sql.Statement;

public class InitDB {


  public static OracleDataSource start(String username, String password) {
    System.out.println("*** STARTING INIT DB ***");
    OracleDataSource ods;
    try {
      // create a OracleDataSource instance
      ods = new OracleDataSource();
      ods.setURL("jdbc:oracle:thin:@//gort.fit.vutbr.cz:1521/orclpdb");
      /**
       * * To set System properties, run the Java VM with the following at its command line: ...
       * -Dlogin=LOGIN_TO_ORACLE_DB -Dpassword=PASSWORD_TO_ORACLE_DB ... or set the project
       * properties (in NetBeans: File / Project Properties / Run / VM Options)
       */

      ods.setUser(username);
      ods.setPassword(password);

      try (Connection conn = ods.getConnection()) {
        try (Statement stmt = conn.createStatement()) {
          // select something from the system's dual table
          try (ResultSet rset =
              stmt.executeQuery("select 1+2.0 as col1, 'foo' as col2 from dual")) {
            // ... a usage of the result set
          }
        }
      }
    } catch (SQLException sqlEx) {
      System.err.println("SQLException: " + sqlEx.getMessage());
      return null;
    }
    return ods;
  }
}
