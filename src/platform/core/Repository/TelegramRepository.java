package platform.core.Repository;
import java.sql.Connection; 
import java.sql.PreparedStatement; 
import java.sql.SQLException ;

import platform.core.util.DBConnection;

public class TelegramRepository {

	public static void insertTelegram(int telegramLength, int subsystemId, int component, long globalId,
            int plcIndex, int scannerNumber, String scannerName, String telegramVersion, String status,
            int responseLength, String BarcodeList, String responseAscii) throws SQLException {

		String sql = "INSERT INTO scanner_telegram "
				+ "(telegram_length, subsystem_id, component, global_id, plc_index, scanner_number, scanner_name, "
				+ "telegram_version, status, response_length, barcodes, response_ascii) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Now you can use this SQL with a PreparedStatement to insert the values
		    try(Connection conn = DBConnection.getConnection();
		    	PreparedStatement stmt = conn.prepareStatement(sql)){
		    	stmt.setInt(1, telegramLength);
		    	stmt.setInt(2, subsystemId);
		    	stmt.setInt(3, component);
		    	stmt.setLong(4,  globalId);
		    	stmt.setInt(5, plcIndex);
		    	stmt.setInt(6,scannerNumber);
		    	stmt.setString(6,scannerName);
		    	stmt.setString(6,telegramVersion);
		    	stmt.setString(6,status);
		    	stmt.setInt(7, responseLength);
		    	stmt.setNString(8, BarcodeList);
		    	stmt.setNString(8, responseAscii);	   
		        stmt.executeUpdate();
		            System.out.println("✔️ Data inserted successfully!");
		    } 
		    catch(SQLException e) {
		    	 System.err.println("❌ DB Insert Error: " + e.getMessage());
		    }
      }
}
