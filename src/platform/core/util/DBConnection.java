package platform.core.util;

import java.sql.Connection; 
import java.sql.DriverManager ;
import java.sql.SQLException; 

public class DBConnection {

 private static final String URL = "jdbc:sqlserver://WS-5S64T74:1434;databaseName=CebuSAC2_db;encrypt=true;trustServerCertificate=true";


 private static final String USER = "sa" ;
 private static final String PASSWORD = "root" ;
 
 public static Connection getConnection() throws SQLException{
	 return DriverManager.getConnection(URL , USER , PASSWORD) ;
 }


}
