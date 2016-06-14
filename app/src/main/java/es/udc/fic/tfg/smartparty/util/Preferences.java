package es.udc.fic.tfg.smartparty.util;

/**
 * Created by Rubén Montero Vázquez on 16/12/2015
 * This file is part of the degree's final project: Smart Party.
 * University of A Coruña (2015-2016)
 */

public class Preferences {
   public static final String SHARED_PREFERENCES_NAME = "SmartPartyPrefs";
   public static final String PREFERENCES_THEME_COLOR_RED = "ThemeColorRed";
   public static final String PREFERENCES_THEME_COLOR_GREEN = "ThemeColorGreen";
   public static final String PREFERENCES_THEME_COLOR_BLUE = "ThemeColorBlue";
   // From 0 to 180
   public static final int PREFERENCES_RED_DEFAULT = 147;
   public static final int PREFERENCES_GREEN_DEFAULT = 3;
   public static final int PREFERENCES_BLUE_DEFAULT = 137;

   public static final String PREFERENCES_COMPLEX_USERNAME = "ComplexUsername";
   public static final String PREFERENCES_STATUS = "Status";
   public static final String PREFERENCES_PHOTO_DATE = "PhotoDate";
   public static final String PREFERENCES_PHOTO_NAV = "PhotoNav";
   public static final String PREFERENCES_NOTIFY_NEW_USER = "NotifyNewUsers";
   public static final boolean PREFERENCES_NOTIFY_NEW_USER_DEFAULT = true;
   public static final String PREFERENCES_NOTIFY_MISSED_CALL = "NotifyMissedCalls";
   public static final boolean PREFERENCES_NOTIFY_MISSED_CALL_DEFAULT = true;
   public static final String PREFERENCES_EXHAUSTIVE_SCAN = "ExhaustiveScan";
   public static final boolean PREFERENCES_EXHAUSTIVE_SCAN_DEFAULT = false;
   public static final String PREFERENCES_LISTS = "Lists";
   public static final String PREFERENCES_LIST_EVERYONE = "ListEveryone";
   public static final boolean PREFERENCES_DEFAULT_EVERYONE = true;
   public static final String PREFERENCES_CALLS = "CallsRegister";
   public static final String PREFERENCES_RECYCLER_SIZE = "RecyclerSize";
   public static final int PREFERENCES_RECYCLER_SIZE_DEFAULT = 1;
   public static final String PREFERENCES_LOGS = "Logs";
   public static final String PREFERENCES_SERVICE_STATUS = "ServiceStatus";
   public static final String PREFERENCES_FIRST_START = "FirstStart";
   public static final String PREFERENCES_NEED_TO_EXPLAIN_LISTS = "NeedToExplainLists";

   public static final String INTENT_LIST_NAME = "ListName";
   public static final String INTENT_RECEIVE_CALL = "ReceiveCall";
   public static final String INTENT_ADDRESS = "Address";
   public static final String INTENT_SERVER_PORT = "ServerPort";
   public static final String INTENT_REMOTE_RTP_PORT = "RemoteRtpPort";
   public static final String INTENT_PEER_NAME = "PeerName";

   public static final int INCOMING_CALL_VIBRATE_ON = 700;
   public static final int INCOMING_CALL_VIBRATE_OFF = 2000;

   public static final int MAX_REGISTER_CALLS = 100;

   public static final int SERVER_PORT = 60826;
}
