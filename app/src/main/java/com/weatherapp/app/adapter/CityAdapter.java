package com.weatherapp.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.weatherapp.app.R;
import com.weatherapp.app.db.CityEntity;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.CityViewHolder> {

    private Context context;
    private List<CityEntity> cities;
    private OnCityClickListener listener;
    private OnPinClickListener pinListener;
    private OnDeleteClickListener deleteListener;

    public interface OnCityClickListener {
        void onCityClick(CityEntity city);
    }
    
    public interface OnPinClickListener {
        void onPinClick(CityEntity city);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(CityEntity city);
    }

    public CityAdapter(Context context, List<CityEntity> cities, OnCityClickListener listener, OnPinClickListener pinListener, OnDeleteClickListener deleteListener) {
        this.context = context;
        this.cities = cities;
        this.listener = listener;
        this.pinListener = pinListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.city_item_layout, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        CityEntity city = cities.get(position);
        holder.cityNameTv.setText(city.name);
        
        // Display weather info
        if (city.currentTemp != null && !city.currentTemp.equals("--")) {
            holder.tempTv.setText(city.currentTemp + "°");
        } else {
            holder.tempTv.setText("--");
        }
        
        if (city.weatherDesc != null && !city.weatherDesc.isEmpty()) {
            holder.weatherDescTv.setText(city.weatherDesc);
        } else {
            holder.weatherDescTv.setText("Tap to load");
        }
        
        if (city.aqi != null && !city.aqi.isEmpty()) {
            holder.aqiTv.setText("AQI: " + city.aqi);
        } else {
            holder.aqiTv.setText("");
        }
        
        // Pin Logic
        if (city.isFavorite) {
            holder.pinIcon.setImageResource(R.drawable.ic_pin_filled);
            holder.pinIcon.setColorFilter(context.getResources().getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.pinIcon.setImageResource(R.drawable.ic_pin_outline);
            holder.pinIcon.setColorFilter(context.getResources().getColor(R.color.textColorSecondary), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        holder.pinIcon.setOnClickListener(v -> pinListener.onPinClick(city));
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDeleteClick(city));
        holder.itemView.setOnClickListener(v -> listener.onCityClick(city));
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    public static class CityViewHolder extends RecyclerView.ViewHolder {
        TextView cityNameTv, tempTv, weatherDescTv, aqiTv;
        android.widget.ImageView pinIcon, deleteBtn;

        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            cityNameTv = itemView.findViewById(R.id.city_name_tv);
            tempTv = itemView.findViewById(R.id.temp_tv);
            weatherDescTv = itemView.findViewById(R.id.weather_desc_tv);
            aqiTv = itemView.findViewById(R.id.aqi_tv);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            deleteBtn = itemView.findViewById(R.id.delete_btn);
        }
    }
}
