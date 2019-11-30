-- canvas has coordinates [0,0] in the left bottom corner

DELETE FROM Village WHERE o_type = 'house';

INSERT INTO Village (o_name, o_type, geometry) VALUES (
    'A', 'house',
    SDO_GEOMETRY(2003, NULL, NULL, -- two-dimensional polygon
        SDO_ELEM_INFO_ARRAY(1, 1003, 3), --o ne rectangle (1003 = exterior)
        SDO_ORDINATE_ARRAY(20,35, 65,50) -- only 2 points needed to
            -- define rectangle (lower left and upper right) with
            -- Cartesian-coordinate data
    )
);