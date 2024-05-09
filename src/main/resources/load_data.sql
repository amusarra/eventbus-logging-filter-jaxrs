-- Description: Load data into the database

--  Insert data into the tables Horse
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (1, 'Judio XXXV', 'Pura Raza Española - PRE', 'Black', '2012-05-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (2, 'Fuego de Cardenas', 'Pura Raza Española - PRE', 'Grey', '2010-03-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (3, 'Francisco', 'Puro Sangue Arabo - PSA', 'Grey', '2010-03-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (4, 'Shirus', 'Quarab', 'Grey', '2012-05-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (5, 'Artemis', 'San Fratellano', 'Black', '2024-05-01');
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