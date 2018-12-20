package com.example.android.harjoitus7_8;

import android.content.Context;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import java.util.Calendar;


/**
 * Created by Roope on 28-Feb-18.
 */

public class TrainingAdapter extends FirebaseRecyclerAdapter<TrainingEntry, TrainingAdapter.TrainingAdapterViewHolder> {

    public TrainingAdapter(FirebaseRecyclerOptions<TrainingEntry> options) {
        super(options);
    }


    public class TrainingAdapterViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDateTextView;
        public final TextView mSportTextView;
        public final TextView mDurationTextView;
        public final TextView mRpeTextView;
        public final TextView mSharpnessTextView;

        public TrainingAdapterViewHolder(View view) {
            super(view);
            mDateTextView = (TextView) view.findViewById(R.id.tv_training_date);
            mSportTextView = (TextView) view.findViewById(R.id.tv_training_sport);
            mDurationTextView = (TextView) view.findViewById(R.id.tv_training_duration);
            mRpeTextView = (TextView) view.findViewById(R.id.tv_training_rpe);
            mSharpnessTextView = (TextView) view.findViewById(R.id.tv_training_sharpness);
        }
    }

    @Override
    public TrainingAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.training_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        TrainingAdapterViewHolder viewHolder = new TrainingAdapterViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(TrainingAdapterViewHolder holder, int position, TrainingEntry model) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(model.getTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        holder.mDateTextView.setText(dateFormat.format(calendar.getTime()));
        holder.mSportTextView.setText(model.getSport());
        holder.mDurationTextView.setText(String.valueOf(model.getDuration()) + " min");
        holder.mSharpnessTextView.setText(String.valueOf(model.getSharpness()));
        holder.mRpeTextView.setText(String.valueOf(model.getRpe()));
    }


}

