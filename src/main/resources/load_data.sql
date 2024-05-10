-- Description: Load data into the database

--  Insert data into the tables Horse
INSERT INTO horse (id, name, sex, coat, breed, dateOfBirth, registrationNumber, microchipNumber, passportNumber, height)
VALUES (1, 'Thunder', 'M', 'Black', 'Quarter Horse', '2015-05-12', 'AQHA123456', '123456789012345', 'PASS123', 150);

INSERT INTO horse (id, name, sex, coat, breed, dateOfBirth, registrationNumber, microchipNumber, passportNumber, height)
VALUES (2, 'Bella', 'F', 'Bay', 'Thoroughbred', '2018-08-20', 'TB123', '987654321098765', 'PASS456', 160);

INSERT INTO horse (id, name, sex, coat, breed, dateOfBirth, registrationNumber, microchipNumber, passportNumber, height)
VALUES (3, 'Spirit', 'M', 'Chestnut', 'Arabian', '2017-03-05', 'ARAB567', '543210987654321', 'PASS789', 155);

INSERT INTO horse (id, name, sex, coat, breed, dateOfBirth, registrationNumber, microchipNumber, passportNumber, height)
VALUES (4, 'Whisper', 'F', 'Grey', 'Andalusian', '2016-10-15', 'AND456', '246813579135790', 'PASS246', 158);

INSERT INTO horse (id, name, sex, coat, breed, dateOfBirth, registrationNumber, microchipNumber, passportNumber, height)
VALUES (5, 'Blaze', 'M', 'Palomino', 'Paint', '2019-07-25', 'PAINT789', '987654321', 'PASS357', 152);

ALTER SEQUENCE Horse_SEQ RESTART WITH 6;

--  Insert data into the tables Owner
INSERT INTO owner(id, name, surname, email, phoneNumber, address, city, state, zipCode, country)
VALUES (1, 'John', 'Doe', 'john.doe@dontesta.it', '123456789', 'Via Roma 1', 'Rome', 'RM', '00100', 'Italy');
INSERT INTO owner(id, name, surname, email, phoneNumber, address, city, state, zipCode, country)
VALUES (2, 'Mario', 'Rossi', 'mario.rossi@dontesta.it', '987654321', 'Via Garibaldi 1', 'Milan', 'MI', '20100',
        'Italy');
INSERT INTO owner(id, name, surname, email, phoneNumber, address, city, state, zipCode, country)
VALUES (3, 'Valentina', 'Rossi', 'valentina.rossi@dontesta.it', '987654321', 'Via Bronte 1', 'Bronte', 'CT', '95034',
        'Italy');
ALTER SEQUENCE Owner_SEQ RESTART WITH 4;

-- Insert data into the tables Horse_Owner
INSERT INTO horse_owner(horses_id, owners_id) VALUES (1, 1);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (1, 2);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (2, 1);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (3, 2);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (4, 1);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (4, 2);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (5, 1);
INSERT INTO horse_owner(horses_id, owners_id) VALUES (5, 3);