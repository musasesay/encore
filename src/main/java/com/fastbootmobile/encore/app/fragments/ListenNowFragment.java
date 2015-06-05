/*
 * Copyright (C) 2014 Fastboot Mobile, LLC.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>.
 */

package com.fastbootmobile.encore.app.fragments;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.TransactionTooLargeException;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.TextView;

import com.fastbootmobile.encore.api.echonest.AutoMixBucket;
import com.fastbootmobile.encore.api.echonest.AutoMixManager;
import com.fastbootmobile.encore.app.AppActivity;
import com.fastbootmobile.encore.app.MainActivity;
import com.fastbootmobile.encore.app.R;
import com.fastbootmobile.encore.app.SearchActivity;
import com.fastbootmobile.encore.app.adapters.ListenNowAdapter;
import com.fastbootmobile.encore.app.ui.ParallaxScrollListView;
import com.fastbootmobile.encore.app.ui.ScrollStatusBarColorListener;
import com.fastbootmobile.encore.framework.ListenLogger;
import com.fastbootmobile.encore.framework.PluginsLookup;
import com.fastbootmobile.encore.model.Album;
import com.fastbootmobile.encore.model.Artist;
import com.fastbootmobile.encore.model.Playlist;
import com.fastbootmobile.encore.model.SearchResult;
import com.fastbootmobile.encore.model.Song;
import com.fastbootmobile.encore.providers.ILocalCallback;
import com.fastbootmobile.encore.providers.IMusicProvider;
import com.fastbootmobile.encore.providers.ProviderAggregator;
import com.fastbootmobile.encore.providers.ProviderConnection;
import com.fastbootmobile.encore.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass showing ideas of tracks and albums to listen to.
 * Use the {@link ListenNowFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListenNowFragment extends Fragment implements ILocalCallback {
    private static final String TAG = "ListenNowFragment";

    private static final String PREFS = "listen_now";
    private static final String LANDCARD_NO_CUSTOM_PROVIDERS = "card_no_custom";

    private View mHeaderView;
    private int mBackgroundColor;
    private EditText mSearchBox;
    private CardView mCardSearchBox;
    private AbsListView.OnScrollListener mScrollListener;
    private ListenNowAdapter mAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ListenNowFragment.
     */
    public static ListenNowFragment newInstance() {
        return new ListenNowFragment();
    }

    /**
     * Default empty constructor
     */
    public ListenNowFragment() {
        mScrollListener = new ScrollStatusBarColorListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (view.getChildCount() == 0 || getActivity() == null) {
                    return;
                }

                final float heroHeight = mHeaderView.getMeasuredHeight();
                final float scrollY = getScroll(view);
                final float toolbarBgAlpha = Math.min(1, scrollY / heroHeight);
                final int toolbarAlphaInteger = (((int) (toolbarBgAlpha * 255)) << 24) | 0xFFFFFF;
                mColorDrawable.setColor(toolbarAlphaInteger & mBackgroundColor);

                SpannableString spannableTitle = new SpannableString(((MainActivity) getActivity()).getFragmentTitle());
                mAlphaSpan.setAlpha(toolbarBgAlpha);

                ActionBar actionbar = ((AppActivity) getActivity()).getSupportActionBar();
                if (actionbar != null) {
                    actionbar.setBackgroundDrawable(mColorDrawable);
                    spannableTitle.setSpan(mAlphaSpan, 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    actionbar.setTitle(spannableTitle);
                }

                mCardSearchBox.setAlpha(1.0f - toolbarBgAlpha);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackgroundColor = getResources().getColor(R.color.primary);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public float getToolbarAlpha() {
        return 1.0f - mCardSearchBox.getAlpha();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_listen_now, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        View root = getView();

        if (root != null) {
            ParallaxScrollListView listView = (ParallaxScrollListView) root;
            listView.setOnScrollListener(mScrollListener);
            setupHeader(listView);

            mAdapter = new ListenNowAdapter();
            listView.setAdapter(mAdapter);
            setupItems();
        }
    }

    private void setupHeader(ParallaxScrollListView listView) {
        LayoutInflater inflater = LayoutInflater.from(listView.getContext());
        mHeaderView = inflater.inflate(R.layout.header_listen_now, listView, false);
        mCardSearchBox = (CardView) mHeaderView.findViewById(R.id.cardSearchBox);
        mSearchBox = (EditText) mHeaderView.findViewById(R.id.ebSearch);
        mSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    final String query = v.getText().toString();

                    Intent intent = new Intent(getActivity(), SearchActivity.class);
                    intent.setAction(Intent.ACTION_SEARCH);
                    intent.putExtra(SearchManager.QUERY, query);
                    v.getContext().startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        listView.addParallaxedHeaderView(mHeaderView);
    }

    private void setupItems() {
        final ProviderAggregator aggregator = ProviderAggregator.getDefault();
        final PluginsLookup plugins = PluginsLookup.getDefault();
        final List<Playlist> playlists = aggregator.getAllPlaylists();
        final List<Song> songs = new ArrayList<>();

        // Get the list of songs first
        final List<ProviderConnection> providers = plugins.getAvailableProviders();
        for (ProviderConnection provider : providers) {
            int limit = 50;
            int offset = 0;

            while (true) {
                try {
                    List<Song> providerSongs = provider.getBinder().getSongs(offset, limit);

                    songs.addAll(providerSongs);
                    offset += providerSongs.size();

                    if (providerSongs.size() < limit) {
                        Log.d(TAG, "Got " + providerSongs.size() + " instead of " + limit + ", assuming end of list");
                        break;
                    }
                } catch (TransactionTooLargeException e) {
                    limit -= 5;
                    if (limit <= 0) {
                        Log.e(TAG, "Error getting songs from " + provider.getProviderName()
                                + ": transaction too large even with limit = 5");
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting songs from " + provider.getProviderName() + ": " + e.getMessage());
                    break;
                }
            }
        }


        // Add a card if we have local music, but no cloud providers
        if (providers.size() <= PluginsLookup.BUNDLED_PROVIDERS_COUNT && songs.size() > 0) {
            final SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, 0);
            if (!prefs.getBoolean(LANDCARD_NO_CUSTOM_PROVIDERS, false)) {
                // Show the "You have no custom providers" card
                mAdapter.addItem(new ListenNowAdapter.CardItem(getString(R.string.ln_landcard_nocustomprovider_title),
                        getString(R.string.ln_landcard_nocustomprovider_body),
                        getString(R.string.browse), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProviderDownloadDialog.newInstance(false).show(getFragmentManager(), "DOWN");
                    }
                },
                        getString(R.string.ln_landcard_dismiss), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prefs.edit().putBoolean(LANDCARD_NO_CUSTOM_PROVIDERS, true).apply();
                        // This item must always be the first of the list
                        mAdapter.removeItem(0);
                    }
                }));
            }
        }

        // Add a card if there's no music at all (no songs and no playlists)
        if (providers.size() <= PluginsLookup.BUNDLED_PROVIDERS_COUNT && songs.size() == 0 && playlists.size() == 0) {
            mAdapter.addItem(new ListenNowAdapter.CardItem(getString(R.string.ln_card_nothing_title),
                    getString(R.string.ln_card_nothing_body),
                    getString(R.string.browse),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ProviderDownloadDialog.newInstance(false).show(getFragmentManager(), "DOWN");
                        }
                    },
                    getString(R.string.configure),
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            ((MainActivity) getActivity()).openSection(MainActivity.SECTION_SETTINGS);
                        }
                    }));

            mAdapter.addItem(new ListenNowAdapter.CardItem(getString(R.string.ln_card_nothinghint_title),
                    getString(R.string.ln_card_nothinghint_body), null, null));
        }


        // Add the "Recently played" section if we have recent tracks
        final ListenLogger logger = new ListenLogger(getActivity());
        List<ListenLogger.LogEntry> logEntries = logger.getEntries(50);

        if (logEntries.size() > 0) {
            mAdapter.addItem(new ListenNowAdapter.SectionHeaderItem(getString(R.string.ln_section_recents),
                    R.drawable.ic_nav_history_active, getString(R.string.more), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).openSection(MainActivity.SECTION_HISTORY);
                }
            }));

            int i = 0;
            List<ListenNowAdapter.ItemCardItem> itemsCouple = new ArrayList<>();

            for (ListenLogger.LogEntry entry : logEntries) {
                if (i == 4) {
                    // Stop here, add remaining item
                    if (itemsCouple.size() > 0) {
                        for (ListenNowAdapter.ItemCardItem item : itemsCouple) {
                            mAdapter.addItem(item);
                        }
                    }
                    break;
                }

                Song song = aggregator.retrieveSong(entry.getReference(), entry.getIdentifier());
                if (song != null) {
                    int type = Utils.getRandom(2);
                    if (song.getAlbum() != null && (type == 0 || type == 1 && song.getArtist() == null)) {
                        Album album = aggregator.retrieveAlbum(song.getAlbum(), song.getProvider());

                        if (album != null) {
                            itemsCouple.add(new ListenNowAdapter.ItemCardItem(album));
                            ++i;
                        }
                    } else if (song.getArtist() != null) {
                        Artist artist = aggregator.retrieveArtist(song.getArtist(), song.getProvider());

                        if (artist != null) {
                            itemsCouple.add(new ListenNowAdapter.ItemCardItem(artist));
                            ++i;
                        }
                    }
                }

                if (itemsCouple.size() == 2) {
                    ListenNowAdapter.CardRowItem row = new ListenNowAdapter.CardRowItem(
                            itemsCouple.get(0),
                            itemsCouple.get(1)
                    );
                    mAdapter.addItem(row);
                    itemsCouple.clear();
                }
            }
        }

        // Add playlists section
        mAdapter.addItem(new ListenNowAdapter.SectionHeaderItem(getString(R.string.ln_section_playlists),
                R.drawable.ic_nav_playlist_active, getString(R.string.browse), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).openSection(MainActivity.SECTION_PLAYLISTS);
            }
        }));

        if (playlists != null && playlists.size() > 0) {
            int i = 0;
            List<ListenNowAdapter.ItemCardItem> itemsCouple = new ArrayList<>();

            for (Playlist playlist : playlists) {
                if (i == 4) {
                    // Stop here, add remaining item
                    if (itemsCouple.size() > 0) {
                        for (ListenNowAdapter.ItemCardItem item : itemsCouple) {
                            mAdapter.addItem(item);
                        }
                    }
                    break;
                }

                if (playlist != null) {
                    ListenNowAdapter.ItemCardItem item = new ListenNowAdapter.ItemCardItem(playlist);
                    itemsCouple.add(item);
                    ++i;
                }

                if (itemsCouple.size() == 2) {
                    ListenNowAdapter.CardRowItem row = new ListenNowAdapter.CardRowItem(
                            itemsCouple.get(0),
                            itemsCouple.get(1)
                    );
                    mAdapter.addItem(row);
                    itemsCouple.clear();
                }
            }
        }


        // Add automix section
        mAdapter.addItem(new ListenNowAdapter.SectionHeaderItem(getString(R.string.lb_section_automixes),
                R.drawable.ic_nav_automix_active, getString(R.string.create), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).openSection(MainActivity.SECTION_AUTOMIX);
            }
        }));

        List<AutoMixBucket> buckets = AutoMixManager.getDefault().getBuckets();
        if (buckets == null || buckets.size() == 0) {
            mAdapter.addItem(new ListenNowAdapter.GetStartedItem(getString(R.string.ln_automix_getstarted_body),
                    getString(R.string.ln_action_getstarted), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).onNavigationDrawerItemSelected(MainActivity.SECTION_AUTOMIX);
                }
            }));
        } else {
            for (final AutoMixBucket bucket : buckets) {
                mAdapter.addItem(new ListenNowAdapter.SimpleItem(bucket.getName(),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new Thread() {
                                    public void run() {
                                        AutoMixManager.getDefault().startPlay(bucket);
                                    }
                                }.start();
                            }
                        }));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.onSectionAttached(MainActivity.SECTION_LISTEN_NOW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        ProviderAggregator.getDefault().addUpdateCallback(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        ProviderAggregator.getDefault().removeUpdateCallback(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSongUpdate(final List<Song> s) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAlbumUpdate(final List<Album> a) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPlaylistUpdate(List<Playlist> p) {
    }

    @Override
    public void onPlaylistRemoved(String ref) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onArtistUpdate(final List<Artist> a) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProviderConnected(IMusicProvider provider) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSearchResult(List<SearchResult> searchResult) {
    }
}
