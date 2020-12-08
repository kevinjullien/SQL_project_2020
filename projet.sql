-- Kevin Jullien, Narjis Laraki

DROP SCHEMA IF EXISTS projet CASCADE;
CREATE SCHEMA projet;


CREATE TABLE projet.formations ( 
	id_formation SERIAL PRIMARY KEY,
	nom_formation VARCHAR(100) NOT NULL CHECK (nom_formation<>''),
	ecole VARCHAR(100) NOT NULL
);

CREATE TABLE projet.blocs ( 
	code CHARACTER(6) PRIMARY KEY,
	formation INTEGER REFERENCES projet.formations (id_formation) NOT NULL,
	nbr_exams_non_reserves INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE projet.utilisateurs (
	id_utilisateur SERIAL PRIMARY KEY,
	email VARCHAR(100) unique NOT NULL check (email SIMILAR TO '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}'),
	nom_utilisateur VARCHAR(100) unique NOT NULL CHECK (nom_utilisateur<>''),
	mot_de_passe VARCHAR(100) NOT NULL CHECK (mot_de_passe<>''),
	bloc CHARACTER(6) REFERENCES projet.blocs(code) NOT NULL
);

CREATE TABLE projet.examens (
	code CHARACTER(6) PRIMARY KEY CHECK(code SIMILAR TO 'IPL[0-9]{3}'),
	nom_examen VARCHAR(100) NOT NULL CHECK(nom_examen<>''),
	bloc CHARACTER(6) REFERENCES projet.blocs (code) NOT NULL,
	duree INTERVAL NOT NULL CHECK (duree > INTERVAL '0' MINUTE),
	est_sur_machine BOOLEAN NOT NULL,
	date_heure_prevue TIMESTAMP,
	completement_reserve BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE projet.locaux (
	id_local SERIAL PRIMARY KEY,
	nom_local VARCHAR(100) UNIQUE NOT NULL CHECK(nom_local<>''),
	places_disponibles INTEGER NOT NULL CHECK (places_disponibles>0),
	contient_machines BOOLEAN NOT NULL
);

CREATE TABLE projet.reservations (
	local_examen INTEGER REFERENCES projet.locaux (id_local) NOT NULL,
	examen CHARACTER(6) REFERENCES projet.Examens (code) NOT NULL,
	PRIMARY KEY(local_examen, examen)
);

CREATE TABLE projet.inscriptions (
	utilisateur INTEGER REFERENCES projet.utilisateurs (id_utilisateur) NOT NULL,
	examen CHARACTER(6) REFERENCES projet.examens (code) NOT NULL,
	PRIMARY KEY(utilisateur, examen)
);



-- Fonctions Admin


-- 1. Encoder un local

CREATE OR REPLACE FUNCTION projet.ajouterLocal(nom VARCHAR(100), places_disponibles INTEGER, contient_machines BOOLEAN) RETURNS RECORD AS $$ 
DECLARE 
	retour RECORD;
BEGIN 
	INSERT INTO projet.locaux VALUES (DEFAULT, nom, places_disponibles, contient_machines) 
	RETURNING * into retour;
	RETURN retour;
END

$$ LANGUAGE plpgsql;


-- 2. Encoder un examen *** Vérifier que tous les checks suffisent (devrait être le cas) ***

CREATE OR REPLACE FUNCTION projet.ajouterExamen(code CHAR(6), nom VARCHAR(100), bloc CHAR(6), duree INTERVAL, estSurMachine BOOLEAN) RETURNS RECORD AS $$
DECLARE
	retour RECORD;
BEGIN
	INSERT INTO projet.examens VALUES (code, nom, bloc, duree, estSurMachine, null, DEFAULT) RETURNING * INTO retour;
	RETURN retour;
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.trigger_ajouterExamen() RETURNS TRIGGER AS $$
DECLARE

BEGIN
	UPDATE projet.blocs SET nbr_exams_non_reserves = (nbr_exams_non_reserves + 1 ) WHERE NEW.bloc = code;
	RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_ajouterExamen AFTER INSERT ON projet.examens
FOR EACH ROW EXECUTE PROCEDURE projet.trigger_ajouterExamen();


-- 3. Encoder l’heure de début d’un examen
	
CREATE OR REPLACE FUNCTION projet.encoderHeureDebutExamen(dateEtHeure_demande TIMESTAMP, examen_demande CHARACTER(6)) RETURNS VOID AS $$
DECLARE
	
BEGIN
	-- Si l'examen n'est pas répertorié
	IF NOT EXISTS (SELECT * FROM projet.examens e WHERE e.code = examen_demande) THEN
		RAISE 'Cet examen n''existe pas';
	END IF;
	
	-- Tout va bien
	UPDATE projet.examens SET date_heure_prevue = dateEtHeure_demande WHERE code = examen_demande;
	END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.trigger_encoderHeureDebutExamen() RETURNS TRIGGER AS $$
DECLARE 
	
BEGIN

	-- Si l'examen ne contient aucun inscrit
	IF NOT EXISTS (SELECT * FROM projet.inscriptions i WHERE i.examen = NEW.code) THEN
		RAISE 'Examen ne contient aucun inscrit';
	END IF;
	
	-- Si un examen du même bloc se déroule le même jour
	IF EXISTS (SELECT * FROM projet.examens e WHERE e.bloc = NEW.bloc AND e.date_heure_prevue IS NOT NULL
			   AND DATE_PART('DAY', e.date_heure_prevue) = DATE_PART('DAY', NEW.date_heure_prevue)
			   AND DATE_PART('MONTH', e.date_heure_prevue) = DATE_PART('MONTH', NEW.date_heure_prevue)
			   AND e.code != NEW.code)
    THEN
		RAISE 'Un autre examen du même bloc se déroule le même jour';
	END IF;
	
	-- Si des conflits horaires existent
	IF EXISTS (SELECT * FROM projet.examens e WHERE (e.date_heure_prevue <= (NEW.date_heure_prevue + NEW.duree)) 
			   AND (NEW.date_heure_prevue <= (e.date_heure_prevue + e.duree))
			   AND e.code != NEW.code
			   AND EXISTS (SELECT * FROM projet.inscriptions i1, projet.inscriptions i2 WHERE i1.utilisateur = i2.utilisateur 
						   AND i1.examen = NEW.code AND i2.examen = e.code AND e.code <> NEW.code))
 	THEN
		RAISE 'Il y a un conflit horaire';
	END IF;
	
	-- Encodage impossible si local/locaux réservé(s) 
	IF EXISTS (SELECT * FROM projet.reservations r WHERE (r.examen = NEW.code)) 
	THEN 
		RAISE 'Vous ne pouvez plus modifier l''heure de début ';
	END IF;
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_encoderHeureDebutExamen BEFORE UPDATE OF date_heure_prevue ON projet.examens
FOR EACH ROW EXECUTE PROCEDURE projet.trigger_encoderHeureDebutExamen();


-- 4. Réserver un local pour un examen

CREATE OR REPLACE FUNCTION projet.reserverLocalExamen(localExamen VARCHAR(100), examen CHARACTER(6)) RETURNS INTEGER AS $$
DECLARE 
    localExam INTEGER := 0;
BEGIN 
	localExam := (SELECT id_local from projet.locaux WHERE nom_local = localExamen);
    INSERT INTO projet.reservations VALUES(localExam, examen);
    RETURN localExam;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.trigger_reserverLocalExamen() RETURNS TRIGGER AS $$
DECLARE

BEGIN
	-- Si l'exament est déjà complètement réservé
	IF EXISTS (SELECT * FROM projet.examens e WHERE e.code = NEW.examen AND e.completement_reserve = TRUE)
	THEN
		RAISE 'L''examen est déjà completement réservé';
	END IF;
	
	-- S'il n'y a aucun inscrit à l'examen
	IF NOT EXISTS (SELECT * FROM projet.inscriptions i WHERE i.examen = NEW.examen)
	THEN
		RAISE 'Aucun insrit pour cet examen';
	END IF;
	
	-- Si la date de début d'examen n'a pas été encodée
	IF ((SELECT e.date_heure_prevue FROM projet.examens e WHERE e.code = NEW.examen) IS NULL)
	THEN
		RAISE 'Aucune heure n''est prévue pour cet examen';
	END IF;
	
	-- Si l'examen nécessite des machines et que le local n'en contient pas
	IF ((SELECT e.est_sur_machine FROM projet.examens e WHERE e.code = NEW.examen) = TRUE
		AND (SELECT l.contient_machines FROM projet.locaux l WHERE l.id_local = NEW.local_examen) = FALSE)
	THEN
		RAISE 'Le local que vous avez choisi ne contient pas de machines alors que l''examen est sur machines';
	END IF;
	
	-- S’il existe déjà une réservation pour un autre examen dans le local à ce moment-là
		IF EXISTS (SELECT * FROM projet.examens e1, projet.examens e2, projet.reservations r
				   WHERE e1.code = r.examen AND r.local_examen = NEW.local_examen
				   AND e1.code <> NEW.examen AND e2.code = NEW.examen
				   AND(e1.date_heure_prevue <= (e2.date_heure_prevue + e2.duree))
				   AND (e2.date_heure_prevue <= (e1.date_heure_prevue + e1.duree))
				  )
		THEN
			RAISE 'un examen se déroule déjà dans ce local au même moment';
		END IF;
		
	-- Si le compte de places totales disponibles >= au nombre d'élèves -> completement réservé
	IF ((SELECT COUNT(*) FROM projet.inscriptions i WHERE i.examen = NEW.examen) <=
		(SELECT SUM(l.places_disponibles) FROM projet.reservations r, projet.locaux l WHERE l.id_local = r.local_examen AND r.examen = NEW.examen))
	THEN
		UPDATE projet.examens SET completement_reserve = TRUE WHERE code = NEW.examen;
		UPDATE projet.blocs SET nbr_exams_non_reserves = (nbr_exams_non_reserves - 1 ) WHERE code = (SELECT e.bloc from projet.examens e WHERE e.code = NEW.examen);
	END IF;
	
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_reserverLocalExamen AFTER INSERT ON projet.reservations
FOR EACH ROW EXECUTE PROCEDURE projet.trigger_reserverLocalExamen();


-- 5.

CREATE OR REPLACE VIEW projet.visualiserHorairePourBloc AS
SELECT e.bloc, e.date_heure_prevue, e.code, e.nom_examen, count(r.local_examen) AS "quantite" 
	FROM projet.examens e LEFT OUTER JOIN projet.reservations r ON r.examen = e.code
	WHERE (e.date_heure_prevue IS NULL OR e.date_heure_prevue IS NOT NULL)
	GROUP BY e.date_heure_prevue, e.code, e.nom_examen
	ORDER BY e.date_heure_prevue;


-- 6. Visualiser les réservations d'un local particulier

CREATE OR REPLACE VIEW projet.visualiserReservationsLocal AS
SELECT l.nom_local, r.examen, e.nom_examen, e.bloc, e.date_heure_prevue, (e.date_heure_prevue + e.duree) AS "Fin" 
	FROM projet.reservations r, projet.examens e, projet.locaux l 
	WHERE r.examen = e.code
	AND r.local_examen = l.id_local;
	

-- 7. Visualier tous les examens pas encore totalement réservés

CREATE OR REPLACE VIEW projet.visualiserExamensPasEncoreReserves AS
SELECT e.code, e.nom_examen, e.bloc 
    FROM projet.examens e 
    WHERE e.completement_reserve = FALSE 
    ORDER BY e.code;


-- 8. Visualier le nombre d'examens pas encore totalement réservés par bloc

CREATE OR REPLACE VIEW projet.visualiserExamensPasEncoreReservesParBloc AS
SELECT b.code, b.nbr_exams_non_reserves
    FROM projet.blocs b;



-- User

-- A. Inscription

CREATE OR REPLACE FUNCTION projet.inscription(email VARCHAR(100), nom_utilisateur VARCHAR(100), mot_de_passe VARCHAR(100), bloc CHAR(6)) RETURNS VOID AS $$
DECLARE
BEGIN
	INSERT INTO projet.utilisateurs VALUES (default, nom_utilisateur, email, mot_de_passe, bloc);
END
$$ LANGUAGE plpgsql;


-- B. Connexion

CREATE OR REPLACE FUNCTION projet.connexion(nom VARCHAR(100)) RETURNS VARCHAR(100) AS $$
DECLARE
	mdp VARCHAR(100);
BEGIN
	mdp := (SELECT u.mot_de_passe FROM projet.utilisateurs u WHERE u.nom_utilisateur = nom);
	RETURN mdp;
END
$$ LANGUAGE plpgsql;


-- 1. Visualiser les examens

CREATE OR REPLACE VIEW projet.visualiserExamens AS
	SELECT e.code, e.nom_examen, e.bloc, e.duree FROM projet.examens e ORDER BY e.bloc, e.nom_examen;


-- 2. S'inscrire à un examen

CREATE OR REPLACE FUNCTION projet.inscriptionExamen(nom_util VARCHAR(100), code_exam CHAR(6)) RETURNS VOID AS $$
DECLARE
	id_util INTEGER;
BEGIN
	id_util := (SELECT id_utilisateur FROM projet.utilisateurs u WHERE u.nom_utilisateur = nom_util);
	INSERT INTO projet.inscriptions VALUES (id_util, code_exam);
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.trigger_inscriptionExamen() RETURNS TRIGGER AS $$
DECLARE
BEGIN
	IF EXISTS (SELECT * FROM projet.examens e WHERE e.code = NEW.examen AND e.date_heure_prevue IS NOT NULL)
	THEN
		RAISE 'Un horaire est déjà fixé, impossible de s''inscrire';
	END IF;
	RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_inscriptionExamen AFTER INSERT ON projet.inscriptions
FOR EACH ROW EXECUTE PROCEDURE projet.trigger_inscriptionExamen();


--3 Inscription à tous les examens du bloc 

CREATE OR REPLACE FUNCTION projet.inscriptionATousLesExamens(nom_util VARCHAR(100)) RETURNS VOID AS $$ 
DECLARE 
	bloc_util CHAR(6);
	examen RECORD;
BEGIN
	bloc_util := (SELECT bloc FROM projet.utilisateurs WHERE nom_utilisateur = nom_util);
	FOR examen IN SELECT * FROM projet.examens e WHERE e.bloc = bloc_util LOOP
	PERFORM * FROM projet.inscriptionExamen(nom_util, examen.code);
	END LOOP;
END
$$ LANGUAGE plpgsql;


--4 Voir son horaire d'examen 

CREATE OR REPLACE VIEW projet.voirHoraireExamen AS
	SELECT u.nom_utilisateur, e.code, e.nom_examen, e.bloc, e.date_heure_prevue, (e.date_heure_prevue + duree),
	 string_agg(l.nom_local::text, '+') 
	FROM projet.utilisateurs u, projet.inscriptions i, projet.examens e LEFT OUTER JOIN projet.reservations r ON e.code = r.examen LEFT OUTER JOIN projet.locaux l ON r.local_examen = l.id_local
	WHERE (e.date_heure_prevue IS NULL OR e.date_heure_prevue IS NOT NULL)
	AND u.id_utilisateur = i.utilisateur
	AND i.examen = e.code
	GROUP BY 1,2,3,4,5
	ORDER BY e.date_heure_prevue;
	

-- Inserts nécessaires pour le scénario

INSERT INTO projet.formations(nom_formation, ecole)
VALUES ('Informatique de gestion', 'HE Vinci');

INSERT INTO projet.blocs
VALUES ('Bloc 1', 1, default),
('Bloc 2', 1, default);

INSERT INTO projet.examens 
VALUES ('IPL100', 'APOO', 'Bloc 1', INTERVAL '120' MINUTE, false, null, default),
('IPL150', 'Algo', 'Bloc 1', INTERVAL '240' MINUTE, true, null, default),
('IPL200', 'Javascript', 'Bloc 2', INTERVAL '120' MINUTE, true, null, default);

INSERT INTO projet.locaux
VALUES (default, 'A017', 2, true),
(default, 'A019', 1, true);

-- Le mot de passe est 'pass'
INSERT INTO projet.utilisateurs
VALUES (default, 'damas@gmail.com', 'Damas', '$2a$10$J0SLoWhHkQN4LX6W9vv5POd00OsN54wLZarxQXN5n4OqTEdf8P2lO', 'Bloc 1'),
(default, 'ferneeuw@gmail.com', 'Ferneeuw', '$2a$10$J0SLoWhHkQN4LX6W9vv5POd00OsN54wLZarxQXN5n4OqTEdf8P2lO', 'Bloc 2'),
(default, 'cambron@gmail.com', 'Cambron', '$2a$10$J0SLoWhHkQN4LX6W9vv5POd00OsN54wLZarxQXN5n4OqTEdf8P2lO', 'Bloc 2');



