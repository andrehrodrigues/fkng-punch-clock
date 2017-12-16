package singularity.com.br.fkngpunchclock.entities;

/**
 * Created by Andre on 01/07/2017.
 */

public class ClockPunch {

    int id;
    int day;
    int month;
    int year;
    String date;
    String time;

    public ClockPunch() {
    }

    public ClockPunch(String date, String time, boolean is24HourModeEnabled) {

        //Get the full data string and splits it into separeted values.
        convertDateStringToInt(date);

        //If is not 24 hours time treat to convert to 24.
        if( is24HourModeEnabled  == false){
            String parts[] = time.split(" ");
            if("PM".equals(parts[1])){
                time = convertTimeStringTo24Format(parts[0]);
            }
        }

        this.time = time;
    }

    public ClockPunch(int day, int month, int year, String date, String time) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.date = date;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    private void convertDateStringToInt(String date){
        String parts[] = date.split("/");
        setDay(Integer.parseInt(parts[0]));
        setMonth(Integer.parseInt(parts[1]));
        setYear(Integer.parseInt(parts[2]));
    }

    private String convertTimeStringTo24Format(String time){
        String parts[] = time.split(":");
        String convTime = String.valueOf(Integer.parseInt(parts[0]) + 12)+":"+parts[1];
        return convTime;
    }
}
