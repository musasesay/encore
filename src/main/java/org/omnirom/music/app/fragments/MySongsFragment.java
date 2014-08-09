package org.omnirom.music.app.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.omnirom.music.app.MainActivity;
import org.omnirom.music.app.R;
import org.omnirom.music.app.adapters.MySongsAdapter;

/**
 * Created by h4o on 19/06/2014.
 */
public class MySongsFragment extends Fragment {
    ViewPager mViewPager;
    MySongsAdapter mySongsAdapter;
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

    }
    public static MySongsFragment newInstance() {
        MySongsFragment fragment = new MySongsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_my_songs, container, false);
        assert root != null;

        mySongsAdapter = new MySongsAdapter(getResources(), getChildFragmentManager());
        mViewPager = (ViewPager) root.findViewById(R.id.pager);

        mViewPager.setOffscreenPageLimit(2);
        PagerTabStrip pagerTitleStrip = (PagerTabStrip) root.findViewById(R.id.pager_title_strip);
        pagerTitleStrip.setTabIndicatorColorResource(R.color.primary_light);

        mViewPager.setAdapter(mySongsAdapter);
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_MY_SONGS);
    }
}
