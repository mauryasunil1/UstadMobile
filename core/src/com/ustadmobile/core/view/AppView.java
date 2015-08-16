/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */
package com.ustadmobile.core.view;

/**
 * Display methods that can be used in multiple areas: dialog, alerts, etc.
 * 
 * @author mike
 */
public interface AppView {
    
    /**
     * Short duration to show notification for - matches the android constant
     * 
     * http://developer.android.com/reference/android/widget/Toast.html#LENGTH_SHORT
     */
    public static final int LENGTH_SHORT = 0;
    
    /**
     * Long duration to show a notification for - matches the android constnat
     * 
     * http://developer.android.com/reference/android/widget/Toast.html#LENGTH_LONG
     */
    public static final int LENGTH_LONG = 1;
    
    /**
     * Show progress going on (e.g. show spinner) with title
     * 
     * @param title 
     */
    public abstract void showProgressDialog(String title);
    
    /**
     * Dismiss the progress dialog (if showing).  If this method is called
     * when no progress dialog is showing there is no effect/no error thrown
     * 
     * @return true if dialog was showing and is now hidden, false otherwise
     */
    public abstract boolean dismissProgressDialog();
    
    
    /**
     * Show an alert dialog
     * @param title Dialog window title
     * @param text Message text to show
     */
    public abstract void showAlertDialog(String title, String text);
    
    /**
     * Dismiss the alert dialog (this can be done by the user normally) if its
     * still showing
     */
    public abstract void dismissAlertDialog();
    
    /**
     * Show a short notification on top of the normal UI
     * @param text The text to show in the notification
     * @param length 
     */
    public abstract void showNotification(String text, int length);
    
}
