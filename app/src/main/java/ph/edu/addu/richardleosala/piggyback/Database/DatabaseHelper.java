package ph.edu.addu.richardleosala.piggyback.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME_MESSAGES = "DIRECTORY";

    public static final String T_DNN_NUM = "numbers";
    public static final String COL_DNN_ID = "num_id";
    public static final String COL_DNN_NUM = "num";

    public static final String T_DNM_TEXT = "msgs";
    public static final String COL_DNM_ID = "msg_id";
    public static final String COL_DNM_TEXT = "text";
    public static final String COL_DNN_ID_FK = "num_id";
    public static final String COL_DNN_SENDER = "sender";

    public static final String STORED_MSGS = "strMsgs";
    public static final String STORED_MSGS_ID = "STR_ID";
    public static final String STORED_MSGS_MSGS = "str_msg";
    public static final String STORED_MSGS_SENDER = "str_sender"; //sender®
    public static final String STORED_MSGS_RECEIVER = "str_receiver"; //recipient
    public static final String STORED_MSGS_SENTTO = "str_recs"; //receivers
    public DatabaseHelper(Context context) {
        super(context, DB_NAME_MESSAGES, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + T_DNN_NUM + " (num_id INTEGER PRIMARY KEY AUTOINCREMENT, " + " num VARCHAR(255)   )";
        db.execSQL(createTable);
        createTable = "CREATE TABLE "+T_DNM_TEXT + " (msg_id INTEGER PRIMARY KEY AUTOINCREMENT," + "text VARCHAR(1000) "+", num_id INT"+", sender INT)";
        db.execSQL(createTable);
        createTable = "CREATE TABLE "+STORED_MSGS + "("+STORED_MSGS_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+STORED_MSGS_MSGS+" VARCHAR(1500),"+STORED_MSGS_SENDER+" VARCHAR(255), "+STORED_MSGS_RECEIVER+" VARCHAR(255), "+ STORED_MSGS_SENTTO +" VARCHAR(1500));";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_DNN_NUM );
        db.execSQL("DROP TABLE IF EXISTS " + T_DNM_TEXT );
        db.execSQL("DROP TABLE IF EXISTS " + STORED_MSGS );
    }

    public boolean addToNum (String num){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_DNN_NUM, num);
        long res = db.insert(T_DNN_NUM, null, contentValues);
        if(res == -1)
            return false;
        else
            return true;
    }

    public boolean addToTEXT (String msg, String num){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_DNM_TEXT, msg);
        contentValues.put(COL_DNN_ID_FK, num);
        contentValues.put(COL_DNN_SENDER, 1);
        long res = db.insert(T_DNM_TEXT, null, contentValues);
        if(res == -1)
            return false;
        else
            return true;
    }

    public boolean recMsgs(String msg, String num){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        long res = db.insert(T_DNM_TEXT, null, contentValues);
        contentValues.put(COL_DNM_TEXT, msg);
        contentValues.put(COL_DNN_ID_FK, num);
        contentValues.put(COL_DNN_SENDER, 2);
        if(res == -1)
            return false;
        else
            return true;
    }

    public Cursor checkNum(String num){
        SQLiteDatabase db = getWritableDatabase();
        String q = "SELECT * FROM " + T_DNM_TEXT +" WHERE num_id = " + num;
        Cursor data = db.rawQuery(q, null);
        return data;
    }

    public Cursor getAllData(){
        SQLiteDatabase db = getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM "+ T_DNN_NUM, null);
        return data;
    }

    public boolean storeMsgs(String sender, String msg, String receiver, String sentTo){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        long res = db.insert(STORED_MSGS, null, contentValues);
        contentValues.put(STORED_MSGS_SENDER, sender);
        contentValues.put(STORED_MSGS_RECEIVER, receiver);
        contentValues.put(STORED_MSGS_MSGS, msg);
        contentValues.put(STORED_MSGS_SENTTO, sentTo );
        return false;
    }
}
