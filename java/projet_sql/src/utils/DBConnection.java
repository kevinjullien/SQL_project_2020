/**
 * @authors Kevin Jullien et Narjis Laraki
 */
package utils;

import java.sql.*;
import java.time.*;
import java.util.Scanner;
import org.postgresql.util.PGInterval;

import static salt.BCrypt.*;
import static utils.Utils.*;

public class DBConnection {
	
	private Connection conn;
	private static final String url = "jdbc:postgresql://localhost:5432/dbkevinjullien";
	private static final String password = "password";
	private Scanner scanner = new Scanner(System.in);

	public DBConnection() {
		this.getConnection();
	}

	private void getConnection() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Driver PostgreSQL manquant !");
			System.exit(1);
		}
		try {
			conn = DriverManager.getConnection(url, "postgres", password);
		} catch (SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}
	}

	public void ajouterLocal() {
		try {

			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.ajouterLocal(?, ?, ?) "
					+ "t(id_local INTEGER, nom_local VARCHAR(100), places_disponibles INTEGER, contient_machines BOOLEAN);");

			System.out.println("Entrez le nom du local: ");
			String nom = scanner.next();

			System.out.println("Entrez le nombre de places disponibles: ");
			int nbrPlaces = scanner.nextInt();

			System.out.println("le local contient-il des ordinateurs? (o/n)");

			char contientOrdi = 'x';

			while (!(contientOrdi == 'o' || contientOrdi == 'O' || contientOrdi == 'n' || contientOrdi == 'N')) {
				contientOrdi = scanner.next().charAt(0);
			}

			ps.setString(1, nom);
			ps.setInt(2, nbrPlaces);
			ps.setBoolean(3, contientOrdi == 'o' || contientOrdi == 'O' ? true : false);

			ps.executeQuery();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void ajouterExamen() {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.ajouterExamen(?, ?, ?, ?::interval, ?) "
					+ "t(code CHAR(6), nom_examen VARCHAR(100), bloc CHARACTER(6), duree INTERVAL, est_sur_machine BOOLEAN, "
					+ "date_heure_prevue TIMESTAMP, completement_reserve BOOLEAN);");

			System.out.println("Entrez le code de l'examen: ");
			String code = scanner.next();

			System.out.println("Entrez le nom de l'examen: ");
			String nom = scanner.next();

			displayBlocList(conn);
			scanner.nextLine();
			String bloc = scanner.nextLine();

			System.out.println("Quelle est la durée (en minutes) de cet examen ?");
			String duree = scanner.next();

			System.out.println("Est-il sur machine ? (o/n)");

			char estSurMachine = 'x';

			while (!(estSurMachine == 'o' || estSurMachine == 'O' || estSurMachine == 'n' || estSurMachine == 'N')) {
				estSurMachine = scanner.next().charAt(0);
			}

			ps.setString(1, code);
			ps.setString(2, nom);
			ps.setString(3, bloc);
			ps.setString(4, duree + " minutes");
			ps.setBoolean(5, estSurMachine == 'o' || estSurMachine == 'O' ? true : false);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void encoderHeure() {
		try {

			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.encoderHeureDebutExamen(?,?);");

			System.out.println("Entrez la date au format DD/MM/YYYY: ");
			String date = scanner.next();
			String[] tDate = date.split("/");

			System.out.println("Entrez l'heure au format HH:MM");
			String heure = scanner.next();
			String[] tHeure = heure.split(":");

			LocalDateTime time = LocalDateTime.of(Integer.parseInt(tDate[2]), Integer.parseInt(tDate[1]),
					Integer.parseInt(tDate[0]), Integer.parseInt(tHeure[0]), Integer.parseInt(tHeure[1]));
			ZonedDateTime zdt = time.atZone(ZoneId.of("Europe/Paris"));
			Timestamp time2 = new Timestamp(zdt.toInstant().toEpochMilli());

			displayExamensList(conn);
			String examen = scanner.next();

			ps.setTimestamp(1, time2);
			ps.setString(2, examen);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void reserverLocal() {
		try {

			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.reserverLocalExamen(?, ?); ");

			displayLocauxList(conn);

			String local = scanner.next();

			displayExamensList(conn);
			String code = scanner.next();

			ps.setString(1, local);
			ps.setString(2, code);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void visualiserHoraireBloc() {
		try {
			PreparedStatement ps = conn
					.prepareStatement("SELECT * FROM projet.visualiserHorairePourBloc WHERE bloc = ?;");

			displayBlocList(conn);
			scanner.nextLine();
			String bloc = scanner.nextLine();

			ps.setString(1, bloc);

			ResultSet rs = ps.executeQuery();

			boolean firstLine = true;
			while (rs.next()) {
				if (firstLine) {
					System.out.println("\nHoraire - Code examen - Nom examen - nombre de locaux réservés\n");

					firstLine = false;
				}
				System.out.print("  " + rs.getTimestamp(2));
				System.out.print(" - " + rs.getString(3));
				System.out.print(" - " + rs.getString(4));
				System.out.println(" - " + rs.getInt(5));
			}
			if (firstLine)
				System.out.println("\nAucune données à afficher");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void visualiserReservationLocal() {
		try {
			PreparedStatement ps = conn
					.prepareStatement("SELECT * FROM projet.visualiserReservationsLocal WHERE nom_local = ?;");

			displayLocauxList(conn);

			String local = scanner.next();

			ps.setString(1, local);

			ResultSet rs = ps.executeQuery();

			boolean firstLine = true;
			while (rs.next()) {
				if (firstLine) {
					System.out.println("\nExamen - nom examen - bloc - début - fin  \n"); // displayed once if
					// content exists
					firstLine = false;
				}
				System.out.print("  " + rs.getString(2));
				System.out.print(" - " + rs.getString(3));
				System.out.print(" - " + rs.getString(4));
				System.out.print(" - " + rs.getTimestamp(5));
				System.out.println(" - " + rs.getTimestamp(6));
			}
			if (firstLine)
				System.out.println("\nAucune données à afficher");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void visualiserExamensNonReserves() {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.visualiserExamensPasEncoreReserves");
			ResultSet rs = ps.executeQuery();

			boolean firstLine = true;
			while (rs.next()) {
				if (firstLine) {
					System.out.println("\nCode examen - Nom examen - Bloc");
					firstLine = false;
				}
				System.out.print("  " + rs.getString(1));
				System.out.print(" - " + rs.getString(2));
				System.out.println(" - " + rs.getString(3));
			}
			if (firstLine)
				System.out.println("\nAucune données à afficher");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void visualiserExamensNonReservesParBloc() {
		try {
			PreparedStatement ps = conn
					.prepareStatement("SELECT * FROM projet.visualiserExamensPasEncoreReservesParBloc");
			ResultSet rs = ps.executeQuery();

			boolean firstLine = true;
			while (rs.next()) {
				if (firstLine) {
					System.out.println("\nCode bloc - Nombre d'examens non réservés");
					firstLine = false;
				}
				System.out.print("  " + rs.getString(1));
				System.out.println(" - " + rs.getInt(2));
			}
			if (firstLine)
				System.out.println("\nAucune données à afficher");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public String inscription(String salt) {
		String nom = null;
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT projet.inscription(?,?,?,?);");
			System.out.println("Veuillez entrer votre nom d'utilisateur: ");
			nom = scanner.next();

			System.out.println("Veuillez entrer votre email: ");
			String email = scanner.next();

			displayBlocList(conn);
			scanner.nextLine();
			String bloc = scanner.nextLine();
			System.out.println("Veuillez entrer votre mot de passe: ");
			String mdp = scanner.next();

			ps.setString(1, nom);
			ps.setString(2, email);
			ps.setString(3, hashpw(mdp, salt));
			ps.setString(4, bloc);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");

		}
		return nom;
	}

	public String connexion() {
		String nom = null;
		String nomEncode;
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT projet.connexion(?);");
			System.out.println("Veuillez entrer votre nom d'utilisateur: ");
			nomEncode = scanner.next();

			System.out.println("Veuillez entrer votre mot de passe: ");
			String mdp = scanner.next();

			ps.setString(1, nomEncode);

			ResultSet rs = ps.executeQuery();

			boolean gotName = true;
			boolean mdpOK = false;
			while (rs.next()) {
				if (rs.getString(1) == null) {
					gotName = false;
				} else
					mdpOK = checkpw(mdp, rs.getString(1));
			}
			if (gotName && mdpOK) {
				nom = nomEncode;
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
		return nom;
	}

	public void visualiserExamens() {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.visualiserExamens;");

			ResultSet rs = ps.executeQuery();

			boolean first = true;
			while (rs.next()) {
				if (first) {
					System.out.println("Code de l'examen - Nom - Bloc - Durée");
					first = false;
				}
				PGInterval interval = (PGInterval) rs.getObject(4);
				System.out.print("   " + rs.getString(1));
				System.out.print(" - " + rs.getString(2));
				System.out.print(" - " + rs.getString(3));
				System.out.println(" - " + interval.getHours() + " heures et " + interval.getMinutes() + " minutes");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void inscriptionExamen(String nom) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.inscriptionExamen(?,?);");

			displayExamensList(conn);

			String exam = scanner.next();

			ps.setString(1, nom);
			ps.setString(2, exam);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void inscriptionExamensDuBloc(String nom) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM projet.inscriptionATousLesExamens(?);");

			ps.setString(1, nom);

			ps.executeQuery();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}

	public void visualiserHoraire(String nom) {
		try {
			PreparedStatement ps = conn
					.prepareStatement("SELECT * FROM projet.voirHoraireExamen WHERE nom_utilisateur = ? ;");

			ps.setString(1, nom);

			ResultSet rs = ps.executeQuery();

			boolean firstLine = true;
			while (rs.next()) {
				if (firstLine) {
					System.out.println("\nCode examen - Nom examen - Bloc - Heure de début - Heure de fin - Locaux \n");

					firstLine = false;
				}
				System.out.print("  " + rs.getString(2));
				System.out.print(" - " + rs.getString(3));
				System.out.print(" - " + rs.getString(4));
				System.out.print(" - " + rs.getTimestamp(5));
				System.out.print(" - " + rs.getTimestamp(6));
				System.out.println(" - " + rs.getString(7));

			}
			if (firstLine)
				System.out.println("\nAucune donnés à  afficher");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("\n");
		}
	}
}
