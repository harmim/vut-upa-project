-------------------------------------------------------------------
-- CREATE THE SPATIAL INDEX --
-------------------------------------------------------------------
CREATE INDEX village_spatial_idx
    ON Village (geometry)
    INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;
-- Preceding statement created an R-tree index.

-------------------------------------------------------------------
-- SDO_NN_DISTANCE -- object selected by ID
-- Returns the distance of an object returned by this operator --
-------------------------------------------------------------------
SELECT v1.o_id, SDO_NN_DISTANCE(1) dist
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND v2.o_id = 2
  AND SDO_NN(v1.geometry, v2.geometry, 'sdo_num_res=8 distance=200', 1) = 'TRUE'
  AND v1.o_type IN ('trees', 'bushes1', 'Kine')
ORDER BY dist;

-------------------------------------------------------------------
-- SDO_NN_DISTANCE -- objects selected by TYPE
-- Returns the distance of an object returned by this operator --
-------------------------------------------------------------------
SELECT v1.o_id, SDO_NN_DISTANCE(1) dist
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND v2.o_type IN ('Line', 'Kine')
  AND SDO_NN(v1.geometry, v2.geometry, 'sdo_num_res=8 distance=200', 1) = 'TRUE'
  AND v1.o_type IN ('trees', 'bushes1', 'Kine')
ORDER BY dist;

-------------------------------------------------------------------
-- SDO_RELATE -- object selected by ID - UNION ALL more masks
-- Determines whether or not two geometries interact in a specified way --
-------------------------------------------------------------------
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_RELATE(v1.geometry, v2.geometry, 'mask=INSIDE') = 'TRUE'
  AND v2.o_id = 12
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2')
UNION ALL
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_RELATE(v1.geometry, v2.geometry, 'mask=OVERLAPBDYINTERSECT') = 'TRUE'
  AND v2.o_id = 12
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2');

-------------------------------------------------------------------
-- SDO_RELATE -- objects selected by TYPE - UNION ALL more masks
-- Determines whether or not two geometries interact in a specified way --
-------------------------------------------------------------------
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_RELATE(v1.geometry, v2.geometry, 'mask=INSIDE') = 'TRUE'
  AND v2.o_type IN ('House', 'T2')
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2')
UNION ALL
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_RELATE(v1.geometry, v2.geometry, 'mask=OVERLAPBDYINTERSECT') = 'TRUE'
  AND v2.o_type IN ('House', 'T2')
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2');

-------------------------------------------------------------------
-- SDO_FILTER -- object by ID
-- Specifies which geometries may interact with a given geometry. --
-------------------------------------------------------------------
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_FILTER(v1.geometry, v2.geometry) = 'TRUE'
  AND v2.o_id = 12
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2');

-------------------------------------------------------------------
-- SDO_FILTER -- object by TYPE
-- Specifies which geometries may interact with a given geometry. --
-------------------------------------------------------------------
SELECT /*+ INDEX(v village_spatial_idx) */ v1.o_id
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND SDO_FILTER(v1.geometry, v2.geometry) = 'TRUE'
  AND v2.o_type IN ('House', 'T2')
  AND v1.o_type IN ('House', 'bushes1', 'Line', 'T2');

-------------------------------------------------------------------
-- AREA -- object by ID
-- Returns the area of a two-dimensional polygon. --
-------------------------------------------------------------------
SELECT o_id, SDO_GEOM.SDO_AREA(geometry, 0.005)
FROM Village
WHERE o_id = 2;

-------------------------------------------------------------------
-- LENGTH -- object by ID
-- Returns the length or perimeter of a geometry object. --
-------------------------------------------------------------------
SELECT o_id, SDO_GEOM.SDO_LENGTH(geometry, 0.005)
FROM Village
WHERE o_id = 7;

-------------------------------------------------------------------
-- DIAMETER -- object by ID
-- Returns the length of the diameter of a geometry object. --
-------------------------------------------------------------------
SELECT o_id, SDO_GEOM.SDO_DIAMETER(geometry, 0.005)
FROM Village
WHERE o_id = 4;

-------------------------------------------------------------------
-- DISTANCE -- objects by ID
-- Computes the minimum distance between two geometry objects, which
-- is the distance between the closest pair of points or segments of
-- the two objects. --
-------------------------------------------------------------------
SELECT SDO_GEOM.SDO_DISTANCE(v1.geometry, v2.geometry, 0.005)
FROM Village v1,
     Village v2
WHERE v1.o_id = 4
  AND v2.o_id = 7;