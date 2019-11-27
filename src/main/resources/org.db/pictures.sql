-- drop tables
DROP TABLE Images;

-- create the Images table
CREATE TABLE Images (
    image_id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    image ORDSYS.ORDImage,
    -- collection of pixels representing a 2-dimensional image
    image_si ORDSYS.SI_StillImage,
    -- the "average" color of a given image
    image_ac ORDSYS.SI_AverageColor,
    -- the occurrence of each color is found
    image_ch ORDSYS.SI_ColorHistogram,
    -- the location of specific colors
    image_pc ORDSYS.SI_PositionalColor,
    -- coarseness. contrast. direction of granularity
    image_tx ORDSYS.SI_Texture
);

-- trigger to generate 'SI_StillImage' and its corresponding SI metadata for each inserted or updated ORDImage value
CREATE OR REPLACE TRIGGER generate_image_features
    AFTER INSERT OR UPDATE OF image on Images
    FOR EACH ROW
DECLARE
    si ORDSYS.SI_StillImage;
    BEGIN
        si := new SI_StillImage(:NEW.image.getContent());
        UPDATE Images i SET image_si = si,
        image_ac = SI_AverageColor(si),
        image_ch = SI_ColorHistogram(si),
        image_pc = SI_PositionalColor(si),
        image_tx = SI_Texture(si)
        WHERE i.image_id = :NEW.image_id;
END;
/