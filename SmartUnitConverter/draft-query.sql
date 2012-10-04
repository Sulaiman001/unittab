
-- save to history

INSERT OR REPLACE INTO unitHistory VALUES (datetime(), 3);

-- clean history

DELETE FROM unitHistory WHERE lastUsed NOT IN
(
SELECT lastUsed FROM unitHistory ORDER BY lastUsed DESC LIMIT 2
)
;
sdsdsdf

DROP TABLE "unitHistory";
CREATE TABLE "unitHistory" (
    "lastUsed" DATETIME PRIMARY KEY,
    "unitId" INTEGER UNIQUE NOT NULL,
    FOREIGN KEY("unitId") REFERENCES "unit"(id)
);





-- Create tables

CREATE TABLE "category" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "name" TEXT NOT NULL
);

CREATE TABLE "unit" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "categoryId" INTEGER NOT NULL,
    "name" TEXT,
    "shortName" TEXT,
    FOREIGN KEY(categoryId) REFERENCES category(id)
);

CREATE TABLE "conversion" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "base" INTEGER NOT NULL,
    "target" INTEGER NOT NULL,
    "fx" REAL,
    "formula" TEXT,
    "reversedFormula" TEXT,
    FOREIGN KEY("base") REFERENCES "unit"(id),
    FOREIGN KEY("target") REFERENCES "unit"(id)
);


-- Insert test data

-- Insert unit to category 47 Length / Distance

INSERT INTO unit(id, categoryId, name, shortName) VALUES (1,47,"metre,meter","m");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (2,47,"kilometer","km");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (3,47,"hectometer","hm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (4,47,"dekameter","dam");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (5,47,"decimeter","dm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (6,47,"centimeter","cm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (7,47,"millimeter","mm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (8,47,"micrometer","μm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (9,47,"foot","ft");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (10,47,"inch","in");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (11,47,"mile International",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (12,47,"mile statute",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (13,47,"nautical mile UK",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (14,47,"nautical mile Int.",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (15,47,"yard","yd");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (16,47,"furlong","fur");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (17,47,"rope",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (18,47,"rod","rd");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (19,47,"league","lea");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (20,47,"chain","ch");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (21,47,"perch",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (22,47,"fathom","fath");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (23,47,"ell",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (24,47,"link","li");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (25,47,"cubit (UK)",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (26,47,"hand",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (27,47,"span cloth",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (28,47,"finger cloth",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (29,47,"nail cloth",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (30,47,"barleycorn",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (31,47,"mil thou",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (32,47,"arpent",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (33,47,"pica",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (34,47,"point",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (35,47,"twip",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (36,47,"aln",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (37,47,"famn",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (38,47,"caliber","cl");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (39,47,"ken",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (40,47,"reed",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (41,47,"handbreadth",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (42,47,"fingerbreadth",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (43,47,"smoot",null);
INSERT INTO unit(id, categoryId, name, shortName) VALUES (44,47,"nanometer","nm");
INSERT INTO unit(id, categoryId, name, shortName) VALUES (45,47,"angstrom","Å");

-- insert rates to category 47 Length / Distance

INSERT INTO conversion(base, target, fx) VALUES (1,2,0.001);
INSERT INTO conversion(base, target, fx) VALUES (1,3,0.01);
INSERT INTO conversion(base, target, fx) VALUES (1,4,0.1);
INSERT INTO conversion(base, target, fx) VALUES (1,5,10);
INSERT INTO conversion(base, target, fx) VALUES (1,6,100);
INSERT INTO conversion(base, target, fx) VALUES (1,7,1000);
INSERT INTO conversion(base, target, fx) VALUES (1,8,1000000);
INSERT INTO conversion(base, target, fx) VALUES (1,9,3.280839895);
INSERT INTO conversion(base, target, fx) VALUES (1,10,39.3700787402);
INSERT INTO conversion(base, target, fx) VALUES (1,11,0.0006213712);
INSERT INTO conversion(base, target, fx) VALUES (1,12,0.0006213699);
INSERT INTO conversion(base, target, fx) VALUES (1,13,0.0005396118);
INSERT INTO conversion(base, target, fx) VALUES (1,14,0.0005399568);
INSERT INTO conversion(base, target, fx) VALUES (1,15,1.0936132983);
INSERT INTO conversion(base, target, fx) VALUES (1,16,0.0049709695);
INSERT INTO conversion(base, target, fx) VALUES (1,17,0.1640419948);
INSERT INTO conversion(base, target, fx) VALUES (1,18,0.1988387815);
INSERT INTO conversion(base, target, fx) VALUES (1,19,0.0002071237);
INSERT INTO conversion(base, target, fx) VALUES (1,20,0.0497096954);
INSERT INTO conversion(base, target, fx) VALUES (1,21,0.1988387815);
INSERT INTO conversion(base, target, fx) VALUES (1,22,0.5468066492);
INSERT INTO conversion(base, target, fx) VALUES (1,23,0.8748906387);
INSERT INTO conversion(base, target, fx) VALUES (1,24,4.9709695379);
INSERT INTO conversion(base, target, fx) VALUES (1,25,2.1872265967);
INSERT INTO conversion(base, target, fx) VALUES (1,26,9.842519685);
INSERT INTO conversion(base, target, fx) VALUES (1,27,4.3744531934);
INSERT INTO conversion(base, target, fx) VALUES (1,28,8.7489063867);
INSERT INTO conversion(base, target, fx) VALUES (1,29,17.4978127734);
INSERT INTO conversion(base, target, fx) VALUES (1,30,118.1102362205);
INSERT INTO conversion(base, target, fx) VALUES (1,31,39370.078740158);
INSERT INTO conversion(base, target, fx) VALUES (1,32,0.0170877078);
INSERT INTO conversion(base, target, fx) VALUES (1,33,236.220472441);
INSERT INTO conversion(base, target, fx) VALUES (1,34,2834.6456692913);
INSERT INTO conversion(base, target, fx) VALUES (1,35,56692.913385827);
INSERT INTO conversion(base, target, fx) VALUES (1,36,1.6841317365);
INSERT INTO conversion(base, target, fx) VALUES (1,37,0.5613772455);
INSERT INTO conversion(base, target, fx) VALUES (1,38,3937.0078740158);
INSERT INTO conversion(base, target, fx) VALUES (1,39,0.4720632942);
INSERT INTO conversion(base, target, fx) VALUES (1,40,0.3645377661);
INSERT INTO conversion(base, target, fx) VALUES (1,41,13.1233595801);
INSERT INTO conversion(base, target, fx) VALUES (1,42,52.4934383202);
INSERT INTO conversion(base, target, fx) VALUES (1,43,0.5876131155);
INSERT INTO conversion(base, target, fx) VALUES (1,44,1000000000);
INSERT INTO conversion(base, target, fx) VALUES (1,45,10000000000);

