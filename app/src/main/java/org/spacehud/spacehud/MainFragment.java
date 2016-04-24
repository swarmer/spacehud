package org.spacehud.spacehud;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private TextView heartTextView;
    private TextView pulseTextView;
    private ImageView imageView;
    private List<Long> beats = new ArrayList<Long>();

    public MainFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_fragment, container, false);
        heartTextView = (TextView)rootView.findViewById(R.id.heartTextView);
        pulseTextView = (TextView)rootView.findViewById(R.id.pulseTextView);
        imageView = (ImageView)rootView.findViewById(R.id.imageView);

        return rootView;
    }

    private int calculateBpm() {
        int recentBeatCount = 0;
        for (long ts : beats) {
            if (ts >= System.currentTimeMillis() - 10000) {
                ++recentBeatCount;
            }
        }

        return recentBeatCount * (60 / 10);
    }

    private void updatePulseInfo() {
        int bpm = calculateBpm();
        pulseTextView.setText(String.valueOf(bpm));

        if (bpm < 30 || bpm > 150) {
            Drawable drawable = getResources().getDrawable(R.drawable.red);
            imageView.setImageDrawable(drawable);
        } else if (bpm < 40 || bpm > 120) {
            Drawable drawable = getResources().getDrawable(R.drawable.yellow);
            imageView.setImageDrawable(drawable);
        } else {
            Drawable drawable = getResources().getDrawable(R.drawable.green);
            imageView.setImageDrawable(drawable);
        }
    }

    public void onBeat() {
        beats.add(System.currentTimeMillis());

        heartTextView.setAlpha(1.0f);
        heartTextView.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(null);

        updatePulseInfo();
    }
}
