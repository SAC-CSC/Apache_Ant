/**
 * 
 */
/**
 * 
 */
	module CSC_SAC {
	    requires java.sql;

	    // Allow your module to access classes from unnamed modules (like NanoHTTPD)
	    requires static java.desktop;

	    opens rfc;

	    // Tell the module system: I want to read unnamed modules
	    // This part *cannot be written* here; it must be passed as a compiler/jvm argument:
	    // --add-reads CSC_SAC=ALL-UNNAMED
	}
