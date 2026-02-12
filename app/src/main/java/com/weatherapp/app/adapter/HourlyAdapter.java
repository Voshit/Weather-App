package com.weatherapp.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.weatherapp.app.R;
import com.weatherapp.app.update.UpdateUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.ViewHolder> {
    private final Context context;
    private final JSONObject hourlyData;

    public HourlyAdapter(Context context, JSONObject hourlyData) {
        this.context = context;
        this.hourlyData = hourlyData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hourly, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            // Get data for this hour
            long time = hourlyData.getJSONArray("time").getLong(position);
            int code = hourlyData.getJSONArray("weather_code").getInt(position);
            double temp = hourlyData.getJSONArray("temperature_2m").getDouble(position);
            String timeStr = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date(time * 1000));

            // Determine if it is day/night based on time (simple logic or passed param)
            // Ideally we check sunrise/sunset but for hourly we can approximate or just use day icons
            // Let's assume day for simplicity or check hour (6-18)
            int hour = Integer.parseInt(timeStr.substring(0, 2));
            int isDay = (hour >= 6 && hour < 18) ? 1 : 0;

            holder.timeTv.setText(timeStr);
            holder.tempTv.setText(String.format("%.0f°", temp));
            
            holder.iconIv.setImageResource(
                    context.getResources().getIdentifier(
                            UpdateUI.getIconID(code, isDay, 0, 0),
                            "drawable",
                            context.getPackageName()
                    ));

            // Highlight current hour
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime >= time && currentTime < time + 3600) {
                holder.itemView.setBackgroundResource(R.drawable.bg_status_pill);
                holder.timeTv.setTextColor(android.graphics.Color.WHITE);
                holder.tempTv.setTextColor(android.graphics.Color.WHITE);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_white_card);
                holder.timeTv.setTextColor(context.getResources().getColor(R.color.textColorSecondary));
                holder.tempTv.setTextColor(context.getResources().getColor(R.color.textColorPrimary));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        try {
            if (hourlyData != null && hourlyData.has("time")) {
                // Limit to next 48 hours
                return Math.min(48, hourlyData.getJSONArray("time").length());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeTv, tempTv;
        ImageView iconIv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTv = itemView.findViewById(R.id.hourly_time);
            tempTv = itemView.findViewById(R.id.hourly_temp);
            iconIv = itemView.findViewById(R.id.hourly_icon);
        }
    }
}
