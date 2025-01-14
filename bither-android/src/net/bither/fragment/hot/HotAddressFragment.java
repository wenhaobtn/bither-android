/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.fragment.hot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView;

import net.bither.R;
import net.bither.adapter.hot.HotAddressFragmentListAdapter;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.AddressFragmentListItemView;
import net.bither.ui.base.AddressInfoChangedObserver;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.ui.base.PinnedHeaderAddressExpandableListView;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.util.BroadcastUtil;
import net.bither.util.LogUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;

public class HotAddressFragment extends Fragment implements Refreshable, Selectable {
    private HotAddressFragmentListAdapter mAdapter;
    private PinnedHeaderAddressExpandableListView lv;
    private View ivNoAddress;
    private List<BitherAddress> watchOnlys;
    private List<BitherAddressWithPrivateKey> privates;
    private boolean isLoading = false;
    private boolean isWalletReady = false;
    private SelectedThread selectedThread;
    private IntentFilter broadcastIntentFilter = new IntentFilter();
    private List<String> addressesToShowAdded;
    private String notifyAddress = null;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        broadcastIntentFilter.addAction(BroadcastUtil.ACTION_ADDRESS_STATE);
        broadcastIntentFilter.addAction(BroadcastUtil.ACTION_MARKET);
        watchOnlys = new ArrayList<BitherAddress>();
        privates = new ArrayList<BitherAddressWithPrivateKey>();
        List<BitherAddressWithPrivateKey> ps = WalletUtils.getPrivateAddressList();
        List<BitherAddress> ws = WalletUtils.getWatchOnlyAddressList();
        if (ws != null) {
            watchOnlys.addAll(ws);
        }
        if (ps != null) {
            privates.addAll(ps);
        }
    }

    public void refresh() {
        if (isWalletReady) {
            List<BitherAddressWithPrivateKey> ps = WalletUtils.getPrivateAddressList();
            List<BitherAddress> ws = WalletUtils.getWatchOnlyAddressList();
            watchOnlys.clear();
            privates.clear();
            if (ws != null) {
                watchOnlys.addAll(ws);
            }
            if (ps != null) {
                privates.addAll(ps);
            }
            if (watchOnlys.size() + privates.size() == 0) {
                ivNoAddress.setVisibility(View.VISIBLE);
                lv.setVisibility(View.GONE);
            } else {
                ivNoAddress.setVisibility(View.GONE);
                lv.setVisibility(View.VISIBLE);
            }
            mAdapter.notifyDataSetChanged();
            for (int i = 0;
                 i < mAdapter.getGroupCount();
                 i++) {
                lv.expandGroup(i);
            }
            if (notifyAddress != null) {
                scrollToAddress(notifyAddress);
            }
            lv.removeCallbacks(showAddressesAddedRunnable);
            if (addressesToShowAdded != null) {
                lv.postDelayed(showAddressesAddedRunnable, 600);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().registerReceiver(walletReadyReceiver, new IntentFilter(BroadcastUtil
                        .ACTION_ADDRESS_LOAD_COMPLETE_STATE)
        );
        View view = inflater.inflate(R.layout.fragment_hot_address, container, false);
        lv = (PinnedHeaderAddressExpandableListView) view.findViewById(R.id.lv);
        lv.setOnScrollListener(listScroll);
        mAdapter = new HotAddressFragmentListAdapter(getActivity(), watchOnlys, privates, lv);
        lv.setAdapter(mAdapter);
        ivNoAddress = view.findViewById(R.id.iv_no_address);
        return view;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(walletReadyReceiver);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        int listItemCount = lv.getChildCount();
        for (int i = 0;
             i < listItemCount;
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof AddressFragmentListItemView) {
                AddressFragmentListItemView av = (AddressFragmentListItemView) v;
                av.onResume();
            }
        }
        getActivity().registerReceiver(broadcastReceiver, broadcastIntentFilter);
    }

    @Override
    public void onPause() {
        int listItemCount = lv.getChildCount();
        for (int i = 0;
             i < listItemCount;
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof AddressFragmentListItemView) {
                AddressFragmentListItemView av = (AddressFragmentListItemView) v;
                av.onPause();
            }
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    public void scrollToAddress(final String address) {
        ArrayList<String> addresses = new ArrayList<String>();
        addresses.add(address);
        showAddressesAdded(addresses);
        doRefresh();
    }

    public void showAddressesAdded(List<String> addresses) {
        addressesToShowAdded = addresses;
        if (addressesToShowAdded == null || addressesToShowAdded.size() == 0) {
            DropdownMessage.showDropdownMessage(getActivity(),
                    getString(R.string.addresses_already_monitored));
        }
    }

    @Override
    public void doRefresh() {
        if (lv == null) {
            return;
        }
        if (lv.getFirstVisiblePosition() != 0) {
            lv.post(new SmoothScrollListRunnable(lv, 0, new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            }));
        } else {
            refresh();
        }
    }

    private void lvRefreshing() {
        lv.postDelayed(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, 150);
    }

    @Override
    public void onSelected() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        if ((privates == null || privates.size() == 0) && (watchOnlys == null || watchOnlys.size
                () == 0) && !isLoading) {
            if (lv != null) {
                lvRefreshing();
            } else {
                if (selectedThread == null || !selectedThread.isAlive()) {
                    selectedThread = new SelectedThread();
                    selectedThread.start();
                }
            }
        }
    }

    private class SelectedThread extends Thread {
        @Override
        public void run() {
            super.run();
            for (int i = 0;
                 i < 20;
                 i++) {
                if (lv != null) {
                    lvRefreshing();
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private Runnable showAddressesAddedRunnable = new Runnable() {
        @Override
        public void run() {
            if (addressesToShowAdded == null || addressesToShowAdded.size() == 0) {
                return;
            }
            if (watchOnlys != null && watchOnlys.size() > 0) {
                boolean isWatchOnly = false;
                int position = 0;
                for (int i = 0;
                     i < watchOnlys.size();
                     i++) {
                    if (StringUtil.compareString(watchOnlys.get(i).getAddress(),
                            addressesToShowAdded.get(0))) {
                        isWatchOnly = true;
                        position = i;
                        break;
                    }
                }
                if (isWatchOnly) {
                    int group = 1;
                    if (privates == null || privates.size() == 0) {
                        group = 0;
                    }
                    lv.expandGroup(group);
                    if (position == 0) {
                        lv.setSelection(lv.getFlatListPosition(ExpandableListView
                                .getPackedPositionForGroup(group)));
                    } else {
                        lv.setSelectionFromTop(lv.getFlatListPosition(ExpandableListView
                                .getPackedPositionForChild(group, position)), UIUtil.dip2pix(35));
                    }
                    final int g = group;
                    final int p = position;
                    lv.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            for (int i = 0;
                                 i < addressesToShowAdded.size();
                                 i++) {
                                int position = lv.getFlatListPosition(ExpandableListView
                                        .getPackedPositionForChild(g, p + i));
                                if (position >= lv.getFirstVisiblePosition() && position <= lv
                                        .getLastVisiblePosition()) {
                                    View v = lv.getChildAt(position - lv.getFirstVisiblePosition());
                                    v.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                            R.anim.address_notification));
                                }
                            }
                            addressesToShowAdded = null;
                        }
                    }, 400);
                    return;
                }
            }
            if (privates != null && privates.size() > 0) {
                boolean isPrivate = false;
                int position = 0;
                for (int i = 0;
                     i < privates.size();
                     i++) {
                    if (StringUtil.compareString(privates.get(i).getAddress(),
                            addressesToShowAdded.get(0))) {
                        position = i;
                        isPrivate = true;
                        break;
                    }
                }
                if (isPrivate) {
                    int group = 0;
                    lv.expandGroup(group);
                    if (position == 0) {
                        lv.setSelection(lv.getFlatListPosition(ExpandableListView
                                .getPackedPositionForGroup(group)));
                    } else {
                        lv.setSelectionFromTop(lv.getFlatListPosition(ExpandableListView
                                .getPackedPositionForChild(group, position)), UIUtil.dip2pix(35));
                    }
                    final int g = group;
                    final int p = position;
                    lv.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0;
                                 i < addressesToShowAdded.size();
                                 i++) {
                                int position = lv.getFlatListPosition(ExpandableListView
                                        .getPackedPositionForChild(g, p + i));
                                LogUtil.d("Anim", "anim position: " + position);
                                if (position >= lv.getFirstVisiblePosition() && position <= lv
                                        .getLastVisiblePosition()) {
                                    View v = lv.getChildAt(position - lv.getFirstVisiblePosition());
                                    v.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                            R.anim.address_notification));
                                }
                            }
                            addressesToShowAdded = null;
                        }
                    }, 400);
                    return;
                }
            }
            addressesToShowAdded = null;
        }
    };

    private BroadcastReceiver walletReadyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            isWalletReady = true;
            LogUtil.d("Wallet", "WalletReady");
            refresh();
        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // getString("","") is api 12
            String a = null;
            if (intent.hasExtra(BroadcastUtil.ACTION_ADDRESS_STATE)) {
                a = intent.getExtras().getString(BroadcastUtil.ACTION_ADDRESS_STATE);
            }

            if (intent.hasExtra(BroadcastUtil.ACTION_ADDRESS_ERROR)) {
                int errorCode = intent.getExtras().getInt(BroadcastUtil.ACTION_ADDRESS_ERROR);

                if (HandlerMessage.MSG_ADDRESS_NOT_MONITOR == errorCode) {
                    int id = R.string.address_monitor_failed_multiple_address;
                    DropdownMessage.showDropdownMessage(getActivity(), id);
                    doRefresh();

                }
            }
            int itemCount = lv.getChildCount();
            for (int i = 0;
                 i < itemCount;
                 i++) {
                View v = lv.getChildAt(i);
                if (v instanceof AddressInfoChangedObserver) {
                    AddressInfoChangedObserver o = (AddressInfoChangedObserver) v;
                    o.onAddressInfoChanged(a);
                }
                if (v instanceof MarketTickerChangedObserver) {
                    MarketTickerChangedObserver o = (MarketTickerChangedObserver) v;
                    o.onMarketTickerChanged();
                }
            }
        }
    };
    
    private OnScrollListener listScroll = new OnScrollListener() {
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            PinnedHeaderAddressExpandableListView v = (PinnedHeaderAddressExpandableListView) view;
            final long flatPos = v.getExpandableListPosition(firstVisibleItem);
            int groupPosition = ExpandableListView.getPackedPositionGroup(flatPos);
            int childPosition = ExpandableListView.getPackedPositionChild(flatPos);
            v.configureHeaderView(groupPosition, childPosition);
        }
    };
}
