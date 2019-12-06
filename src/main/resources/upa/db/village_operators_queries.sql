-------------------------------------------------------------------
-- CREATE THE SPATIAL INDEX --
-------------------------------------------------------------------
CREATE INDEX village_spatial_idx
    ON Village (geometry)
    INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;
-- Preceding statement created an R-tree index.

SELECT /*+ INDEX(v village_spatial_idx) */ v.o_id, SDO_NN_DISTANCE(1) dist
FROM Village v
WHERE SDO_NN(v.geometry, (SELECT geometry FROM Village WHERE o_id = 2), 'sdo_num_res=8 distance=200', 1) = 'TRUE'
  AND v.o_type IN ('trees', 'bushes1', 'Kine')
ORDER BY dist;