package pers.missingno.bledemo.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pers.missingno.bledemo.R;
import pers.missingno.bledemo.adapter.BluetoothRecyclerAdapter;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_OPEN_FILE=0;
    public static final String SERVICE_UUID="655c0b34-b03d-46bc-aed9-d6c6c5eb9628";

    private BluetoothAdapter bluetoothAdapter;
    private RecyclerView recyclerView;
    private BluetoothRecyclerAdapter bluetoothRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView= (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        bluetoothRecyclerAdapter=new BluetoothRecyclerAdapter();
        recyclerView.setAdapter(bluetoothRecyclerAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                    Toast.makeText(MainActivity.this, R.string.ble_not_support,Toast.LENGTH_SHORT).show();
                    return;
                }
                if(bluetoothAdapter==null||!bluetoothAdapter.isEnabled()){
                    Toast.makeText(MainActivity.this, R.string.open_bluetooth,Toast.LENGTH_SHORT).show();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "选择文件"), REQUEST_OPEN_FILE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_waiting) {
            if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                Toast.makeText(MainActivity.this, R.string.ble_not_support,Toast.LENGTH_SHORT).show();
                return true;
            }
            if(bluetoothAdapter==null||!bluetoothAdapter.isEnabled()){
                Toast.makeText(MainActivity.this, R.string.open_bluetooth,Toast.LENGTH_SHORT).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
                return true;
            }

            final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            final ScanCallback callback=new ScanCallback() {
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    ParcelUuid[] uuids = result.getDevice().getUuids();
                    for(ParcelUuid uuid : uuids){
                        if(uuid.toString().equals(SERVICE_UUID)){
                            bluetoothRecyclerAdapter.addItem(result.getDevice());
                            break;
                        }
                    }
                }
            };
            Toast.makeText(MainActivity.this, R.string.start_scan,Toast.LENGTH_SHORT).show();
            scanner.startScan(callback);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, R.string.stop_scan,Toast.LENGTH_SHORT).show();
                        }
                    });
                    scanner.stopScan(callback);
                }
            }).start();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_OPEN_FILE:
                if(resultCode==RESULT_OK){
                    final Intent intent=data;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String filePath=getPathByUri4kitkat(MainActivity.this,intent.getData());
                            ByteArrayOutputStream baos=new ByteArrayOutputStream();
                            byte[] buf=new byte[256];
                            FileInputStream fis=null;
                            try {
                                fis=new FileInputStream(filePath);
                                int len;
                                while ((len=fis.read(buf))>0){
                                    baos.write(buf,0,len);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                if(fis!=null)
                                    try {
                                        fis.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                            }
                            byte[] data=baos.toByteArray();

                            List<BluetoothDevice> deviceList=new ArrayList<>();
                            List<String> nameList = new ArrayList<>();
                            ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1,nameList);

                            BluetoothLeAdvertiser advertiser=bluetoothAdapter.getBluetoothLeAdvertiser();

                            AdvertiseSettings.Builder settingsbuilder = new AdvertiseSettings.Builder();
                            settingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
                            settingsbuilder.setConnectable(true);
                            settingsbuilder.setTimeout(0);
                            settingsbuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
                            AdvertiseSettings advertiseSettings = settingsbuilder.build();

                            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
                            dataBuilder.addServiceUuid(ParcelUuid.fromString(SERVICE_UUID));
                            dataBuilder.addServiceData(ParcelUuid.fromString(SERVICE_UUID),data);
                            AdvertiseData advertiseData = dataBuilder.build();

                            advertiser.startAdvertising(advertiseSettings, advertiseData, new AdvertiseCallback() {
                                @Override
                                public void onStartFailure(final int errorCode) {
                                    super.onStartFailure(errorCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "发送失败:"+errorCode, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                                    super.onStartSuccess(settingsInEffect);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this,"准备完成",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        }
                    }).start();

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPathByUri4kitkat(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {// ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {// DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {// MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {// MediaStore
            // (and
            // general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {// File
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
