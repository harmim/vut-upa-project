-- drop tables
DROP TABLE Village CASCADE CONSTRAINTS;
DROP TABLE CircleCollection CASCADE CONSTRAINTS;

-- create the Village table
CREATE TABLE Village
(
    o_id     INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    o_name   VARCHAR(256),
    o_type   VARCHAR(256),
    geometry SDO_GEOMETRY
);

CREATE TABLE CircleCollection
(
    c_id    INTEGER       NOT NULL,
    x_start BINARY_DOUBLE NOT NULL,
    y_start BINARY_DOUBLE NOT NULL,
    r0      BINARY_DOUBLE NOT NULL,
    n       INTEGER       NOT NULL
);

-- set the PRIMARY KEY for Village and CircleCollections tables
ALTER TABLE Village
    ADD CONSTRAINT pk_o_id PRIMARY KEY (o_id);
ALTER TABLE CircleCollection
    ADD CONSTRAINT pk_c_id PRIMARY KEY (c_id);

-- set the FOREIGN KEY for CicleCollection table
ALTER TABLE CircleCollection
    ADD CONSTRAINT fk_c_id FOREIGN KEY (c_id) REFERENCES Village (o_id);

DELETE
FROM USER_SDO_GEOM_METADATA
WHERE TABLE_NAME = 'VILLAGE'
  AND COLUMN_NAME = 'GEOMETRY';

INSERT INTO USER_SDO_GEOM_METADATA
VALUES ('Village', 'geometry',
           -- X axis in range 0-750, Y axis in range 0-500, both with accurancy 0.1 points
        SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', 0, 750, 0.1), SDO_DIM_ELEMENT('Y', 0, 500, 0.1)),
           -- a local spatial reference system (not geographical; analytical functions will be without units)
        NULL);

-------------------------------------------------------------------
-- CREATE THE SPATIAL INDEX --
-------------------------------------------------------------------
CREATE INDEX village_spatial_idx
    ON Village (geometry)
    INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;
-- Preceding statement created an R-tree index.

-- check the validity of SDO_GEOMETRY
-- with a custom accurance = 0.01
SELECT o_name, SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(geometry, 0.1) valid
FROM Village;
-- with respect to the accurance that is set in the SDO_GEOM_METADATA
SELECT v.o_name, v.geometry.ST_isValid()
FROM Village v;