-- canvas has coordinates [0,0] in the left bottom corner

DELETE FROM Village;

INSERT INTO Village (o_name, o_type, geometry) VALUES (
    'A', 'House',
    SDO_GEOMETRY(2003, NULL, NULL, -- two-dimensional polygon
        SDO_ELEM_INFO_ARRAY(1, 1003, 3), -- one rectangle (1003 = exterior)
        SDO_ORDINATE_ARRAY(20,35, 65,50) -- only 2 points needed to
            -- define rectangle (lower left and upper right) with
            -- Cartesian-coordinate data
    )
);

INSERT INTO Village (o_name, o_type, geometry) VALUES (
    'L', 'Line',
    -- one or more line segments in two dimensions
    SDO_GEOMETRY(2002, NULL, NULL,
    -- line string is made up of straight line segments and that the ordinates for this line string start at offset 1
    -- the end point of this line string is determined by the starting offset of the second line string
        SDO_ELEM_INFO_ARRAY(1, 2, 1),
    -- points which line segments contains (the segments are created between them)
        SDO_ORDINATE_ARRAY(10,25, 20,30, 25,25, 30,30))
);

INSERT INTO Village (o_name, o_type, geometry) VALUES (
    'E-circles', 'grass',
    SDO_GEOMETRY(2004, NULL, NULL, -- 2D collection
    -- 3 exterior circles (center-bottom, right-middle, center-top)
        SDO_ELEM_INFO_ARRAY(1,1003,4, 7,1003,4, 13,1003,4, 19,1003,4, 25,1003,4),
        SDO_ORDINATE_ARRAY(
            54.0, 50.0, 58.0, 54.0, 54.0, 58.0, 66.0, 50.0, 70.0, 54.0, 66.0, 58.0, 78.0, 50.0, 82.0, 54.0, 78.0, 58.0, 90.0, 50.0, 94.0, 54.0, 90.0, 58.0, 102.0, 50.0, 106.0, 54.0, 102.0, 58.0
        )
    )
);

SELECT o_name, SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geometry, 0.1) valid
FROM Village;
-- with respect to the accurance that is set in the SDO_GEOM_METADATA
SELECT v.o_name, v.geometry.ST_isValid()
FROM Village v;