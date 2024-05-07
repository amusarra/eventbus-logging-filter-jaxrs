-- Description: Load data into the database

--  Insert data into the tables Horse
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (1, 'Judio XXXV', 'Pura Raza Española - PRE', 'Black', '2012-05-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (2, 'Fuego de Cardenas', 'Pura Raza Española - PRE', 'Grey', '2010-03-01');
INSERT INTO horse(id, name, breed, coat, dateOfBirth) VALUES (3, 'Francisco', 'Puro Sangue Arabo - PSA', 'Grey', '2010-03-01');
ALTER SEQUENCE Horse_SEQ RESTART WITH 4;

--  Insert data into the tables Owner
INSERT INTO owner(id, name, surname, email, phoneNumber) VALUES (1, 'John', 'Doe', 'john.doe@dontesta.it', '123456789');
INSERT INTO owner(id, name, surname, email, phoneNumber) VALUES (2, 'Mario', 'Rossi', 'mario.rossi@dontesta.it', '987654321');
ALTER SEQUENCE Owner_SEQ RESTART WITH 3;

-- Insert data into the tables Horse_Owner
INSERT INTO horse_owner(horse_id, owner_id) VALUES (1, 1);
INSERT INTO horse_owner(horse_id, owner_id) VALUES (2, 1);
INSERT INTO horse_owner(horse_id, owner_id) VALUES (3, 2);