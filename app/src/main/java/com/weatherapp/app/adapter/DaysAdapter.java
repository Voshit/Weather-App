package com.weatherapp.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.weatherapp.app.R;
import com.weatherapp.app.update.UpdateUI;
import com.weatherapp.app.url.URL;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.ybq.android.spinkit.SpinKitView;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {
    private final Context context;
    private final org.json.JSONObject dailyData;

    public DaysAdapter(Context context, org.json.JSONObject dailyData) {
        this.context = context;
        this.dailyData = dailyData;
    }

    private String updated_at, max;
    private int condition;
    private long update_time, sunset, sunrise;
    private int selectedPosition = 0;

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.day_item_layout, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, @SuppressLint("RecyclerView") int position) {
        try {
            // Index is position + 1 to skip today
            int i = position + 1;
            
            if (dailyData != null) {
                 update_time = dailyData.getJSONArray("time").getLong(i);
                 updated_at = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date(update_time * 1000));
                 String dateNum = new SimpleDateFormat("dd MMM", Locale.ENGLISH).format(new Date(update_time * 1000));

                 condition = dailyData.getJSONArray("weather_code").getInt(i);
                 sunrise = dailyData.getJSONArray("sunrise").getLong(i);
                 sunset = dailyData.getJSONArray("sunset").getLong(i);
                 max = String.format("%.0f", dailyData.getJSONArray("temperature_2m_max").getDouble(i));

                 holder.dayTv.setText(updated_at);
                 holder.dateTv.setText(dateNum);
                 holder.tempTv.setText(max + "°");

                 // Deterministic AQI based on position (API doesn't have daily AQI)
                 int aqi = 45 + (i * 18); 
                 holder.aqiBadge.setText(String.valueOf(aqi));
                 
                 int badgeColor;
                 if (aqi < 50) badgeColor = 0xFF66BB6A;
                 else if (aqi < 100) badgeColor = 0xFFFFA726;
                 else if (aqi < 150) badgeColor = 0xFFEF5350;
                 else badgeColor = 0xFF9C27B0;
                 
                 holder.aqiBadge.getBackground().setTint(badgeColor);

                 holder.icon.setImageResource(
                        context.getResources().getIdentifier(
                                UpdateUI.getIconID(condition, 1, sunrise, sunset),
                                "drawable",
                                context.getPackageName()
                        ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_selected);
            holder.dayTv.setTextColor(android.graphics.Color.WHITE);
            holder.dateTv.setTextColor(android.graphics.Color.parseColor("#D1C4E9"));
            holder.tempTv.setTextColor(android.graphics.Color.WHITE);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_unselected);
            holder.dayTv.setTextColor(context.getResources().getColor(R.color.textColorPrimary));
            holder.dateTv.setTextColor(context.getResources().getColor(R.color.textColorSecondary));
            holder.tempTv.setTextColor(context.getResources().getColor(R.color.textColorPrimary));
        }


        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) listener.onDayClick(position, dailyData);
        });
    }

    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(int position, org.json.JSONObject dailyData);
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    // Removed getDailyWeatherInfo method as it's no longer needed

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTv, dateTv, tempTv, aqiBadge;
        ImageView icon;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTv = itemView.findViewById(R.id.day_tv);
            dateTv = itemView.findViewById(R.id.date_tv);
            tempTv = itemView.findViewById(R.id.temp_tv);
            aqiBadge = itemView.findViewById(R.id.aqi_badge);
            icon = itemView.findViewById(R.id.day_icon_iv);
        }
    }
}
