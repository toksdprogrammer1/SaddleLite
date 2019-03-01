package com.netpluspay.saddlelite.utils;

import android.content.Context;
import android.text.TextUtils;

import java.util.Collection;

import smartpesa.sdk.devices.SpTerminal;

public class PreferredTerminalUtils {

    PreferenceHelper mPreferenceHelper;
    String DEFAULT_DEVICE;

    public PreferredTerminalUtils(Context context, String merchantCode, String operatorCode) {
        mPreferenceHelper = new PreferenceHelper(context);
        DEFAULT_DEVICE = merchantCode + operatorCode;
    }

    public void saveTerminal(SpTerminal spTerminal) {
        if (spTerminal != null) {
            mPreferenceHelper.putString(DEFAULT_DEVICE, spTerminal.getName());
        }
    }

    public String getPreferredTerminalName() {
        String defaultTerminal = mPreferenceHelper.getString(DEFAULT_DEVICE);
        if (TextUtils.isEmpty(defaultTerminal)) {
            return "";
        } else {
            return defaultTerminal;
        }
    }

    public SpTerminal matches(Collection<SpTerminal> collection) {
        String defaultTerminal = mPreferenceHelper.getString(DEFAULT_DEVICE);
        SpTerminal spTerminal = null;
        boolean found = false;
        if (!TextUtils.isEmpty(defaultTerminal) && !collection.isEmpty()) {
            for (final SpTerminal terminal : collection) {
                if (terminal.getName().equals(defaultTerminal)) {
                    spTerminal = terminal;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        } else {
            return null;
        }
        return spTerminal;
    }

    public void clearTerminal() {
        mPreferenceHelper.putString(DEFAULT_DEVICE, "");
    }
}
