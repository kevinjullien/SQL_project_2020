/**
 * @authors Kevin Jullien et Narjis Laraki
 */
package main;

import java.util.Scanner;
import utils.DBConnection;

public class AdminMain {

	public static void main(String[] args) {

		DBConnection connection = new DBConnection();
		
		try (Scanner scanner = new Scanner(System.in)) {
			int menuKey = 0;

			System.out.println("###############################################");
			System.out.println("#                                             #");
			System.out.println("# Bienvenue sur la plateforme administrateur! #");
			System.out.println("#                                             #");
			System.out.println("###############################################");

			do {
				System.out.println("1. Ajouter un local");
				System.out.println("2. Ajouter un examen");
				System.out.println("3. Encoder l'heure de début d'un examen");
				System.out.println("4. Réserver un local pour examen");
				System.out.println("5. Visualiser horaire des examens pour un bloc particulier");
				System.out.println("6. Visualiser toutes les réservations d'un local particulier");
				System.out.println("7. Visualiser tous les examens qui ne sont pas encore completement réservés");
				System.out.println("8. Visualiser tous les examens qui ne sont pas encore completement réservés pour chaque bloc");

				menuKey = scanner.nextInt();

				switch (menuKey) {
				case 1:
					connection.ajouterLocal();
					break;

				case 2:
					connection.ajouterExamen();
					break;

				case 3:
					connection.encoderHeure();
					break;

				case 4:
					connection.reserverLocal();
					break;

				case 5:
					connection.visualiserHoraireBloc();
					break;

				case 6:
					connection.visualiserReservationLocal();
					break;
					
				case 7:
					connection.visualiserExamensNonReserves();
					break;

				case 8:
					connection.visualiserExamensNonReservesParBloc();
					break;

				default:
					break;
				}
			} while (menuKey <= 8 && menuKey > 0);

			System.out.println("Au revoir!");
		}
	}
}
