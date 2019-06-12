package aicare.net.cn.iweightdemo;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import aicare.net.cn.iweightdemo.scan.DeviceDialog;
import aicare.net.cn.iweightdemo.utils.T;
import aicare.net.cn.iweightlibrary.bleprofile.BleProfileService;
import aicare.net.cn.iweightlibrary.bleprofile.BleProfileServiceReadyActivity;
import aicare.net.cn.iweightlibrary.entity.AlgorithmInfo;
import aicare.net.cn.iweightlibrary.entity.BM09Data;
import aicare.net.cn.iweightlibrary.entity.BM15Data;
import aicare.net.cn.iweightlibrary.entity.BodyFatData;
import aicare.net.cn.iweightlibrary.entity.BroadData;
import aicare.net.cn.iweightlibrary.entity.DecimalInfo;
import aicare.net.cn.iweightlibrary.entity.User;
import aicare.net.cn.iweightlibrary.entity.WeightData;
import aicare.net.cn.iweightlibrary.utils.AicareBleConfig;
import aicare.net.cn.iweightlibrary.utils.L;
import aicare.net.cn.iweightlibrary.utils.ParseData;
import aicare.net.cn.iweightlibrary.wby.WBYService;

public class MyActivity extends BleProfileServiceReadyActivity implements DeviceDialog.OnDeviceScanListener, View.OnClickListener {

    private final static String TAG = "MyActivity";
    private Menu menu;
    private Toolbar toolbar;

    private WBYService.WBYBinder binder;

    private Button btn_sync_user;
    private RadioGroup rg_change_unit;

    private TextView tv_age, tv_height, tv_weight, tv_temp, text_view_weight;
    private SeekBar seek_bar_age, seek_bar_height, seek_bar_weight;

    private RadioGroup rg_sex;

    private ListView lv_data;
    private ArrayAdapter listAdapter;
    private List<String> dataList = new ArrayList<>();

    private User user = null;
    private byte unit = AicareBleConfig.UNIT_KG;

    private FloatingActionButton fab_log;

    private boolean showListView = false;

    private boolean isNewBM15TestData;

    private BroadData cacheBroadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initData();
        initViews();
        initEvents();

        L.isDebug = true;

        if (!ensureBLESupported()) {
            T.showShort(this, R.string.not_support_ble);
            finish();
        }
        if (!isBLEEnabled()) {
            showBLEDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.binder == null) {
            bindService(null);
        }
    }

    private void initData() {
        user = new User(1, 2, 28, 170, 768, 551);
    }

    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn_sync_user = (Button) findViewById(R.id.btn_sync_user);

        rg_change_unit = (RadioGroup) findViewById(R.id.rg_change_unit);
        rg_change_unit.check(R.id.rb_kg);

        tv_age = (TextView) findViewById(R.id.tv_age);
        setAgeText();

        tv_height = (TextView) findViewById(R.id.tv_height);
        setHeightText();

        tv_weight = (TextView) findViewById(R.id.tv_weight);
        tv_temp = (TextView) findViewById(R.id.tv_temp);

        text_view_weight = (TextView) findViewById(R.id.text_view_weight);
        setWeightText();

        seek_bar_age = (SeekBar) findViewById(R.id.seek_bar_age);
        seek_bar_age.setMax(82);
        seek_bar_age.setProgress(user.getAge() - 18);

        seek_bar_height = (SeekBar) findViewById(R.id.seek_bar_height);
        seek_bar_height.setMax(205);
        seek_bar_height.setProgress(user.getHeight() - 50);

        seek_bar_weight = (SeekBar) findViewById(R.id.seek_bar_weight);
        seek_bar_weight.setMax(1800);
        seek_bar_weight.setProgress(user.getWeight());

        rg_sex = (RadioGroup) findViewById(R.id.rg_sex);
        if (user.getSex() == 1) {
            rg_sex.check(R.id.rb_male);
        } else {
            rg_sex.check(R.id.rb_female);
        }

        lv_data = (ListView) findViewById(R.id.lv_data);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        lv_data.setAdapter(listAdapter);

        fab_log = (FloatingActionButton) findViewById(R.id.fab_log);
        showListView();
    }

    private void showListView() {
        lv_data.setVisibility(showListView ? View.VISIBLE : View.GONE);
        showListView = !showListView;
    }

    private void setWeightText() {
        text_view_weight.setText(getString(R.string.weight, String.valueOf(user.getWeight() / 10d)));
    }

    private void setHeightText() {
        tv_height.setText(getString(R.string.height, user.getHeight()));
    }

    private void setAgeText() {
        tv_age.setText(getString(R.string.age, user.getAge()));
    }

    private void initEvents() {
        btn_sync_user.setOnClickListener(this);

        rg_change_unit.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (isDeviceConnected()) {
                    switch (checkedId) {
                        case R.id.rb_kg:
                            unit = AicareBleConfig.UNIT_KG;
                            binder.syncUnit(AicareBleConfig.UNIT_KG);
                            break;
                        case R.id.rb_lb:
                            unit = AicareBleConfig.UNIT_LB;
                            binder.syncUnit(AicareBleConfig.UNIT_LB);
                            break;
                        case R.id.rb_st:
                            unit = AicareBleConfig.UNIT_ST;
                            binder.syncUnit(AicareBleConfig.UNIT_ST);
                            break;
                        case R.id.rb_jin:
                            unit = AicareBleConfig.UNIT_JIN;
                            binder.syncUnit(AicareBleConfig.UNIT_JIN);
                            break;
                    }
                }
            }
        });

        rg_sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_male:
                        user.setSex(1);
                        break;
                    case R.id.rb_female:
                        user.setSex(2);
                        break;
                }
            }
        });

        seek_bar_age.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                user.setAge(progress + 18);
                setAgeText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seek_bar_height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                user.setHeight(progress + 50);
                setHeightText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seek_bar_weight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                user.setWeight(progress);
                setWeightText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        fab_log.setOnClickListener(this);
    }

    private void setDefault() {
        tv_weight.setText(R.string.default_weight);
        tv_temp.setText(R.string.default_temp);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_log) {
            showListView();
            return;
        }

        switch (v.getId()) {
            case R.id.btn_sync_user:
                if (isDeviceConnected()) {
                    binder.syncUser(user);
                    break;
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                if (!isBLEEnabled()) {
                    showBLEDialog();
                } else {
                    if (isDeviceConnected()) {
                        binder.disconnect();
                    } else {
                        if (cacheBroadData == null) {
                            showDialog();
                            devicesDialog.startScan();
                        } else {
                            cacheBroadData = null;
                            setStateTitle("", BleProfileService.STATE_DISCONNECTED);
                            stopLeScan();
                        }
                    }
                }
                break;
        }

        return true;
    }


    private DeviceDialog devicesDialog;

    private void showDialog() {
        if (devicesDialog == null) {
            devicesDialog = new DeviceDialog(this, this);
            devicesDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    hideDialog();
                }
            });
        }
        devicesDialog.show();
    }

    private void hideDialog() {
        if (devicesDialog != null) {
            devicesDialog.dismiss();
            devicesDialog = null;
        }
    }

    @Override
    protected void onServiceBinded(WBYService.WBYBinder binder) {
        this.binder = binder;
    }

    @Override
    protected void onServiceUnbinded() {
        this.binder = null;
    }

    @Override
    protected void getAicareDevice(final BroadData broadData) {
        if (broadData != null) {
            L.e(TAG, broadData.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (devicesDialog != null && devicesDialog.isShowing()) {
                        devicesDialog.setDevice(broadData);
                    }
                    if (cacheBroadData != null && TextUtils.equals(cacheBroadData.getAddress(), broadData.getAddress())) {
                        if (broadData.getDeviceType() == AicareBleConfig.BM_09) {
                            if (broadData.getSpecificData() != null) {
                                BM09Data data = AicareBleConfig.getBm09Data(broadData.getAddress(), broadData.getSpecificData());
                                if (isNewData(data) && data.getWeight() != 0) {
                                    showInfo(data.toString(), false);
                                }
                            }
                        } else if (broadData.getDeviceType() == AicareBleConfig.BM_15) {
                            if (broadData.getSpecificData() != null) {
                                BM15Data data = AicareBleConfig.getBm15Data(broadData.getAddress(), broadData.getSpecificData());
                                WeightData weightData = new WeightData();
                                weightData.setWeight(data.getWeight());
                                weightData.setTemp(data.getTemp());
                                weightData.setAdc(data.getAdc());
                                weightData.setCmdType(data.getAgreementType());
                                weightData.setDeviceType(AicareBleConfig.BM_15);
                                switch (data.getUnitType()) {
                                    case 1:
                                    case 2:
                                    case 3:
                                        weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 2));
                                        break;
                                    case 4:
                                    case 5:
                                    case 6:
                                        weightData.setDecimalInfo(new DecimalInfo(2, 1, 1, 1, 1, 2));
                                        break;
                                }
                                onGetWeightData(weightData);
                            }
                        } else {
                            if (broadData.getSpecificData() != null) {
                                WeightData weightData = AicareBleConfig.getWeightData(broadData.getSpecificData());
                                onGetWeightData(weightData);
                            }
                        }
                    }
                }
            });
        }
    }

    private BM09Data bm09Data;

    private boolean isNewData(BM09Data data) {
        if (bm09Data == null) {
            bm09Data = data;
            return true;
        }
        if (bm09Data.getWeight() != data.getWeight()) {
            bm09Data = data;
            return true;
        }
        return false;
    }
//
//    private BM15Data bm15Data;
//
//    private boolean isNewBM15Data(BM15Data data) {
//        if (bm15Data == null) {
//            bm15Data = data;
//            return true;
//        }
//        if (bm15Data.getWeight() != data.getWeight() || bm15Data.getAdc() != data.getAdc() || bm15Data.getTemp() != data.getTemp() || bm15Data.getAgreementType() != data.getAgreementType()) {
//            bm15Data = data;
//            return true;
//        }
//        return false;
//    }

    @Override
    protected void onDestroy() {
        stopScan();
        if (isDeviceConnected()) {
            this.binder.disconnect();
        }
        super.onDestroy();
    }

    private void startLeScan() {
        startScan();
    }

    private void stopLeScan() {
        stopScan();
    }

    @Override
    public void scan() {
        startScan();
        devicesDialog.setScanning(true);
    }

    @Override
    public void stop() {
        stopScan();
        devicesDialog.setScanning(false);
    }

    @Override
    public void connect(BroadData device) {
        if (device.getDeviceType() == AicareBleConfig.TYPE_WEI_BROAD || device.getDeviceType() == AicareBleConfig.TYPE_WEI_TEMP_BROAD || device.getDeviceType() == AicareBleConfig.BM_09 || device.getDeviceType() == AicareBleConfig.BM_15) {
            cacheBroadData = device;
            showInfo(getString(R.string.state_bound, device.getAddress()), true);
            setStateTitle(device.getAddress(), -1);
            startLeScan();
        } else {
            startConnect(device.getAddress());
        }
    }

    @Override
    public void onStateChanged(String deviceAddress, int state) {
        super.onStateChanged(deviceAddress, state);
        switch (state) {
            case BleProfileService.STATE_CONNECTED:
                showInfo(getString(R.string.state_connected, deviceAddress), true);
                setStateTitle(deviceAddress, state);
                break;
            case BleProfileService.STATE_DISCONNECTED:
                showInfo(getString(R.string.state_disconnected), true);
                setStateTitle(deviceAddress, state);
                break;
            case BleProfileService.STATE_SERVICES_DISCOVERED:
                showInfo(getString(R.string.state_service_discovered), true);
                break;
            case BleProfileService.STATE_INDICATION_SUCCESS:
                showInfo(getString(R.string.state_indication_success), true);
                break;
            case BleProfileService.STATE_TIME_OUT:
                showInfo(getString(R.string.state_time_out), true);
                break;
            case BleProfileService.STATE_CONNECTING:
                showInfo(getString(R.string.state_connecting), true);
                break;
        }
    }

    private void showInfo(String str, boolean showToast) {
        if (showToast) showToast(str);
        String time = ParseData.getCurrentTime() + "\n----" + str;
        dataList.add(time);
        listAdapter.notifyDataSetChanged();
        lv_data.setSelection(dataList.size() - 1);
    }

    private void setStateTitle(final String deviceAddress, final int state) {
        switch (state) {
            case BleProfileService.STATE_CONNECTED:
                L.e(TAG, "STATE_CONNECTED");
                toolbar.setSubtitle(deviceAddress);
                menu.getItem(0).setTitle(R.string.disconnect);
                break;
            case BleProfileService.STATE_DISCONNECTED:
                L.e(TAG, "STATE_DISCONNECTED");
                toolbar.setSubtitle("");
                menu.getItem(0).setTitle(R.string.start_scan);
                setDefault();
                break;
            case -1:
                toolbar.setSubtitle(deviceAddress);
                menu.getItem(0).setTitle(R.string.unbound);
                break;
        }
    }

    @Override
    public void onError(final String errMsg, final int errCode) {
        L.e(TAG, "Message = " + errMsg + " errCode = " + errCode);
        showInfo(getString(R.string.state_error, errMsg, errCode), true);
    }

    @Override
    public void onGetWeightData(final WeightData weightData) {
        if (weightData == null) return;
        L.e(TAG, weightData.toString());
        setWeighDataText(AicareBleConfig.getWeight(weightData.getWeight(), unit, weightData.getDecimalInfo()));
        if (weightData.getTemp() != Double.MAX_VALUE) {
            tv_temp.setText(getString(R.string.temp, String.valueOf(weightData.getTemp())));
        }
        if (weightData.getDeviceType() == AicareBleConfig.BM_15) {
            if (weightData.getCmdType() != 3) {
                isNewBM15TestData = true;
                showInfo(weightData.toString(), false);
            }
            if (weightData.getCmdType() == 3 && weightData.getAdc() > 0 && isNewBM15TestData) {
                isNewBM15TestData = false;
                BodyFatData bodyFatData = AicareBleConfig.getBM15BodyFatData(weightData, user.getSex(), user.getAge(), user.getHeight());
                showInfo(bodyFatData.toString(), true);
            }
        }
    }

    private void setWeighDataText(String weight) {
        tv_weight.setText(getString(R.string.weight, weight));
    }

    @Override
    public void onGetSettingStatus(@AicareBleConfig.SettingStatus int status) {
        L.e(TAG, "SettingStatus = " + status);
        switch (status) {
            case AicareBleConfig.SettingStatus.NORMAL:
                showInfo(getString(R.string.settings_status, getString(R.string.normal)), true);
                break;
            case AicareBleConfig.SettingStatus.LOW_POWER:
                showInfo(getString(R.string.settings_status, getString(R.string.low_power)), true);
                break;
            case AicareBleConfig.SettingStatus.LOW_VOLTAGE:
                showInfo(getString(R.string.settings_status, getString(R.string.low_voltage)), true);
                break;
            case AicareBleConfig.SettingStatus.ERROR:
                showInfo(getString(R.string.settings_status, getString(R.string.error)), true);
                break;
            case AicareBleConfig.SettingStatus.TIME_OUT:
                showInfo(getString(R.string.settings_status, getString(R.string.time_out)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_UNIT_SUCCESS:
                showInfo(getString(R.string.settings_status, getString(R.string.set_unit_success)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_UNIT_FAILED:
                showInfo(getString(R.string.settings_status, getString(R.string.set_unit_failed)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_USER_SUCCESS:
                showInfo(getString(R.string.settings_status, getString(R.string.set_user_success)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_USER_FAILED:
                showInfo(getString(R.string.settings_status, getString(R.string.set_user_failed)), true);
                break;
            case AicareBleConfig.SettingStatus.ADC_MEASURED_ING:
                showInfo(getString(R.string.settings_status, getString(R.string.adc_measured_ind)), true);
                break;
            case AicareBleConfig.SettingStatus.ADC_ERROR:
                showInfo(getString(R.string.settings_status, getString(R.string.adc_error)), true);
                break;
        }
    }

    @Override
    public void onGetResult(final int index, final String result) {
        L.e(TAG, "index = " + index + "; result = " + result);
        switch (index) {
            case WBYService.BLE_VERSION:
                showInfo(getString(R.string.ble_version, result), true);
                break;
            case WBYService.USER_ID:
                showInfo(getString(R.string.user_id, result), true);
                break;
            case WBYService.MCU_DATE:
                showInfo(getString(R.string.mcu_date, result), true);
                break;
            case WBYService.MCU_TIME:
                showInfo(getString(R.string.mcu_time, result), true);
                break;
            case WBYService.ADC:
                showInfo(getString(R.string.adc, result), true);
                break;
        }
    }


    @Override
    public void onGetFatData(boolean isHistory, final BodyFatData bodyFatData) {
        L.e(TAG, "isHistory = " + isHistory + "; BodyFatData = " + bodyFatDataToString(bodyFatData));
        showInfo(getString(R.string.body_fat_data, bodyFatDataToString(bodyFatData)), true);
        seek_bar_weight.setProgress((int) (Double.valueOf(AicareBleConfig.getWeight(bodyFatData.getWeight(), AicareBleConfig.UNIT_KG, bodyFatData.getDecimalInfo())) * 10));
    }

    @Override
    protected void onGetDecimalInfo(DecimalInfo decimalInfo) {
        if (decimalInfo == null) return;
        L.e(TAG, decimalInfo.toString());
        String decimalStr = (getString(R.string.source_decimal, decimalInfo.getSourceDecimal()))
                + (getString(R.string.kg_decimal, decimalInfo.getKgDecimal()))
                + (getString(R.string.lb_decimal, decimalInfo.getLbDecimal()))
                + (getString(R.string.st_decimal, decimalInfo.getStDecimal()))
                + (getString(R.string.kg_graduation, decimalInfo.getKgGraduation()))
                + (getString(R.string.lb_graduation, decimalInfo.getLbGraduation()));
        showInfo(decimalStr, true);
    }

    @Override
    protected void onGetAlgorithmInfo(AlgorithmInfo algorithmInfo) {
        if (algorithmInfo == null) return;
        String algorithmStr = (getString(R.string.adc, String.valueOf(algorithmInfo.getAdc()))
                + (getString(R.string.algorithm_id, algorithmInfo.getAlgorithmId())));
        showInfo(algorithmStr, true);
    }

    @Override
    protected void bluetoothStateChanged(int state) {
        super.bluetoothStateChanged(state);
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                break;
            case BluetoothAdapter.STATE_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                break;
        }
    }

    private void showToast(String info) {
        T.showShort(this, info);
    }

    public String bodyFatDataToString(BodyFatData bodyFatData) {
        if (bodyFatData == null) {
            return "";
        }
        return "BodyFatData{date='" + bodyFatData.getDate() + '\'' + ", time='" + bodyFatData.getTime() + '\'' + ", weight=" + AicareBleConfig.getWeight(bodyFatData.getWeight(), unit, bodyFatData.getDecimalInfo())
                + ", bmi=" + bodyFatData.getBmi() + ", bfr=" + bodyFatData.getBfr() + ", sfr=" + bodyFatData.getSfr() + ", uvi=" + bodyFatData.getUvi()
                + ", rom=" + bodyFatData.getRom() + ", bmr=" + bodyFatData.getBmr() + ", bm=" + bodyFatData.getBm() + ", vwc=" + bodyFatData.getVwc()
                + ", bodyAge=" + bodyFatData.getBodyAge() + ", pp=" + bodyFatData.getPp() + ", number=" + bodyFatData.getNumber() + ", sex=" + bodyFatData.getSex()
                + ", age=" + bodyFatData.getAge() + ", height=" + bodyFatData.getHeight() + ", adc=" + bodyFatData.getAdc() + ", decimalInfo=" + bodyFatData.getDecimalInfo().toString() + '}';
    }
}
