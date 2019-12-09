-- canvas has coordinates [0,0] in the left bottom corner

DELETE
FROM Village;

INSERT INTO Village (o_name, o_type, geometry)
VALUES ('A', 'House',
        SDO_GEOMETRY(2003, NULL, NULL, -- two-dimensional polygon
                     SDO_ELEM_INFO_ARRAY(1, 1003, 3), -- one rectangle (1003 = exterior)
                     SDO_ORDINATE_ARRAY(20, 35, 65, 50) -- only 2 points needed to
        -- define rectangle (lower left and upper right) with
        -- Cartesian-coordinate data
            ));

INSERT INTO Village (o_name, o_type, geometry)
VALUES ('L', 'Line',
           -- one or more line segments in two dimensions
        SDO_GEOMETRY(2002, NULL, NULL,
            -- line string is made up of straight line segments and that the ordinates for this line string start at offset 1
            -- the end point of this line string is determined by the starting offset of the second line string
                     SDO_ELEM_INFO_ARRAY(1, 2, 1),
            -- points which line segments contains (the segments are created between them)
                     SDO_ORDINATE_ARRAY(10, 25, 20, 30, 25, 25, 30, 30)));

SELECT o_name, SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geometry, 0.1) valid
FROM Village;
-- with respect to the accurance that is set in the SDO_GEOM_METADATA
SELECT v.o_name, v.geometry.ST_isValid()
FROM Village v;

COMMIT;