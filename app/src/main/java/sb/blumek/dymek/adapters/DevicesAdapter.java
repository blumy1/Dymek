package sb.blumek.dymek.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;

import sb.blumek.dymek.R;


public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.MyViewHolder> {
    private List<BluetoothDevice> deviceList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice bluetoothDevice);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void addDevice(BluetoothDevice device) {
        if (deviceList == null ||
                deviceList.contains(device))
            return;

        deviceList.add(device);
        notifyItemInserted(deviceList.size() - 1);
        notifyDataSetChanged();
    }

    public void clear() {
        if (deviceList == null)
            return;
        deviceList.clear();
        notifyItemRangeRemoved(0, deviceList.size());
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView deviceAddressTV;
        TextView deviceNameTV;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceAddressTV = itemView.findViewById(R.id.address_tv);
            deviceNameTV = itemView.findViewById(R.id.name_tv);
        }
    }

    public DevicesAdapter(OnItemClickListener onItemClickListener,
                          List<BluetoothDevice> deviceList) {
        this(onItemClickListener);
        this.deviceList = deviceList;
    }

    public DevicesAdapter(OnItemClickListener onItemClickListener) {
        this();
        this.onItemClickListener = onItemClickListener;
    }

    public DevicesAdapter() {
        this.deviceList = new ArrayList<>();
    }

    @NonNull
    @Override
    public DevicesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.device_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        String address = deviceList.get(position).getAddress();
        String name = deviceList.get(position).getName();
        holder.deviceAddressTV.setText(address);
        holder.deviceNameTV.setText(name);
        holder.itemView.setOnClickListener(view ->
                onItemClickListener.onItemClick(deviceList.get(position)));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}