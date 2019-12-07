-------------------------------------------------------------------
-- CREATE THE SPATIAL INDEX --
-------------------------------------------------------------------
CREATE INDEX village_spatial_idx
    ON Village (geometry)
    INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;
-- Preceding statement created an R-tree index.

SELECT v1.o_id, SDO_NN_DISTANCE(1) dist
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND v2.o_id = 2
  AND SDO_NN(v1.geometry, v2.geometry, 'sdo_num_res=8 distance=200', 1) = 'TRUE'
  AND v1.o_type IN ('trees', 'bushes1', 'Kine')
ORDER BY dist;

SELECT v1.o_id, SDO_NN_DISTANCE(1) dist
FROM Village v1,
     Village v2
WHERE v1.o_id <> v2.o_id
  AND v2.o_type IN ('Line', 'Kine')
  AND SDO_NN(v1.geometry, v2.geometry, 'sdo_num_res=8 distance=200', 1) = 'TRUE'
  AND v1.o_type IN ('trees', 'bushes1', 'Kine')
ORDER BY dist;

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