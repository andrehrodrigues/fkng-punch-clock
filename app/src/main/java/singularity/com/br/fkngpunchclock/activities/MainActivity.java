package singularity.com.br.fkngpunchclock.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import singularity.com.br.fkngpunchclock.R;
import singularity.com.br.fkngpunchclock.activities.adapter.PunchListAdapter;
import singularity.com.br.fkngpunchclock.activities.tasks.JobSchedulerService;
import singularity.com.br.fkngpunchclock.database.DBContract;
import singularity.com.br.fkngpunchclock.database.DBHelper;
import singularity.com.br.fkngpunchclock.entities.ClockPunch;
import singularity.com.br.fkngpunchclock.entities.DayBalance;

public class MainActivity extends AppCompatActivity {

    DBHelper mDbHelper;
    SQLiteDatabase db;
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    //This page layout components
    TextClock punchTime;
    TextView punchDate;
    ImageView inOutImage;
    ListView batidasLV;
    FloatingActionButton addButton;

    //Components from the included General Status Layout
    TextView dayBalance;
    TextView monthBalance;


    PunchListAdapter adapter;
    ArrayList<ClockPunch> clockPunches;
    Double dayTotalBalance = 0.0;
    Double monthBalanceDayBefore = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new DBHelper(this);
        if(!mDbHelper.checkDataBase(getApplicationContext())){
            db = mDbHelper.getWritableDatabase();
            mDbHelper.onCreate(db);
        }else{
            db = mDbHelper.getWritableDatabase();
        }

        db = mDbHelper.getWritableDatabase();
        mDbHelper.onUpgrade(db,1,1);

//        scheduleAutomaticDayBalanceCalculationJob();

        View includedLayout = findViewById(R.id.generalStatusLay);
        dayBalance = (TextView)includedLayout.findViewById(R.id.gsl_day_total_value);
        monthBalance = (TextView)includedLayout.findViewById(R.id.gsl_month_balance_value);

        punchDate = (TextView) findViewById(R.id.punchDate);
        punchTime = (TextClock) findViewById(R.id.punchTime);
        inOutImage = (ImageView) findViewById(R.id.inOutImage);
        batidasLV = (ListView) findViewById(R.id.batidasLV);
        addButton = (FloatingActionButton) findViewById(R.id.addButton);
        clockPunches = new ArrayList<>();

        //Get current date.
        Date date = new Date();

        //Set the punchDate to the current date.
        punchDate.setText(dateFormat.format(date).toString());

        //Set the view list (and respective adapter) to keep the ongoing clock punchs.
        adapter = new PunchListAdapter(MainActivity.this, R.layout.ponto_item_list_view, clockPunches);
        batidasLV.setAdapter(adapter);
        refreshListView(dateFormat.format(date).toString());

        //Floating button for adding.
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertPunchDB(new ClockPunch(punchDate.getText().toString(), punchTime.getText().toString()));
            }
        });

        getMonthBalance( String.valueOf(Calendar.getInstance().get(Calendar.MONTH)+ 1) , String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) );

    }//End of OnCreate()

    public void removeItemOnClickHandler(View v) {
        ClockPunch itemToRemove = (ClockPunch) v.getTag();
        deletePunchDB(itemToRemove);
    }

    public Long getClockPunchesBalance(ArrayList<ClockPunch> clockPunches){
        Long dayTotal = 0L;
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        if(clockPunches.size() > 1) {
            for (int aux = 0; aux < clockPunches.size() && aux+1 < clockPunches.size(); aux += 2) {
                Date date1 = null;
                Date date2 = null;
                try {
                    date1 = format.parse(clockPunches.get(aux).getTime());
                    date2 = format.parse(clockPunches.get(aux + 1).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                dayTotal +=  date2.getTime() - date1.getTime();
            }
        }

        return Long.valueOf(TimeUnit.MILLISECONDS.toHours(dayTotal));
    }

    public String millisecondsToTimeString(Long millis){
        return  String.format("%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
    }

    public void refreshListView(String date) {
        TaskListener listener = new TaskListener() {
            @Override
            public void onFinished() {}

            @Override
            public void onFinished(ArrayList result) {
                if (result.size() > 0) {
                    clockPunches = result;
                    adapter.clear();
                    adapter.addAll(result);
                    adapter.notifyDataSetChanged();

                    if (clockPunches != null && clockPunches.size() % 2 == 1) {
                        inOutImage.setImageResource(R.drawable.ic_flight_takeoff_black_24dp);
                    }else{
                        inOutImage.setImageResource(R.drawable.ic_flight_land_black_24dp);
                    }

                    Long sum = getClockPunchesBalance(result);

                    dayTotalBalance = Long.valueOf(TimeUnit.MILLISECONDS.toHours(sum)).doubleValue();
                    dayBalance.setText(millisecondsToTimeString(sum));

                } else {
                    showToastMessage("Nenhum ponto encontrado");
                }
            }
        };
        new GetClockPunchesTask(listener).execute(date);

    }

    public void showToastMessage(String toastString) {
        Toast.makeText(this, toastString, Toast.LENGTH_LONG).show();
    }

    public void insertPunchDB(ClockPunch pt) {
        ContentValues val = new ContentValues();
        val.put(DBContract.ClockPunch.COLUMN_NAME_DAY, pt.getDay());
        val.put(DBContract.ClockPunch.COLUMN_NAME_MONTH, pt.getMonth());
        val.put(DBContract.ClockPunch.COLUMN_NAME_YEAR, pt.getYear());
        val.put(DBContract.ClockPunch.COLUMN_NAME_TIME, pt.getTime());
        TaskListener listener = new TaskListener() {
            @Override
            public void onFinished() {
                refreshListView(dateFormat.format(new Date()));
            }

            @Override
            public void onFinished(ArrayList<ClockPunch> itemList) {

            }
        };
        new InsertClockPunchTask(listener).execute(val);
    }

    public void deletePunchDB(ClockPunch pt) {
        ContentValues val = new ContentValues();
        val.put(DBContract.ClockPunch._ID, pt.getId());
        val.put(DBContract.ClockPunch.COLUMN_NAME_DAY, pt.getDay());
        val.put(DBContract.ClockPunch.COLUMN_NAME_MONTH, pt.getMonth());
        val.put(DBContract.ClockPunch.COLUMN_NAME_YEAR, pt.getYear());
        val.put(DBContract.ClockPunch.COLUMN_NAME_TIME, pt.getTime());
        TaskListener listener = new TaskListener() {
            @Override
            public void onFinished() {
                refreshListView(dateFormat.format(new Date()));
            }

            @Override
            public void onFinished(ArrayList<ClockPunch> itemList) { }
        };
        new RemoveClockPunchTask(listener).execute(val);
    }

    public void getMonthBalance(String month, String year) {

        TaskListener listener = new TaskListener() {
            @Override
            public void onFinished() {}

            @Override
            public void onFinished(ArrayList result) {
                if (result.size() > 0) {
                    ArrayList<ClockPunch> cps = (ArrayList<ClockPunch>) result;

                    monthBalance.setText(millisecondsToTimeString( getClockPunchesBalance(cps) ));

                    ArrayList<ClockPunch> cpsToRemove = new ArrayList<>();
                    for (ClockPunch cp: cps) {
                        if( cp.getDay() == (Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                            && cp.getMonth() == (Calendar.getInstance().get(Calendar.MONTH)+ 1)
                            && cp.getYear() == (Calendar.getInstance().get(Calendar.YEAR)) ){

                            cpsToRemove.add(cp);
                        }
                    }

                    cps.removeAll(cpsToRemove);

                    monthBalanceDayBefore = Long.valueOf( getClockPunchesBalance(cps) ).doubleValue();

                } else {
                    showToastMessage("Nenhum ponto encontrado");
                }
            }
        };
        new GetClockPunchesTask(listener).execute(month+"/"+year);
    }

    public void scheduleAutomaticDayBalanceCalculationJob(){
        JobScheduler mJobScheduler = (JobScheduler) getApplicationContext().getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( 1,
                new ComponentName( getApplicationContext(),
                        JobSchedulerService.class.getName() ) );

        int runEveryNumberHours = 4;
        builder.setPeriodic( 1000 * 60 * 60 * runEveryNumberHours );
        builder.setPersisted(true);

        if( mJobScheduler.schedule( builder.build() ) <= 0 ) {
            showToastMessage("Shit went wrong mofo.");
        }
    }

    /**
     * Interface criada para a função de callback que será executada ao fim da execução das tarefas
     * de query de dados assíncronas.
     */
    public interface TaskListener {

        public void onFinished();

        public void onFinished(ArrayList<ClockPunch> itemList);
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
            // Define a projection that specifies which columns from the singularity.com.br.fkngpunchclock.database
            // you will actually use after this query.
            String[] projection = {
                    DBContract.ClockPunch._ID,
                    DBContract.ClockPunch.COLUMN_NAME_DAY,
                    DBContract.ClockPunch.COLUMN_NAME_MONTH,
                    DBContract.ClockPunch.COLUMN_NAME_YEAR,
                    DBContract.ClockPunch.COLUMN_NAME_TIME
            };

            String parts[] = date[0].split("/");
            for (int aux = 0; aux < parts.length; aux++) {
                parts[aux] = parts[aux].replaceFirst("^0+(?!$)", "");
            }

            String selection;
            if( parts.length == 2){
                selection = DBContract.ClockPunch.COLUMN_NAME_MONTH + " = " + parts[0]
                        + " AND "+ DBContract.ClockPunch.COLUMN_NAME_YEAR + " = "+parts[1];
            }else{
                selection = DBContract.ClockPunch.COLUMN_NAME_DAY + " = " + parts[0]
                        + " AND " +DBContract.ClockPunch.COLUMN_NAME_MONTH + " = " + parts[1]
                        + " AND " + DBContract.ClockPunch.COLUMN_NAME_YEAR + " = " + parts[2];
            }

            Cursor cursor = db.query(
                    DBContract.ClockPunch.TABLE_NAME, projection, selection, null, null, null, null);

            ArrayList<ClockPunch> clockPunches = new ArrayList<>();

            while (cursor.moveToNext()) {
                ClockPunch pt = new ClockPunch();
                pt.setId(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch._ID)));
                pt.setDay(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch.COLUMN_NAME_DAY)));
                pt.setMonth(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch.COLUMN_NAME_MONTH)));
                pt.setYear(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBContract.ClockPunch.COLUMN_NAME_YEAR)));
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

    private class InsertClockPunchTask extends AsyncTask<ContentValues, Integer, Long> {

        // This is the reference to the associated listener
        private final TaskListener taskListener;

        public InsertClockPunchTask(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        protected Long doInBackground(ContentValues... pontos) {

            long newRowId = 0;
            for (ContentValues pt : pontos) {
                newRowId = db.insert(DBContract.ClockPunch.TABLE_NAME, null, pt);
            }
            return newRowId;
        }

        protected void onPostExecute(Long vazio) {
            this.taskListener.onFinished();
        }
    }

    private class RemoveClockPunchTask extends AsyncTask<ContentValues, Integer, String> {

        // This is the reference to the associated listener
        private final TaskListener taskListener;

        public RemoveClockPunchTask(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        protected String doInBackground(ContentValues... pontos) {
            long newRowId = 0;
            String reg="";
            for (ContentValues pt : pontos) {
                reg+=pt.get(DBContract.ClockPunch.COLUMN_NAME_DAY)+"/"
                        + pt.get(DBContract.ClockPunch.COLUMN_NAME_MONTH)+"/"
                        + pt.get(DBContract.ClockPunch.COLUMN_NAME_YEAR)+" - "
                        + pt.get(DBContract.ClockPunch.COLUMN_NAME_TIME)+" " +
                        "";
                newRowId = db.delete(DBContract.ClockPunch.TABLE_NAME, DBContract.ClockPunch._ID + " = " + pt.get("id"), null);
            }
            return reg;
        }

        protected void onPostExecute(String clkPnch) {
            this.taskListener.onFinished();
            showToastMessage("Clock punch removed.");
        }
    }

//    private class CalculateMonthBalanceTask extends AsyncTask<ContentValues, Integer, Double> {
//
//        // This is the reference to the associated listener
//        private final TaskListener taskListener;
//
//        public CalculateMonthBalanceTask(TaskListener taskListener) {
//            this.taskListener = taskListener;
//        }
//
//        public CalculateMonthBalanceTask() {
//            this.taskListener = null;
//        }
//
//        protected Double doInBackground(ContentValues... months) {
//            // Define a projection that specifies which columns from the singularity.com.br.fkngpunchclock.database
//            // you will actually use after this query.
//            String[] projection = {
//                    DBContract.DayBalance._ID,
//                    DBContract.DayBalance.COLUMN_NAME_DAY,
//                    DBContract.DayBalance.COLUMN_NAME_MONTH,
//                    DBContract.DayBalance.COLUMN_NAME_YEAR,
//                    DBContract.DayBalance.COLUMN_NAME_BALANCE
//            };
//
//            String selection = "SELECT * FROM "
//
//            String campoMes = "(";
//            String campoAno = "(";
//            for (int aux = 0; aux < months.length; aux++) {
//                campoMes += "'" + months[aux].get(DBContract.DayBalance.COLUMN_NAME_MONTH) + "'";
//                campoAno += "'" + months[aux].get(DBContract.DayBalance.COLUMN_NAME_YEAR) + "'";
//                if (aux < months.length - 1) {
//                    campoMes += ',';
//                    campoAno += ',';
//                }
//            }
//            campoMes += ")";
//            campoAno += ")";
//
////            String selection = DBContract.DayBalance.COLUMN_NAME_MONTH+ " in " + campoMes +
////                    " and "+ DBContract.DayBalance.COLUMN_NAME_YEAR+" in "+ campoAno;
////
////            Cursor cursor = db.query(
////                    DBContract.DayBalance.TABLE_NAME, projection, selection, null, null, null, null);
//
//            Cursor cursor = db.rawQuery( );
//
//            ArrayList<DayBalance> balances = new ArrayList<>();
//            Double balance = 0.0;
//
//            while (cursor.moveToNext()) {
//                DayBalance db = new DayBalance();
//                db.setId(cursor.getInt(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance._ID)));
//                db.setDay(cursor.getInt(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance.COLUMN_NAME_DAY)));
//                db.setMonth(cursor.getInt(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance.COLUMN_NAME_MONTH)));
//                db.setYear(cursor.getInt(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance.COLUMN_NAME_YEAR)));
//                db.setBalance(cursor.getDouble(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance.COLUMN_NAME_BALANCE)));
//                balances.add(db);
//                balance += cursor.getDouble(
//                        cursor.getColumnIndexOrThrow(DBContract.DayBalance.COLUMN_NAME_BALANCE));
//            }
//            cursor.close();
//
//            return balance;
//        }
//
//        protected void onPostExecute(Double balance) {
//            Double total = balance + dayTotalBalance;
//            monthBalance.setText(total.toString());
//        }
//    }




}//End of Class
