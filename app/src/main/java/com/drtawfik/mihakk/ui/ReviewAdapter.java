package com.drtawfik.mihakk.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drtawfik.mihakk.data.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {

    public interface OnPick {
        void onPick(Review r);
    }

    private final List<Review> items = new ArrayList<>();
    private final OnPick listener;

    public ReviewAdapter(OnPick listener) {
        this.listener = listener;
    }

    public void submit(List<Review> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ReviewRow.inflate(parent.getContext(), parent));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Review r = items.get(position);
        ReviewRow.bind(h.itemView, r);
        h.itemView.setOnClickListener(v -> listener.onPick(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(View v) {
            super(v);
        }
    }
}
