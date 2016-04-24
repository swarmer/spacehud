package org.spacehud.spacehud;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private AtomicBoolean runBluetoothListener = new AtomicBoolean();
    private Thread bluetoothThread;
    private BluetoothSocket bluetoothSocket;
    private boolean sensorsConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
        runBluetoothListener.set(false);
        try {
            if (bluetoothThread != null)
                bluetoothThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showFatalError(String title, String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(1);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        System.exit(1);
    }

    private void setupBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            showFatalError("Error", "Bluetooth not available");

        if (!bluetoothAdapter.isEnabled()) {
            showFatalError("Error", "Bluetooth disabled");
        }

        BluetoothDevice suitSensorDevice = null;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("HC-06"))
                suitSensorDevice = device;
        }

        if (suitSensorDevice == null) {
            showFatalError("Error", "HC-06 bluetooth device not found");
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            bluetoothSocket = suitSensorDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
            showFatalError("Error", e.getMessage());
        }

        for (int i = 0; i < 2; ++i) {
            try {
                bluetoothSocket.connect();
                sensorsConnected = true;
                break;
            } catch (IOException e) {
            }
        }

        if (!sensorsConnected)
            return;

        InputStream stream = null;
        try {
            stream = bluetoothSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            showFatalError("Error", e.getMessage());
        }

        runBluetoothListener.set(true);
        final InputStream finalInputStreamWat = stream;
        bluetoothThread = new Thread(new Runnable() {
            public void run() {
                bluetoothListener(finalInputStreamWat);
            }
        });
        bluetoothThread.start();
    }

    private void bluetoothListener(InputStream inputStream) {
        StringBuilder commandBuilder = new StringBuilder();
        while (runBluetoothListener.get()) {
            byte[] buffer = new byte[1024];
            try {
                int count = inputStream.read(buffer);
                String message = new String(buffer, 0, count);
                commandBuilder.append(message);

                String commands = commandBuilder.toString();
                if (commands.contains("\n")) {
                    String[] lines = commands.split("\\s+");
                    if (commands.endsWith("\n")) {
                        for (String line : lines) {
                            if (line.startsWith("BEAT")) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        onBeat();
                                    }
                                });
                            }
                        }
                        commandBuilder = new StringBuilder();
                    } else {
                        for (int i = 0; i < lines.length - 1; ++i) {
                            if (lines[i].startsWith("BEAT")) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        onBeat();
                                    }
                                });
                            }
                        }
                        commandBuilder = new StringBuilder(lines[lines.length - 1]);
                    }
                }
            } catch (final IOException e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        e.printStackTrace();
                        showFatalError("Error", e.getMessage());
                    }
                });

            }
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Fragment getCurrentFragment() {
        int index = ((ViewPager)findViewById(R.id.container)).getCurrentItem();
        return mSectionsPagerAdapter.fragments[index];
    }

    public void onBeat() {
        Fragment currentFragment = getCurrentFragment();

        if (currentFragment instanceof MainFragment) {
            ((MainFragment)currentFragment).onBeat();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public Fragment[] fragments;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            fragments = new Fragment[]{
                    MainFragment.newInstance(),
                    HealthFragment.newInstance(),
                    SuitFragment.newInstance(),
                    CommunicationFragment.newInstance()
            };
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "ОБЩЕЕ";
                case 1:
                    return "ЗДОРОВЬЕ";
                case 2:
                    return "КОСТЮМ";
                case 3:
                    return "СВЯЗЬ";
            }
            return null;
        }
    }
}
