package at.aichbauer.ical;

public interface ICalConstants {
	public static final String HELP = "<b>iCal Import/Export</b><br>"
			+ "It is a simple tool to import iCal files into your google calendar."
			+ "<br>To successfully import iCal events please follow the given steps below:<br><br>"
			+ "  +<i>Select a calendar</i><br><small>The selected calendar will be editet</small><br>"
			+ "  +<i>Search iCal files</i> or <i>Set URL</i><br><small>Searches the SD card for iCal files or enter URL of an iCal file directly (http and ftp support)</small><br>"
			+ "  +<i>Select an iCal file</i><br>"
			+ "  +<i>Load iCal file</i><br><small>The iCal file will be parsed, if successfull a number of events should appear next to the button</small><br>"
			+ "  +<i>Insert events</i> or <i>Delete events</i><br><small>Starts the import process. When finished a status information should be displayed.</small><br>"
			+ "<br>"
			+ "If you are considering errors, please contact me via email (lukas.aichbauer@gmail.com) and provide me with an iCal file you would like to import.<br>"
			+ "<br>Thanks to iCal4j Project for the parser/interpreter<br><br>"
			+ "<i>To view this information again: menu --> help</i>";
	public static final String BUY_ME_BEER = "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=lukas%2eaichbauer%40gmx%2eat&lc=AT&item_name=Beer&amount=3%2e00&currency_code=EUR&button_subtype=services&bn=PP%2dBuyNowBF%3abtn_buynowCC_LG%2egif%3aNonHosted";
	public static final String PREFERENCE_HELP_SHOWN = "helpShown";
	public static final String PREFERENCE_LAST_URL = "lastUrl";
	public static final String PREFERENCE_LAST_USERNAME = "lastUsername";
	public static final String PREFERENCE_LAST_PASSWORD = "lastPassword";
	public static final String PREFERENCE_RATED = "rated";
	public static final String MARKET_URL = "market://details?id=at.aichbauer.ical";
}
