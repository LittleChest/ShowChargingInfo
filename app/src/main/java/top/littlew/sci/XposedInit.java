package top.littlew.sci;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private static float max_charging_current_ua = 0;
    private static float max_charging_voltage_uv = 0;
    private static float temperature_decicelsius = 0;

    private static final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                temperature_decicelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                max_charging_current_ua = intent.getIntExtra("max_charging_current", 0);
                max_charging_voltage_uv = intent.getIntExtra("max_charging_voltage", 0);
            }
        }
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIApplication",
                lpparam.classLoader,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        mContext = (Context) param.thisObject;
                            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                            mContext.registerReceiver(batteryInfoReceiver, filter);
                    }
                }
            );

            Class<?> keyguardIndicationControllerClass = XposedHelpers.findClass("com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                    keyguardIndicationControllerClass,
                    "computePowerIndication",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String originalIndication = (String) param.getResult();
                            if (originalIndication == null) {
                                originalIndication = "";
                            }

                            float current_a = max_charging_current_ua / 1000000f;
                            float voltage_v = max_charging_voltage_uv / 1000000f;
                            float power_w = current_a * voltage_v;
                            float temp_c = temperature_decicelsius / 10f;

                            String detailedChargeInfo;
                            boolean hasValidPowerData = max_charging_current_ua > 0 && max_charging_voltage_uv > 0;

                            if (hasValidPowerData) {
                                    detailedChargeInfo = String.format(java.util.Locale.US,
                                            "%s\n%.1fW (%.2fV, %.2fA) • %.1f°C",
                                            originalIndication.trim(), power_w, voltage_v, current_a, temp_c);
                                param.setResult(detailedChargeInfo.trim());
                            } else {
                                if (temperature_decicelsius != 0) {
                                     String tempInfo;
                                         tempInfo = String.format(java.util.Locale.US, " • %.1f°C", temp_c);
                                     if (!originalIndication.matches(".*[0-9]+(\\.[0-9]+)?°[CF].*")) {
                                          param.setResult(originalIndication.trim() + tempInfo);
                                     } else {
                                         param.setResult(originalIndication);
                                     }
                                } else {
                                    param.setResult(originalIndication);
                                }
                            }
                        }
                    }
            );
    }
}
