package org.db;

import oracle.jdbc.pool.OracleDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InitDB {

    public static void start() throws Exception {
        System.out.println("*** STARTING INIT DB ***");
        try {
            // create a OracleDataSource instance
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:@//gort.fit.vutbr.cz:1521/orclpdb");
            /**
             * *
             * To set System properties, run the Java VM with the following at
             * its command line: ... -Dlogin=LOGIN_TO_ORACLE_DB
             * -Dpassword=PASSWORD_TO_ORACLE_DB ... or set the project
             * properties (in NetBeans: File / Project Properties / Run / VM
             * Options)
             */
            ods.setUser(System.getProperty("login"));
            ods.setPassword(System.getProperty("password"));
            /**
             *
             */

            saved_images_to_db(ods);

            // connect to the database
            try (Connection conn = ods.getConnection()) {
                // create a Statement
                try (Statement stmt = conn.createStatement()) {
                    // select something from the system's dual table
                    try (ResultSet rset = stmt.executeQuery(
                            "select 1+2 as col1, 3-4 as col2 from dual")) {
                        // iterate through the result and print the values
                        while (rset.next()) {
                            System.out.println("col1: '" + rset.getString(1)
                                    + "'\tcol2: '" + rset.getString(2) + "'");
                        }
                    } // close the ResultSet
                } // close the Statement
            } // close the connection
        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
        }

    }

    private static void saved_images_to_db(OracleDataSource ods) {
        // connect to the database
        File images_dir = new File("./images/");
        File[] image_name_list = images_dir.listFiles();
        if (image_name_list != null) {
            try (Connection conn = ods.getConnection()) {
                // save images to database
                for (File image_name : image_name_list) {
                    Image image = new Image(image_name.getName(), image_name.getParent());
                    image.save_image_from_file_to_db(conn, image_name.getPath());
                }
                System.out.println("*** SAVED IMAGES DONE ***");

                // delete images from databases
                Image.delete_image_from_db(conn, "car3.gif", "./images");
                System.out.println("*** DELETE IMAGES DONE ***");

            } catch (SQLException | Image.NotFoundException | IOException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
            }
        }
    }
}
