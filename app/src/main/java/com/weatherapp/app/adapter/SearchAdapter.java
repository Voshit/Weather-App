package com.weatherapp.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.weatherapp.app.R;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private Context context;
    private List<String> cityList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String cityName);
    }

    public SearchAdapter(Context context, List<String> cityList, OnItemClickListener listener) {
        this.context = context;
        this.cityList = cityList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        String fullString = cityList.get(position);
        
        // Expected format: "City, Country" or "City, Admin, Country"
        // Let's try to split by comma to make it look nicer
        String city = fullString;
        String country = "";
        
        int firstComma = fullString.indexOf(",");
        if (firstComma != -1) {
            city = fullString.substring(0, firstComma).trim();
            country = fullString.substring(firstComma + 1).trim();
        }

        holder.cityName.setText(city);
        if (!country.isEmpty()) {
            holder.countryName.setText(country);
            holder.countryName.setVisibility(View.VISIBLE);
        } else {
            holder.countryName.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(fullString));
    }

    @Override
    public int getItemCount() {
        return cityList.size();
    }

    public void updateList(List<String> newList) {
        cityList = newList;
        notifyDataSetChanged();
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView cityName, countryName;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            cityName = itemView.findViewById(R.id.city_name);
            countryName = itemView.findViewById(R.id.country_name);
        }
    }
}
