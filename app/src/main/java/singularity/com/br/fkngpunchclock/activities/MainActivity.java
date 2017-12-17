package singularity.com.br.fkngpunchclock.activities;

import android.app.DatePickerDialog;
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
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.DatePicker;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import singularity.com.br.fkngpunchclock.R;
import singularity.com.br.fkngpunchclock.activities.adapter.PunchListAdapter;
import singularity.com.br.fkngpunchclock.activities.tasks.JobSchedulerService;
import singularity.com.br.fkngpunchclock.database.DBContract;
import singularity.com.br.fkngpunchclock.database.DBHelper;
import singularity.com.br.fkngpunchclock.entities.ClockPunch;

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
    Long dayTotalBalance = 0L;
    Long monthBalanceDayBefore = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new DBHelper(this);

        mDbHelper.checkDataBase(getApplicationContext());

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
        punchDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                DatePickerDialog datePicker = setDatePicker();
                datePicker.show();
            }
        });

        //Set the view list (and respective adapter) to keep the ongoing clock punchs.
        adapter = new PunchListAdapter(MainActivity.this, R.layout.ponto_item_list_view, clockPunches);
        batidasLV.setAdapter(adapter);

        //Load all current day CPs and set the list view.
        refreshListView(dateFormat.format(date).toString());

        //Floating button for adding.
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertPunchDB( new ClockPunch(punchDate.getText().toString(), punchTime.getText().toString(), punchTime.is24HourModeEnabled()) );
            }
        });

        getMonthBalance( String.valueOf(Calendar.getInstance().get(Calendar.MONTH)+ 1) , String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) );

    }//End of OnCreate()

    private DatePickerDialog setDatePicker() {

        Calendar newCalendar = Calendar.getInstance();
        return new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                String selectedDate = dateFormat.format(newDate.getTime());
                punchDate.setText(selectedDate);
                refreshListView(selectedDate);
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    public void removeItemOnClickHandler(View v) {
        ClockPunch itemToRemove = (ClockPunch) v.getTag();
        deletePunchDB(itemToRemove);
    }

    //Calculate the difference in time between pairs of CPs in the list order.
    public Long getClockPunchesBalance(ArrayList<ClockPunch> clockPunches){
        Long dayTotal = 0L;
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        if(clockPunches.size() > 1) {
            for (int aux = 0; aux+1 < clockPunches.size(); aux += 2) {
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

        return dayTotal;
    }

    //Convert milliseconds to time format.
    public String millisecondsToTimeString(Long millis){
        return  String.format("%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
    }

    //On altering of the CPs the list view is refreshed and the new total balance for the day is calculated.
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

                    dayTotalBalance = sum;
                    dayBalance.setText(millisecondsToTimeString(sum));

                    updateMonthBalance();

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

    //When a new CP happens refresh the list view (also refresh day balance) and update the month balance.
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

    //When a CP is deleted refresh the list view (also refresh day balance) and update the month balance.
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

    //Calculate current month balance.
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

                    monthBalanceDayBefore = getClockPunchesBalance(cps);

                } else {
                    showToastMessage("Nenhum ponto encontrado");
                }
            }
        };
        new GetClockPunchesTask(listener).execute(month+"/"+year);
    }

    //Calculate the month balance based on the current day balance (saved in milliseconds) and the month balance (saved considering the whole month minus the current day).
    public void updateMonthBalance()
    {
        monthBalance.setText( millisecondsToTimeString( monthBalanceDayBefore + dayTotalBalance ) );
    }

    public void scheduleAutomaticDayBalanceCalculationJob(){
        JobScheduler mJobScheduler = (JobScheduler) getApplicationContext().getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( 1,
                new ComponentName( getApplicationContext(),
                        JobSchedulerService.class.getName() ) );

        int runEveryNumberHours = 4;
        builder.setPeriodic( 1000 * 60 * 60 * runEveryNumberHours );
        builder.setPersisted(true);

        if( mJobScheduler.schedule( builder.build() ) == JobScheduler.RESULT_FAILURE ) {
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

}//End of Class
