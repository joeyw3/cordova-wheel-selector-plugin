/**
 */
package com.wellseek.cordova;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.wellseek.cordova.SelectorCordovaPlugin.SELECTOR_THEME;
import static com.wellseek.cordova.SelectorCordovaPlugin.setNumberPickerTextColor;

public class SelectorCordovaPlugin extends CordovaPlugin {
    public static final String TAG = "SelectorCordovaPlugin";
    public static final String BLANK_STRING = "";
    public static final String SPACE = " ";
    public static final int WIDTH = 50;
    public static final int HEIGHT = 50;

    public static boolean WHEEL_WRAP;
    public static final String LIGHT_THEME = "light";
    public static final String DARK_THEME = "dark";
    public static SelectorTheme SELECTOR_THEME = null;

    private static final String INDEX_KEY = "index";
    private static final String DISPLAY_ITEMS_KEY = "displayItems";
    private static final String DEFAULT_SELECTED_ITEMS_KEY = "defaultItems";
    private static final String DISPLAY_KEY = "displayKey";
    private static final String TITLE_KEY = "title";
    private static final String POSITIVE_BUTTON_TEXT_KEY = "positiveButtonText";
    private static final String NEGATIVE_BUTTON_TEXT_KEY = "negativeButtonText";
    private static final String WRAP_WHEEL_TEXT_KEY = "wrapWheelText";
    private static final String THEME_KEY = "theme";
    private static final String CHANGE_EVENT = "changeEvent";

    private CallbackContext callbackContext;
    private String displayKey;
    private List<PickerView> asFinal = new ArrayList<PickerView>();

    public boolean changeEvent;

    public static enum SelectorResultType {
        SelectorResultTypeDone,
        SelectorResultTypeChange
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        final CordovaInterface cordova = this.cordova; 

        Log.d(TAG, "************Showing Wheel Selector");
        final JSONObject options = args.getJSONObject(0);

        String config = args.getString(0);
        Log.d(TAG, "Config options: " + config);
        final JSONArray items = options.getJSONArray(DISPLAY_ITEMS_KEY);
        JSONObject tmpDefaultItemsMightNotBeSet = null;

        try {
            tmpDefaultItemsMightNotBeSet = options.getJSONObject(DEFAULT_SELECTED_ITEMS_KEY);
        }
        catch(JSONException je) {
            tmpDefaultItemsMightNotBeSet = null;
        }

        final JSONObject defaultSelectedItems = tmpDefaultItemsMightNotBeSet;

        if (action.equals("showSelector")) {
            final String title = options.getString(TITLE_KEY);
            final String positiveButton = options.getString(POSITIVE_BUTTON_TEXT_KEY);
            final String negativeButton = options.getString(NEGATIVE_BUTTON_TEXT_KEY);
            final String wrapSelectorText = options.getString(WRAP_WHEEL_TEXT_KEY);
            final String theme = options.getString(THEME_KEY);

            this.callbackContext = callbackContext;
            this.displayKey = options.getString(DISPLAY_KEY);
            this.changeEvent = options.getBoolean(CHANGE_EVENT);

            SelectorCordovaPlugin me = this;

            WHEEL_WRAP = Boolean.parseBoolean(wrapSelectorText);
            SELECTOR_THEME = new SelectorTheme(theme);

            //Log.d(TAG, "Config options: " + config);

            Runnable runnable = new Runnable() {
                public void run() {

                    AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity(), SELECTOR_THEME.getAlertBuilderTheme());
                    builder.setTitle(title);
                    builder.setCancelable(true);
                    List<PickerView> views = null;
                    try {
                        views = getPickerViews(cordova.getActivity(), items, defaultSelectedItems, me);
                    } catch (JSONException je) {
                        Log.v(TAG, "Exception: " + je.getMessage());
                    }

                    me.asFinal = views;
                    LinearLayout layout = new LinearLayout(cordova.getActivity());
                    layout.setOrientation(LinearLayout.HORIZONTAL);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WIDTH, HEIGHT);
                    params.gravity = Gravity.CENTER;
                    layout.setLayoutParams(params);

                    if (views != null) {
                        for (int i = 0; i < views.size(); ++i) {

                            layout.addView(views.get(i).getNumberPicker(), views.get(i).getLayoutParams());
                        }
                    } else {
                        Log.d(TAG, "error, views is null");
                    }

                    builder
                            .setCancelable(false)
                            .setPositiveButton(positiveButton,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            final PluginResult resultToReturnToJS = new PluginResult(PluginResult.Status.OK, (getResults(SelectorResultType.SelectorResultTypeDone)));
                                            callbackContext.sendPluginResult(resultToReturnToJS);
                                            dialog.dismiss();

                                        }
                                    })
                            .setNegativeButton(negativeButton,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            Log.d(TAG, "User canceled");
                                             callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                                            dialog.cancel();
                                        }
                                    });

                    builder.setView(layout);

                    AlertDialog alert = builder.create();

                    alert.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                    alert.show();
                }
            };

            this.cordova.getActivity().runOnUiThread(runnable);
        } else if (action.equals("updateItems") && this.asFinal.size() != 0) {
            List<PickerView> existingViews = this.asFinal;

            if(existingViews != null) {
                for (int i = 0; i < existingViews.size(); ++i) {
                    if(defaultSelectedItems != null && defaultSelectedItems.length() == items.length()){
                        try {
                            String defaultSelctedValue = defaultSelectedItems.getString(Integer.toString(i));
                            existingViews.get(i).setData(this.toStringArray(items.getJSONArray(i)), defaultSelctedValue);
                        } catch(JSONException je) {
                            existingViews.get(i).setData(this.toStringArray(items.getJSONArray(i)), "");
                        }
                    } else {
                        existingViews.get(i).setData(this.toStringArray(items.getJSONArray(i)), "");
                    }
                }
            }
        }

        return true;
    }

    public static List<PickerView> getPickerViews(Activity activity, JSONArray items, JSONObject defaultSelectedValues, SelectorCordovaPlugin selectorCordovaPlugin) throws JSONException {
        List<PickerView> views = new ArrayList<PickerView>();
        for (int i = 0; i < items.length(); ++i) {
            if(defaultSelectedValues != null && defaultSelectedValues.length() == items.length()){

                try {
                    String defaultSelctedValue = defaultSelectedValues.getString(Integer.toString(i));
                    views.add(new PickerView(activity, items.getJSONArray(i), defaultSelctedValue, selectorCordovaPlugin));
                }catch(JSONException je) {
                    views.add(new PickerView(activity, items.getJSONArray(i), "", selectorCordovaPlugin));
                }
            }else {
                views.add(new PickerView(activity, items.getJSONArray(i), "", selectorCordovaPlugin));
            }
        }
        return views;
    }

    public static String[] toStringArray(JSONArray array) {
        if (array == null)
            return null;

        String[] arr = new String[array.length()];
        for (int i = 0; i < arr.length; i++) {
            if (array.optString(i) != null && array.optString(i).equalsIgnoreCase(BLANK_STRING))
                arr[i] = SPACE;
            else
                arr[i] = array.optString(i);
        }
        return arr;
    }

    public static boolean setNumberPickerTextColor(NumberPicker numberPicker, int color) {
        float myTextSize = 10;
        try{
            if (android.os.Build.VERSION.SDK_INT >= 29){
                // API >= 29
                Method setColorMethod = numberPicker.getClass().getMethod("setTextColor", int.class);
                setColorMethod.invoke(numberPicker, color);
//                numberPicker.setTextColor(color);
//                numberPicker.setOutlineAmbientShadowColor(color);
            } else{
                // API < 29
                Field selectorWheelPaintField = numberPicker.getClass()
                        .getDeclaredField("mSelectorWheelPaint");
                selectorWheelPaintField.setAccessible(true);
                ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);
                numberPicker.invalidate();
            }
        } catch (NoSuchFieldException e) {
            System.out.println("setNumberPickerTextColor");
        } catch (IllegalAccessException e) {
            System.out.println("setNumberPickerTextColor");
        } catch (NoSuchMethodException e){
            System.out.println("setNumberPickerTextColor");
        }catch (InvocationTargetException e){
            System.out.println("setNumberPickerTextColor");
        }
        final int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    ((EditText) child).setTextColor(color);
                    //this setTextSize works, but given the 'mTextSize' variable is set in ctor
                    //the initial values are small, once activated they get larger
                    //https://android.googlesource.com/platform/frameworks/base.git/+/android-cts-4.2_r1/core/java/android/widget/NumberPicker.java
                    //((Paint) selectorWheelPaintField.get(numberPicker)).setTextSize(48);

//                    return true;
                }catch (IllegalArgumentException e) {
                    System.out.println("setNumberPickerTextColor");
                }
            }
        }
        return false;
    }

    public JSONObject getResults(SelectorResultType selectorResultType) {
        JSONObject result = new JSONObject();
        JSONArray userSelectedValues = new JSONArray();
        JSONObject jsonValue = null;

        try {
            result.put("type", selectorResultType == SelectorResultType.SelectorResultTypeDone ? "confirm" : "change");

            String value;

            for (int i = 0; i < this.asFinal.size(); ++i) {
                jsonValue = new JSONObject();

                value = this.asFinal.get(i).getDataToShow(this.asFinal.get(i).getNumberPicker().getValue());

                jsonValue.put(INDEX_KEY, this.asFinal.get(i).getNumberPicker().getValue());

                if(value != null && value.equalsIgnoreCase(SPACE)) {
                    jsonValue.put(this.displayKey, BLANK_STRING);
                } else {
                    jsonValue.put(this.displayKey, value);
                }

                userSelectedValues.put(jsonValue);
            }

            result.put("selection", userSelectedValues);
        } catch (JSONException je) {}

        return result;
    }

    public CallbackContext getCallbackContext() {
        return this.callbackContext;
    }
}


class PickerView {
    private String[] dataToShow;
    private String defaultSelectedItemValue;
    private Activity activity;
    private NumberPicker picker;
    private SelectorCordovaPlugin selectorCordovaPlugin;

    private LinearLayout.LayoutParams numPicerParams;

    public PickerView(Activity activity, JSONArray args, String defaulSelectedtItem, SelectorCordovaPlugin selectorCordovaPlugin) {
        dataToShow = SelectorCordovaPlugin.toStringArray(args);
        defaultSelectedItemValue = defaulSelectedtItem;
        this.activity = activity;
        this.selectorCordovaPlugin = selectorCordovaPlugin;
    }

    public NumberPicker getNumberPicker() {
        if (picker == null) {
            picker = new NumberPicker(activity);
            picker.setMinValue(0);
            picker.setMaxValue(dataToShow.length - 1);

            int index = -1;

            if(defaultSelectedItemValue != null && defaultSelectedItemValue.length() > 0)
                index = Arrays.asList(dataToShow).indexOf(defaultSelectedItemValue);

            if(index < 0)
                picker.setValue(0);
            else
                picker.setValue(index);

            picker.setDisplayedValues(dataToShow);
            picker.setWrapSelectorWheel(SelectorCordovaPlugin.WHEEL_WRAP);
            picker.setFocusable(false);

            picker.setFocusableInTouchMode(true);

            //turn off soft keyboard
            picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

            if(this.selectorCordovaPlugin.changeEvent) {
                PickerListener listener = new PickerListener(this.selectorCordovaPlugin);
                picker.setOnScrollListener(listener);
                picker.setOnValueChangedListener(listener);   
            }
            
            setNumberPickerTextColor(picker, SELECTOR_THEME.getNumberPickerTextColor());
        }

        return picker;
    }


    public LinearLayout.LayoutParams getLayoutParams() {
        if (numPicerParams == null) {
            numPicerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            numPicerParams.weight = 1;
        }
        return numPicerParams;
    }

    public String getDataToShow(int index) {
        return dataToShow[index];
    }

    public void setData(String[] dataToShow, String defaultSelectedItemValue) {
        int currentMaxValue = picker.getMaxValue();
        int newMaxValue = (dataToShow.length - 1);

        this.dataToShow = dataToShow;
        this.defaultSelectedItemValue = defaultSelectedItemValue;

        if(newMaxValue < currentMaxValue) {
            picker.setMaxValue(newMaxValue);
        }

        int index = -1;

        if(defaultSelectedItemValue != null && defaultSelectedItemValue.length() > 0) {
            index = Arrays.asList(dataToShow).indexOf(defaultSelectedItemValue);
        }

        if(index < 0) {
            picker.setValue(0);
        } else {
            picker.setValue(index);
        }

        picker.setDisplayedValues(dataToShow);

        if(newMaxValue > currentMaxValue) {
            picker.setMaxValue(newMaxValue);
        }
    }
}


class SelectorTheme {
    private String themeColors;

    public SelectorTheme(String theme) {
        themeColors = theme;
    }

    public int getNumberPickerTextColor() {
        if (themeColors.equalsIgnoreCase(SelectorCordovaPlugin.LIGHT_THEME)) {
            return Color.BLACK;
        }

        return Color.WHITE;
    }

    public int getAlertBuilderTheme() {
        if (themeColors.equalsIgnoreCase(SelectorCordovaPlugin.LIGHT_THEME)) {
            return android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        }

        return android.R.style.Theme_DeviceDefault_Dialog_Alert;
    }
}

class PickerListener implements NumberPicker.OnScrollListener, NumberPicker.OnValueChangeListener {
    private int lastScrollState = 0;
    private int selectedValue;
    private SelectorCordovaPlugin selectorCordovaPlugin;

    public PickerListener(SelectorCordovaPlugin selectorCordovaPlugin) {
        this.selectorCordovaPlugin = selectorCordovaPlugin;
    }

    @Override
    public void onScrollStateChange(NumberPicker view, int scrollState) {
        this.lastScrollState = scrollState;

        valueHasChanged();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        this.selectedValue = newVal;

        valueHasChanged();
    }

    public void valueHasChanged() {
        if(this.lastScrollState == SCROLL_STATE_IDLE) {
            final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, (selectorCordovaPlugin.getResults(SelectorCordovaPlugin.SelectorResultType.SelectorResultTypeChange)));

            pluginResult.setKeepCallback(true);

            selectorCordovaPlugin.getCallbackContext().sendPluginResult(pluginResult);
        }
    }
}