// Base                 : Conveyor Sortaion Controller
// Class                : Log Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version

package platform.core.log;

public class Log {

	public void debug(String prefix , String message) {
		System.out.println(prefix+"[DEBUG]"+message) ;	
	}
	public void info(String prefix , String message) {
		System.out.println(prefix+"[INFO]"+message) ;	
	}
	public void error(String prefix , String message) {
		System.err.println(prefix+"[ERROR]"+message) ;	
	}
	public void error(String prefix , String message,Throwable t) {
		System.err.println(prefix+"[ERROR]"+message) ;	
		t.printStackTrace();
	}
	public void warn(String logPrefix, String string) {
		// TODO Auto-generated method stub
		
	}
}
