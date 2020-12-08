/**
 * @authors Kevin Jullien et Narjis Laraki
 */
package main;

import java.util.Scanner;
import utils.DBConnection;

import static salt.BCrypt.*;

public class UserMain {
	
	public static void main(String[] args) {

		DBConnection connection = new DBConnection();

		try (Scanner scanner = new Scanner(System.in)) {
			int menuKey = 0;
			String nom = null;
			boolean estConnecte = false;

			System.out.println("############################################");
			System.out.println("#                                          #");
			System.out.println("# Bienvenue sur la plateforme utilisateur! #");
			System.out.println("#                                          #");
			System.out.println("############################################");

			do {
				System.out.println("1. Vous inscrire");
				System.out.println("2. Vous connecter");

				menuKey = scanner.nextInt();

				switch (menuKey) {
				case 1:
					nom = connection.inscription(gensalt());
					if (nom != null)
						estConnecte = true;
					break;

				case 2:
					nom = connection.connexion();
					if (nom != null)
						estConnecte = true;
					break;

				default:
					break;
				}
			} while ((menuKey < 1 || menuKey > 2) || !estConnecte);

			menuKey = 0;
			do {
				System.out.println("1. Visualiser les examens");
				System.out.println("2. S'inscrire à un examen");
				System.out.println("3. S'inscrire à tous les examens de mon bloc");
				System.out.println("4. Visualiser l'horaire de mes examens");

				menuKey = scanner.nextInt();

				switch (menuKey) {
				case 1:
					connection.visualiserExamens();
					break;

				case 2:
					connection.inscriptionExamen(nom);
					break;

				case 3:
					connection.inscriptionExamensDuBloc(nom);
					break;

				case 4:
					connection.visualiserHoraire(nom);
					break;

				default:
					break;
				}

			} while (menuKey >= 1 || menuKey <= 4);
			
			System.out.println("Au revoir!");
		}
	}
}