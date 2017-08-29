package singularity.com.br.fkngpunchclock.activities.adapter;

/**
 * Created by Andre on 01/07/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import singularity.com.br.fkngpunchclock.R;
import singularity.com.br.fkngpunchclock.entities.ClockPunch;

/**
 * Created by ahrodrigues1 on 14/06/2017.
 */

public class PunchListAdapter extends ArrayAdapter<ClockPunch> {

    private List<ClockPunch> items;
    private int layoutResourceId;
    private Context context;

    public PunchListAdapter(Context context, int layoutResourceId, List<ClockPunch> items) {
        super(context, layoutResourceId, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        PunchHolder holder = null;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResourceId, parent, false);

        holder = new PunchHolder();
        holder.clockPunch = items.get(position);
        holder.removeButton = (ImageButton)row.findViewById(R.id.pilv_remove_button);
        holder.removeButton.setTag(holder.clockPunch);

        holder.time = (TextView)row.findViewById(R.id.pilv_time);

        holder.inOutImage = (ImageView)row.findViewById(R.id.pilv_inOutImage);
        if(position % 2 == 1){
            holder.inOutImage.setImageResource(R.drawable.ic_flight_takeoff_black_24dp);
        }

        row.setTag(holder);

        setupItem(holder);
        return row;
    }

    private void setupItem(PunchHolder holder) {
        holder.time.setText(String.valueOf(holder.clockPunch.getTime()));
    }

    public static class PunchHolder {
        ClockPunch clockPunch;
        TextView time;
        ImageView inOutImage;
        ImageButton removeButton;
    }

}

