package singularity.com.br.fkngpunchclock.entities;

/**
 * Created by Andre on 01/07/2017.
 */

public class DayBalance {

    int id;
    Integer day;
    Integer month;
    Integer year;
    Double balance;

    public DayBalance() {
    }

    public DayBalance(Integer day, Integer month, Integer year, Double balance) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.balance = balance;
    }

    public Integer getDay() {
        return day;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
