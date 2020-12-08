/**
 * @authors Kevin Jullien et Narjis Laraki
 */
package utils;

import java.sql.*;

public interface Utils {

	public static void displayBlocList(Connection conn) {
		System.out.println("Voici la liste des blocs existants:");

		try {
			Statement s = conn.createStatement();
			try (ResultSet rs = s.executeQuery("SELECT code FROM projet.blocs;")) {
				while (rs.next()) {
					System.out.println("  " + rs.getString(1));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Veuillez encoder le bloc: ");
	}

	public static void displayLocauxList(Connection conn) {
		System.out.println("Voici la liste des locaux existants");

		try {
			Statement s = conn.createStatement();
			try (ResultSet rs = s.executeQuery("SELECT nom_local FROM projet.locaux ORDER BY nom_local;")) {
				while (rs.next()) {
					System.out.println("  " + rs.getString(1));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Veuillez encoder le local: ");
	}

	public static void displayExamensList(Connection conn) {
		System.out.println("Voici la liste des examens existants");

		try {
			Statement s = conn.createStatement();
			try (ResultSet rs = s.executeQuery("SELECT code, nom_examen FROM projet.examens ORDER BY code;")) {
				while (rs.next()) {
					System.out.println("  " + rs.getString(1) + " " + rs.getString(2));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Veuillez encoder l'examen: ");
	}

}
