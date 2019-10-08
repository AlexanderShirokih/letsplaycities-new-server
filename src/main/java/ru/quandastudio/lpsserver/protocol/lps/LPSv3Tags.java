package ru.quandastudio.lpsserver.protocol.lps;

public class LPSv3Tags {

	// Magic code
	public static final char LPS_SERVER_VALID = 0xBEEB;
	public static final char LPS_CLIENT_VALID = 0xEBBE;

	// Client Tags

	// Login tags
	public static final byte ACTION_LOGIN = 2;
	public static final byte CLIENT_VERSION = 3;
	public static final byte CLIENT_BUILD = 4;
	public static final byte UUID = 5;
	public static final byte LOGIN = 6;
	public static final byte AVATAR_PART0 = 7;
	public static final byte AVATAR_PART1 = 8;
	public static final byte CAN_REC_MSG = 9;
	public static final byte UID = 10;// v4
	public static final byte ACC_HASH = 11;// v4
	public static final byte SN = 12;// v4
	public static final byte SN_UID = 13;// v4
	public static final byte ACCESS_TOKEN = 91;// v4
	public static final byte ALLOW_SEND_UID = 92;// v4
	public static final byte FIREBASE_TOKEN = 111;// v4, since LPS v2.0

	// Play Tags
	public static final byte ACTION_PLAY = 14;
	public static final byte E_RANDOM_PAIR_MODE = 0;
	public static final byte E_FRIEND_MODE = 1;
	public static final byte OPP_UID = 16;

	// Message Tags
	public static final byte ACTION_MSG = 18;

	// Word Tags
	public static final byte ACTION_WORD = 20;

	// Ban Tags
	public static final byte ACTION_BAN = 22;
	public static final byte E_USER_BAN = 0;
	public static final byte E_REPORT = 1;
	public static final byte UBAN_REASON = 23;
	public static final byte ROOM_CONTENT = 24;

	// Leave Tags
	public static final byte ACTION_LEAVE = 26;

	// Friend Tags
	public static final byte ACTION_FRIEND = 28;
	public static final byte E_SEND_REQUEST = 1;
	public static final byte E_ACCEPT_REQUSET = 2;
	public static final byte E_DENY_REQUSET = 3;
	public static final byte E_DELETE_REQUEST = 4;
	public static final byte FRIEND_UID = 30;

	// Query friend info Tags
	public static final byte ACTION_QUERY_FRIEND_INFO = 32;
	public static final byte F_REQ_UUID = 33;

	// Friend mode request Tags
	public static final byte ACTION_FM_REQ_RESULT = 36;
	public static final byte SENDER_UID = 37;// LPSv4

	// Query banlist Tags
	public static final byte ACTION_QUERY_BANLIST = 106;
	public static final byte ACTION_QUERY_BANLIST_RES = 107;
	public static final byte E_QUERY_LIST = 1;

	// Firebase request Tags
	public static final byte ACTION_FIREBASE_TOKEN = 110;

	// Server Tags

	// Login tags
	public static final byte ACTION_LOGIN_RESULT = 38;
	public static final byte NEWER_BUILD = 39;
	public static final byte BAN_REASON = 40;
	public static final byte CONNECTION_ERROR = 41;
	public static final byte S_UID = 42;
	public static final byte S_ACC_HASH = 43;
	public static final byte S_API_VERSION = 100;

	// Play Tags
	public static final byte ACTION_JOIN = 44;
	public static final byte BANNED_BY_OPP = 45;
	public static final byte S_CAN_REC_MSG = 46;
	public static final byte S_OPP_UUID = 47;
	public static final byte S_AVATAR_PART0 = 48;
	public static final byte S_AVATAR_PART1 = 49;
	public static final byte OPP_LOGIN = 50;
	public static final byte OPP_CLIENT_VERSION = 51;
	public static final byte OPP_CLIENT_BUILD = 52;
	public static final byte S_OPP_UID = 53;// v4
	public static final byte S_OPP_SN = 54;// v4
	public static final byte S_OPP_SNUID = 55;// v4
	public static final byte OPP_IS_FRIEND = 104;
	public static final byte S_BANNED_BY_OPP = 105;

	// Word Tags
	public static final byte S_ACTION_WORD = 58;
	public static final byte WORD = 59;

	// Message Tags
	public static final byte S_ACTION_MSG = 54;
	public static final byte MSG_OWNER = 55;

	// Friend Tags
	public static final byte ACTION_FRIEND_REQUEST = 62;
	public static final byte E_NEW_REQUEST = 0;
	public static final byte E_FRIEND_SAYS_YES = 1;
	public static final byte E_FRIEND_SAYS_NO = 2;

	// Ban Tags
	public static final byte ACTION_BANNED = 64;
	public static final byte S_BAN_REASON = 65;

	// Disconnect Tags
	public static final byte S_ACTION_LEAVE = 68;

	// Query friend info result Tags
	public static final byte ACTION_QUERY_FRIEND_RES = 70;
	public static final byte F_QUERY_USER_ACCEPT = 71;// v4
	public static final byte F_QUERY_USER_IDS = 72;// v4
	public static final byte F_QUERY_NAMES = 73;// v4

	// Sync Tags
	public static final byte ACTION_SYNC = 74;

	// Timeout Tags
	public static final byte ACTION_TIMEOUT = 76;

	// Friend Mode Request Tags
	public static final byte ACTION_FRIEND_MODE_REQ = 78;
	public static final byte FRIEND_MODE_REQ_LOGIN = 79;
	public static final byte FRIEND_MODE_REQ_UID = 81;

	public static final byte E_NEW_FM_REQUEST = 0;
	public static final byte E_FM_RESULT_BUSY = 1;
	public static final byte E_FM_RESULT_OFFLINE = 2;
	public static final byte E_FM_RESULT_NOT_FRIEND = 3;
	public static final byte E_FM_RESULT_DENIED = 4;

	// Action spec Tags
	public static final byte ACTION_SPEC = 120;

	// Action Firebase update token
	public static final byte ACTION_REQUEST_FIREBASE = 124;

}