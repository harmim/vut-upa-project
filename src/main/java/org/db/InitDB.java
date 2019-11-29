package org.db;

import oracle.jdbc.pool.OracleDataSource;
import oracle.ord.im.OrdImage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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

            saved_images_to_db(ods);
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
                    Image.save_image_from_file_to_db(conn, 0, image_name.getPath());
                }
                System.out.println("*** SAVED IMAGES DONE ***");

                // delete images from database
                Image.delete_image_from_db(conn, 1);
                System.out.println("*** DELETE IMAGE DONE ***");

                // change images in db
                Image.process_image_in_db(conn, 3, "rotate", 90.0, 0.0, 0.0, 0.0);
                Image.process_image_in_db(conn, 3, "cut", 0.0, 0.0, 200.0, 200.0);
                Image.process_image_in_db(conn, 3, "mirror", 0.0, 0.0, 0.0, 0.0);
                Image.process_image_in_db(conn, 3, "scale", 2.25, 0.0, 0.0, 0.0);
                Image.process_image_in_db(conn, 3, "monochrome", 0.0, 0.0, 0.0, 0.0);

                // load most similar images from database
                int sim_image_id = Image.find_most_similar_image(conn, 3, 0.3, 0.3, 0.1, 0.3);
                OrdImage load_image = Image.load_image_from_db(conn, sim_image_id);
                load_image.getDataInFile("./src/load_image.gif");
                System.out.println("*** LOAD SIMILAR IMAGE DONE ***");

            } catch (SQLException | Image.NotFoundException | IOException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
            }
        }
    }
}
