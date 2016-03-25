package pers.missingno.bledemo.adapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pers.missingno.bledemo.activity.MainActivity;

public class BluetoothRecyclerAdapter extends RecyclerView.Adapter<BluetoothRecyclerAdapter.BluetoothViewHolder> {

    private List<BluetoothDevice> devices;

    public BluetoothRecyclerAdapter(){
        devices=new ArrayList<>();
    }

    @Override
    public BluetoothViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BluetoothViewHolder(new TextView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(final BluetoothViewHolder holder, final int position) {
        holder.textView.setText(devices.get(position).getName());
        holder.textView.setTextSize(20);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                BluetoothGatt gatt=devices.get(position).connectGatt(v.getContext(), true, new BluetoothGattCallback() {
                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                        if(status==BluetoothGatt.GATT_SUCCESS){
                            FileOutputStream fos=null;
                            try {
                                fos=v.getContext().openFileOutput("file", Context.MODE_PRIVATE);
                                fos.write(characteristic.getValue());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                if(fos!=null)
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                            }
                        }
                    }
                });
                BluetoothGattService service=gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID));
                BluetoothGattCharacteristic characteristic=service.getCharacteristic(UUID.fromString(MainActivity.SERVICE_UUID));
                gatt.readCharacteristic(characteristic);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void addItem(BluetoothDevice device){
        devices.add(device);
        notifyItemInserted(devices.size()-1);
    }

    public static class BluetoothViewHolder extends RecyclerView.ViewHolder{

        protected TextView textView;

        public BluetoothViewHolder(View itemView) {
            super(itemView);
            textView= (TextView) itemView;
        }
    }
}
