package singularity.com.br.fkngpunchclock.entities;

/**
 * Created by Andre on 01/07/2017.
 */

public class ClockPunch {

    int id;
    String date;
    String time;

    public ClockPunch() {
    }

    public ClockPunch(String date, String time) {
        this.date = date;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
