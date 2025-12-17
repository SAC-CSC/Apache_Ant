package platform.core.Repository;

import Entity.AirlineAllocation;
import platform.core.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AirlineAllocationRepository {

    public static List<AirlineAllocation> getEnabledEntries() {
        List<AirlineAllocation> list = new ArrayList<>();

        String query = "SELECT rec_id, airline_code, airline_name, operation, created, deleted, " +
                       "last_edit_time, message_time_stamp, operation_time, origin_date, sort_position, is_enbled " +
                       "FROM tbl_airline_allocation_dom WHERE is_enbled = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

        	while (rs.next()) {
        	    AirlineAllocation a = new AirlineAllocation();
        	    a.setRecId(rs.getLong("rec_id"));
        	    a.setAirlineCode(rs.getString("airline_code"));
        	    a.setAirlineName(rs.getString("airline_name"));
        	    a.setOperation(rs.getString("operation"));
        	    a.setCreated(rs.getTimestamp("created"));
        	    a.setDeleted(rs.getBoolean("deleted"));
        	    a.setLastEditTime(rs.getTimestamp("last_edit_time"));
        	    a.setMessageTimeStamp(rs.getTimestamp("message_time_stamp"));
        	    a.setOperationTime(rs.getTimestamp("operation_time"));
        	    a.setOriginDate(rs.getDate("origin_date"));
        	    a.setSortPosition(rs.getInt("sort_position"));
        	    a.setEnableStatus(rs.getBoolean("is_enbled")); // FIXED
        	    list.add(a);
        	}

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}
