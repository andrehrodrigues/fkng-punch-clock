package singularity.com.br.fkngpunchclock.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Andre on 01/07/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    // If you change the singularity.com.br.fkngpunchclock.database schema, you must increment the singularity.com.br.fkngpunchclock.database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "fkngponto.db";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_CLOCK_PUNCH);
        db.execSQL(SQL_CREATE_TABLE_DAY_BALANCE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This singularity.com.br.fkngpunchclock.database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        onDelete(db);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void onDelete(SQLiteDatabase db){
        db.execSQL(SQL_DELETE_TABLE_CLOCK_PUNCH);
        db.execSQL(SQL_DELETE_TABLE_DAY_BALANCE);
    }

    /**
     * Check if the singularity.com.br.fkngpunchclock.database exist and can be read.
     *
     * @return true if it exists and can be read, false if it doesn't
     */
    public boolean checkDataBase(Context context) {
        SQLiteDatabase checkDB = null;
        String dbPath = context.getDatabasePath(DATABASE_NAME).toString();
        try {
            checkDB = SQLiteDatabase.openDatabase(dbPath, null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
        } catch (SQLiteException e) {
            System.out.println("Banco de dados ainda não foi criado");
        }
        return checkDB != null;
    }

    //Strings contendo os códigos SQL para gerenciamento do banco de dados -------------------------
    private static final String SQL_CREATE_TABLE_CLOCK_PUNCH =
            "CREATE TABLE " + DBContract.ClockPunch.TABLE_NAME + " (" +
                    DBContract.ClockPunch._ID + " INTEGER PRIMARY KEY," +
                    DBContract.ClockPunch.COLUMN_NAME_DATE + " TEXT," +
                    DBContract.ClockPunch.COLUMN_NAME_TIME + " TEXT);" ;

    private static final String SQL_DELETE_TABLE_CLOCK_PUNCH =
            "DROP TABLE IF EXISTS " + DBContract.ClockPunch.TABLE_NAME+";";

    private static final String SQL_CREATE_TABLE_DAY_BALANCE =
            "CREATE TABLE " + DBContract.DayBalance.TABLE_NAME + " (" +
                    DBContract.DayBalance._ID + " INTEGER PRIMARY KEY," +
                    DBContract.DayBalance.COLUMN_NAME_DAY + " INTEGER," +
                    DBContract.DayBalance.COLUMN_NAME_MONTH + " INTEGER," +
                    DBContract.DayBalance.COLUMN_NAME_YEAR + " INTEGER," +
                    DBContract.DayBalance.COLUMN_NAME_BALANCE + " NUMBER);" ;

    private static final String SQL_DELETE_TABLE_DAY_BALANCE =
            "DROP TABLE IF EXISTS " + DBContract.DayBalance.TABLE_NAME+";";

}
