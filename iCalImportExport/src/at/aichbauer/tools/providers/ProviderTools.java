package at.aichbauer.tools.providers;

import java.io.FileOutputStream;
import java.io.PrintStream;

import android.content.ContentValues;

public abstract class ProviderTools {
	private ProviderTools() {
		
	}
	
	public static void writeException(String path, Exception exc) throws Exception {
		PrintStream out = new PrintStream(new FileOutputStream(path));
		exc.printStackTrace(out);
	}
	
	public static String buildWhereAnd(String...columns) {
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<columns.length;i++ ){
			if(i>0) {
				builder.append(" AND ");
			}
			builder.append(columns[i]+" = ?");
		}
		return builder.toString();
	}
	
	public static String[] contentValuesToArray(ContentValues values, String...keys) {
		String[] columns = new String[keys.length];
		for(int i=0; i<columns.length;i++) {
			columns[i] = values.getAsString(keys[i]);
		}
		return columns;
	}
}
