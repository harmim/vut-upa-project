package upa.db;

import oracle.jdbc.pool.OracleDataSource;
import oracle.ord.im.OrdImage;
import upa.db.multimedia.Image;
import upa.db.spatial.*;

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

//            save_images_to_db(ods);
            save_object_to_db(ods);

        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
        }
    }

    private static void save_images_to_db(OracleDataSource ods) {
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

    private static void save_object_to_db(OracleDataSource ods) throws Exception {
        try (Connection conn = ods.getConnection()) {
            // rectangle
//            Rectangle.update_geometry_in_db(conn, 1, new double[]{200,200, 300,300});
//            Rectangle.insert_new_to_db(conn, "Z", "House", new double[]{20,20, 120,120});
//            SpatialObject.delete_spatial_object_from_db(conn, 3);
//            Rectangle.insert_new_to_db(conn, "X", "Building", new double[]{20,20, 120,120});

            Circle.insert_new_to_db(conn, "C", "Circle", new double[]{15.0, 15.0, 5.0});
            Circle.update_geometry_in_db(conn, 4, new double[]{20.00,20.00, 10.0});

//            // line-string
//            StraightLineString.update_geometry_in_db(conn, 2, new double[]{130,35,  180,45, 205,25, 250,55});
//            StraightLineString.insert_new_to_db(
//                    conn, "K", "Kine", new double[]{130,65,  180,75, 205,55, 250,85}
//            );

            // circles
            CircleCollection circles = new CircleCollection();
            circles.compute_coordinates_of_disjoint_circles(8.0, 5, 50.0, 50, true);
            circles.insert_new_to_db(conn, "F-circles", "trees");

            CircleCollection circles1 = new CircleCollection();
            circles1.compute_coordinates_of_disjoint_circles(8.0, 5, 50.0, 65, false);
            circles1.insert_new_to_db(conn, "G-circles", "trees");

            double[][] circles_info = new double[][] {
                    {0, 0, 5},
                    {5, 5, 5},
                    {10, 10, 5},
            };
            CircleCollection circle2 = new CircleCollection();
            circle2.compute_coordinates_of_various_circles(circles_info);
            circle2.insert_new_to_db(conn, "B-circles", "bushes");

            CircleCollection.delete_circle_from_collection(conn, 6, new int[]{2, 3});
            CircleCollection.update_geometry_of_collection(conn, 6, 10, 10, 75);
        } catch (SQLException | IOException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
        }
    }
}
