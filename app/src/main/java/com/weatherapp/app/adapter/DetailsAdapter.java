package com.weatherapp.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.weatherapp.app.R;
import com.weatherapp.app.model.DetailItem;

import java.util.List;

public class DetailsAdapter extends RecyclerView.Adapter<DetailsAdapter.DetailViewHolder> {

    private Context context;
    private List<DetailItem> detailList;

    public DetailsAdapter(Context context, List<DetailItem> detailList) {
        this.context = context;
        this.detailList = detailList;
    }

    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_detail_grid_item, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        DetailItem item = detailList.get(position);
        holder.title.setText(item.getTitle());
        holder.value.setText(item.getValue()); // Keeping full value for this compact view
        holder.icon.setImageResource(item.getIconResId());
        
        // Use color from item
        holder.icon.setColorFilter(item.getColor()); 
        
        // Optional: Tint value text as well or keep it white
        // holder.value.setTextColor(item.getColor());
    }
    
    @Override
    public int getItemCount() {
        return detailList.size();
    }

    public static class DetailViewHolder extends RecyclerView.ViewHolder {
        TextView title, value, subtitle;
        ImageView icon;

        public DetailViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.detail_title);
            value = itemView.findViewById(R.id.detail_value);
            subtitle = itemView.findViewById(R.id.detail_subtitle);
            icon = itemView.findViewById(R.id.detail_icon);
        }
    }
}
