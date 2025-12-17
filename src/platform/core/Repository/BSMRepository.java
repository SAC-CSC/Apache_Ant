package platform.core.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import platform.core.util.DBConnection;

public class BSMRepository {
	
	  public static boolean existsInBSM(String iata) {
	        String sql = "SELECT 1 FROM tbl_IATA WHERE IATA_code = ?";
	        try (Connection conn = DBConnection.getConnection();
	             PreparedStatement stmt = conn.prepareStatement(sql)) {
	            stmt.setString(1, iata);
	            return stmt.executeQuery().next();
	        } catch (SQLException e) {
	            System.err.println("❌ DB Query Error: " + e.getMessage());
	            return false;
	        }
	    }

	    public static Optional<Integer> getDestination(String iata) {
	        String sql = "SELECT Allocation FROM tbl_IATA WHERE iata_code = ?";
	        try (Connection conn = DBConnection.getConnection();
	             PreparedStatement stmt = conn.prepareStatement(sql)) {
	            stmt.setString(1, iata);
	            ResultSet rs = stmt.executeQuery();
	            if (rs.next()) {
	                return Optional.of(rs.getInt("Allocation"));
	            }
	        } catch (SQLException e) {
	            System.err.println("❌ DB Query Error: " + e.getMessage());
	        }
	        return Optional.empty();
	    }
}
