package singularity.com.br.fkngpunchclock.database;

import android.provider.BaseColumns;

/**
 * Created by Andre on 01/07/2017.
 */

public class DBContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DBContract() {}

    /* Inner class that defines the table contents */
//    public static class ClockPunch implements BaseColumns {
//        public static final String TABLE_NAME = "clock_punch";
//        public static final String _ID = "id";
//        public static final String COLUMN_NAME_DATE = "date";
//        public static final String COLUMN_NAME_TIME = "time";
//    }
    /* Inner class that defines the table contents */
    public static class ClockPunch implements BaseColumns {
        public static final String TABLE_NAME = "clock_punch";
        public static final String _ID = "id";
        public static final String COLUMN_NAME_DAY = "day";
        public static final String COLUMN_NAME_MONTH = "month";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_TIME = "time";
    }

    /* Inner class that defines the table contents */
    public static class DayBalance implements BaseColumns {
        public static final String TABLE_NAME = "day_balance";
        public static final String _ID = "id";
        public static final String COLUMN_NAME_DAY = "day";
        public static final String COLUMN_NAME_MONTH = "month";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_BALANCE = "balance";
    }


}
