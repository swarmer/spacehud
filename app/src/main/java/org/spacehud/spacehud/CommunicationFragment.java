package org.spacehud.spacehud;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class CommunicationFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public CommunicationFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static CommunicationFragment newInstance() {
        CommunicationFragment fragment = new CommunicationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.communication_fragment, container, false);
        return rootView;
    }
}
