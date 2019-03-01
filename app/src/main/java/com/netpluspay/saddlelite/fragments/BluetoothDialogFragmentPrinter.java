package com.netpluspay.saddlelite.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.utils.PreferredTerminalUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import smartpesa.sdk.ServiceManager;
import smartpesa.sdk.devices.SpDevice;
import smartpesa.sdk.devices.SpTerminal;

public class BluetoothDialogFragmentPrinter<T extends SpDevice> extends BaseDialogFragment {

    @Bind(R.id.cancel_btn) Button cancelBtn;
    @Bind(R.id.list) ListView list;
    @Bind(R.id.preferredTerminalCB) CheckBox preferredCB;
    @Bind(R.id.bluetoothTitleTv) TextView bluetoothTitleTv;

    protected TerminalSelectedListener<T> mListener;
    protected BluetoothAdapter<T> adapter;
    protected List<T> data = new ArrayList<>();
    PreferredTerminalUtils mPreferredTerminalUtils;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.dialog_fragment_bluetooth_list_printer, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setCancelable(false);
        return d;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        /*if (getMerchantComponent(getActivity()) != null) {
            VerifiedMerchantInfo merchantComponent = getMerchantComponent(getActivity()).provideMerchant();
            if (merchantComponent != null) {
                mPreferredTerminalUtils = new PreferredTerminalUtils(getActivity(), merchantComponent.getMerchantCode(), merchantComponent.getOperatorCode());
            }
        }*/

        adapter = new BluetoothAdapter<>(getActivity(), data);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onSelected(data.get(position));

                //check if the checkbox is checked, if so save as preferred terminal
                if (preferredCB.isChecked()) {
                    mPreferredTerminalUtils.saveTerminal((SpTerminal) data.get(position));
                }

                dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                ServiceManager.get(getActivity()).stopScan();
                mListener.onCancelled();
                getActivity().finish();
            }
        });
    }

    public void updateDevices(Collection<T> devices) {
        data.clear();
        data.addAll(devices);

        ObjectComparator comparator = new ObjectComparator();
        Collections.sort(data, comparator);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public class ObjectComparator implements Comparator<T> {
        public int compare(T obj1, T obj2) {
            return obj1.getName().compareTo(obj2.getName());
        }
    }

    public void setSelectedListener(TerminalSelectedListener<T> listener) {
        mListener = listener;
    }

    public interface TerminalSelectedListener<T> {
        void onSelected(T device);
        void onCancelled();
    }

    protected static class BluetoothAdapter<T extends SpDevice> extends ArrayAdapter<T> {

        public BluetoothAdapter(Context context, List<T> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ((TextView) view).setText(getItem(position).getName());
            return view;
        }
    }
}
