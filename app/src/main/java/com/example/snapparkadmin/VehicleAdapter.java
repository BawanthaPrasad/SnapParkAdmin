package com.example.snapparkadmin;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {
    private List<VehicleData> vehicleList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView vehicleNumberTextView, latTextView, lonTextView, timeStampTextView;
        public Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            vehicleNumberTextView = itemView.findViewById(R.id.VehicleNo);
            latTextView = itemView.findViewById(R.id.lat);
            lonTextView = itemView.findViewById(R.id.lon);
            timeStampTextView = itemView.findViewById(R.id.timeStamp);
            deleteButton = itemView.findViewById(R.id.deleteButton);

        }
    }

    public VehicleAdapter(List<VehicleData> vehicleList) {
        this.vehicleList = vehicleList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VehicleData vehicle = vehicleList.get(position);
        holder.vehicleNumberTextView.setText("Vehicle No: "+String.valueOf(vehicle.getVehicleNo()));
        holder.latTextView.setText("Latitude    : "+String.valueOf(vehicle.getLatitude()));
        holder.lonTextView.setText("Longtitude: "+String.valueOf(vehicle.getLongitude()));
        holder.timeStampTextView.setText(String.valueOf(vehicle.getTimestamp()));

        // Handle delete button click if needed
        holder.deleteButton.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("reports")
                    .whereEqualTo("vehicleNo", vehicle.getVehicleNo())
                    .whereEqualTo("latitude", vehicle.getLatitude())
                    .whereEqualTo("longitude", vehicle.getLongitude())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            documentSnapshot.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Delete successful, remove the item from the list
                                        int deletedPosition = holder.getAdapterPosition();
                                        vehicleList.remove(deletedPosition);
                                        notifyItemRemoved(deletedPosition);
                                        Toast.makeText(holder.itemView.getContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle delete failure
                                        Toast.makeText(holder.itemView.getContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
                                        Log.e("DeleteFailure", "Failed to delete document", e);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle query failure
                        Toast.makeText(holder.itemView.getContext(), "Failed to query Firestore", Toast.LENGTH_SHORT).show();
                        Log.e("QueryFailure", "Failed to query Firestore", e);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }
}
