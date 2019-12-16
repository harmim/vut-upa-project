CREATE TABLE "EXCEL_AT_UPA_42"
(
	"ID" NUMBER GENERATED AS IDENTITY
		CONSTRAINT "EXCEL_AT_UPA_42_PK" PRIMARY KEY
);

CREATE TABLE "IMAGES"
(
	"IMAGE_ID" NUMBER GENERATED AS IDENTITY
		CONSTRAINT "PK_IMAGE_ID" PRIMARY KEY,
	"IMAGE" "ORDSYS"."ORDIMAGE",
	"IMAGE_SI" "ORDSYS"."SI_STILLIMAGE",
	"IMAGE_AC" "ORDSYS"."SI_AVERAGECOLOR",
	"IMAGE_CH" "ORDSYS"."SI_COLORHISTOGRAM",
	"IMAGE_PC" "ORDSYS"."SI_POSITIONALCOLOR",
	"IMAGE_TX" "ORDSYS"."SI_TEXTURE"
);

CREATE TABLE "VILLAGE"
(
	"O_ID" NUMBER GENERATED AS IDENTITY
		CONSTRAINT "PK_O_ID" PRIMARY KEY,
	"O_NAME" VARCHAR(256),
	"O_TYPE" VARCHAR(256),
	"IMAGE_ID" NUMBER DEFAULT 0,
	"GEOMETRY" SDO_GEOMETRY
);

DELETE
FROM "USER_SDO_GEOM_METADATA"
WHERE "TABLE_NAME" = 'VILLAGE' AND "COLUMN_NAME" = 'GEOMETRY';

INSERT INTO "USER_SDO_GEOM_METADATA"
VALUES ('VILLAGE', 'GEOMETRY', "SDO_DIM_ARRAY"("SDO_DIM_ELEMENT"('X', 0, 800, 0.1), "SDO_DIM_ELEMENT"('Y', 0, 600, 0.1)), NULL);

CREATE TABLE "CIRCLECOLLECTION"
(
	"C_ID" NUMBER NOT NULL
		CONSTRAINT "PK_C_ID" PRIMARY KEY
		CONSTRAINT "FK_C_ID" REFERENCES "VILLAGE",
	"X_START" BINARY_DOUBLE NOT NULL,
	"Y_START" BINARY_DOUBLE NOT NULL,
	"R0" BINARY_DOUBLE NOT NULL,
	"N" NUMBER NOT NULL
);

CREATE INDEX "VILLAGE_SPATIAL_IDX"
	ON "VILLAGE" ("GEOMETRY")
	INDEXTYPE IS "MDSYS"."SPATIAL_INDEX_V2";
