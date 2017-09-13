package singularity.com.br.fkngpunchclock.activities.tasks;

/**
 * Created by Andre on 01/07/2017.
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import singularity.com.br.fkngpunchclock.database.DBContract;
import singularity.com.br.fkngpunchclock.database.DBHelper;
import singularity.com.br.fkngpunchclock.entities.ClockPunch;

/**
 * Created by ahrodrigues1 on 20/06/2017.
 */

public class JobSchedulerService extends JobService {

    DBHelper mDbHelper = new DBHelper(getApplicationContext());
    SQLiteDatabase db;

    @Override
    public boolean onStartJob(JobParameters params) {

        System.out.println("BUNDA");
        Toast.makeText( getApplicationContext(),
                "JobService task running", Toast.LENGTH_SHORT )
                .show();

        db = mDbHelper.getWritableDatabase();
        insertDayBalanceDB(new Date().toString());

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }



    public interface TaskListener {

        public void onFinished(ArrayList<ClockPunch> itemList);
    }

    public void insertDayBalanceDB(final String dt){

        TaskListener listener = new TaskListener() {

            @Override
            public void onFinished(ArrayList<ClockPunch> result) {
                if (result.size() > 0) {

                    Double dayTotal = 0.0;
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm");
                    if(result.size() > 1) {
                        for (int aux = 0; aux < result.size() && aux+1 < result.size(); aux += 2) {
                            Date date1 = null;
                            Date date2 = null;
                            try {
                                date1 = format.parse(result.get(aux).getTime());
                                date2 = format.parse(result.get(aux + 1).getTime());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            dayTotal +=  date2.getTime() - date1.getTime();
                        }
                    }

                    String[] parts = dt.split("/");

                    ContentValues val = new ContentValues();
                    val.put(DBContract.DayBalance.COLUMN_NAME_DAY, parts[0]);
                    val.put(DBContract.DayBalance.COLUMN_NAME_MONTH, parts[1]);
                    val.put(DBContract.DayBalance.COLUMN_NAME_YEAR, parts[2]);
                    val.put(DBContract.DayBalance.COLUMN_NAME_BALANCE, (dayTotal)/3600000);

                    TaskListener listenerInsert = new TaskListener() {

                        @Override
                        public void onFinished(ArrayList<ClockPunch> result) {
                            jobFinished( null, false );
                        }
                    };
                    new InsertDayBalanceTask(listenerInsert).execute(val);
                }
            }
        };
        new GetClockPunchesTask(listener).execute(dt);

    }

    private class InsertDayBalanceTask extends AsyncTask<ContentValues, Integer, Long> {

        // This is the reference to the associated listener
        private final TaskListener taskListener;

        public InsertDayBalanceTask(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        protected Long doInBackground(ContentValues... dayBalance) {

            long newRowId = 0;
            for (ContentValues dbal : dayBalance) {
                newRowId = db.insert(DBContract.DayBalance.TABLE_NAME, null, dbal);
            }
            return newRowId;
        }

        protected void onPostExecute(Long vazio) {
            this.taskListener.onFinished(null);
        }
    }

    private class GetClockPunchesTask extends AsyncTask<String, Void, ArrayList<ClockPunch>> {

        // This is the reference to the associated listener
        private final TaskListener taskListener;

        GetClockPunchesTask(TaskListener listener) {
            // The listener reference is passed in through the constructor
            this.taskListener = listener;
        }

        @Override
        protected ArrayList<ClockPunch> doInBackground(String... date) {

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    DBContract.ClockPunch._ID,
                    DBContract.ClockPunch.COLUMN_NAME_DAY,
                    DBContract.ClockPunch.COLUMN_NAME_TIME
            };

            String campo = "(";
            for (int aux = 0; aux < date.length; aux++) {
                campo += "'" + date[aux] + "'";
                if (aux < date.length - 1) {
                    campo += ',';
                }
            }
            campo += ")";

            String selection = DBContract.ClockPunch.COLUMN_NAME_DAY + " in " + campo;

            Cursor cursor = db.query(
                    DBContract.ClockPunch.TABLE_NAME, projection, selection, null, null, null, null);

            ArrayList<ClockPunch> clockPunches = new ArrayList<>();

            while (cursor.moveToNext()) {
                ClockPunch pt = new ClockPunch();
                pt.setId(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch._ID)));
                pt.setDate(cursor.getString(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch.COLUMN_NAME_DAY)));
                pt.setTime(cursor.getString(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch.COLUMN_NAME_TIME)));
                clockPunches.add(pt);
            }
            cursor.close();

            return clockPunches;
        }

        @Override
        protected void onPostExecute(ArrayList<ClockPunch> result) {
            if (result.size() > 0) {
                this.taskListener.onFinished(result);
            }
        }
    }



}
