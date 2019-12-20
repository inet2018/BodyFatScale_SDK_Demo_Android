package aicare.net.cn.iweightdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import static aicare.net.cn.iweightdemo.R.string.weight;

public class MyActivity extends BleProfileServiceReadyActivity implements DeviceDialog.OnDeviceScanListener, View.OnClickListener {

    private final static String TAG = "MyActivity";
    private Menu menu;
    private Toolbar toolbar;
    private DeviceDialog devicesDialog;

    private WBYService.WBYBinder binder;

    private Button btn_sync_history, btn_sync_list, btn_sync_user, btn_sync_time, btn_version;
    private RadioGroup rg_change_unit;

    private TextView tv_age, tv_height, tv_weight, tv_temp, text_view_weight, tv_adc, tv_did;
    private SeekBar seek_bar_age, seek_bar_height, seek_bar_weight, seek_bar_adc;

    private RadioGroup rg_sex;

    private ListView lv_data;
    private ArrayAdapter listAdapter;
    private List<String> dataList = new ArrayList<>();

    private List<User> userList = new ArrayList<>();
    private User user = null;
    private byte unit = AicareBleConfig.UNIT_KG;

    private Button btn_query_did;

    private FloatingActionButton fab_log;
    private CoordinatorLayout coordinator_layout;

    private boolean showListView = false;

    private BroadData cacheBroadData;

    private boolean isNewBM15TestData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initData();
        initViews();
        initEvents();

        if (!ensureBLESupported()) {
            T.showShort(this, R.string.not_support_ble);
            finish();
        }
        initPermissions();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }
        devicesDialog = new DeviceDialog(this, this);
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
        userList.add(user);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle("V" + BuildConfig.VERSION_NAME);
        }

        coordinator_layout = findViewById(R.id.coordinator_layout);

        btn_sync_history = findViewById(R.id.btn_sync_history);
        btn_sync_list = findViewById(R.id.btn_sync_list);
        btn_sync_user = findViewById(R.id.btn_sync_user);
        btn_sync_time = findViewById(R.id.btn_sync_time);
        btn_version = findViewById(R.id.btn_version);

        rg_change_unit = findViewById(R.id.rg_change_unit);
        rg_change_unit.check(R.id.rb_kg);

        tv_age = findViewById(R.id.tv_age);
        setAgeText();

        tv_height = findViewById(R.id.tv_height);
        setHeightText();

        tv_weight = findViewById(R.id.tv_weight);
        tv_temp = findViewById(R.id.tv_temp);
        tv_did = findViewById(R.id.tv_did);

        text_view_weight = findViewById(R.id.text_view_weight);
        setWeightText();

        tv_adc = findViewById(R.id.tv_adc);
        setAdcText();

        seek_bar_age = findViewById(R.id.seek_bar_age);
        seek_bar_age.setMax(82);
        seek_bar_age.setProgress(user.getAge() - 18);

        seek_bar_height = findViewById(R.id.seek_bar_height);
        seek_bar_height.setMax(205);
        seek_bar_height.setProgress(user.getHeight() - 50);

        seek_bar_weight = findViewById(R.id.seek_bar_weight);
        seek_bar_weight.setMax(1800);
        seek_bar_weight.setProgress(user.getWeight());

        seek_bar_adc = findViewById(R.id.seek_bar_adc);
        seek_bar_adc.setMax(1000);
        seek_bar_adc.setProgress(user.getAdc());

        rg_sex = findViewById(R.id.rg_sex);
        if (user.getSex() == 1) {
            rg_sex.check(R.id.rb_male);
        } else {
            rg_sex.check(R.id.rb_female);
        }

        lv_data = findViewById(R.id.lv_data);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        lv_data.setAdapter(listAdapter);

        btn_query_did = findViewById(R.id.btn_query_did);

        fab_log = findViewById(R.id.fab_log);
        showListView();
    }

    private void showListView() {
        lv_data.setVisibility(showListView ? View.VISIBLE : View.GONE);
        showListView = !showListView;
    }

    private void setAdcText() {
        tv_adc.setText(getString(R.string.adc, String.valueOf(user.getAdc())));
    }

    private void setWeightText() {
        text_view_weight.setText(getString(weight, String.valueOf(user.getWeight() / 10d)));
    }

    private void setHeightText() {
        tv_height.setText(getString(R.string.height, user.getHeight()));
    }

    private void setAgeText() {
        tv_age.setText(getString(R.string.age, user.getAge()));
    }

    private void initEvents() {
        btn_sync_history.setOnClickListener(this);
        btn_sync_list.setOnClickListener(this);
        btn_sync_user.setOnClickListener(this);
        btn_sync_time.setOnClickListener(this);
        btn_version.setOnClickListener(this);

        btn_query_did.setOnClickListener(this);

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

        seek_bar_adc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                user.setAdc(progress);
                setAdcText();
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
        tv_did.setText(R.string.default_DID);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_log) {
            showListView();
            return;
        }
        if (isDeviceConnected()) {
            switch (v.getId()) {
                case R.id.btn_sync_history:
                    binder.syncHistory();
                    break;
                case R.id.btn_sync_list:
                    binder.syncUserList(userList);
                    break;
                case R.id.btn_sync_user:
                    binder.syncUser(user);
                    break;
                case R.id.btn_sync_time:
                    binder.syncDate();
                    break;


                case R.id.btn_query_did:
                    binder.queryDID();
                    break;
                case R.id.btn_version:
                    binder.queryBleVersion();
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
                            devicesDialog.show();
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

    @Override
    protected void onServiceBinded(WBYService.WBYBinder binder) {
        this.binder = binder;
        L.e("2017-11-20", TAG + ", onServiceBinded: binder = " + binder);
    }

    @Override
    protected void onServiceUnbinded() {
        this.binder = null;
        L.e("2017-11-20", TAG + ", onServiceUnbinded");
    }

    @Override
    protected void getAicareDevice(final BroadData broadData) {
        if (broadData != null) {
            L.e(TAG, broadData.toString());
            if (devicesDialog.isShowing()) {
                devicesDialog.setDevice(broadData);
            }
            if (cacheBroadData != null && TextUtils
                    .equals(cacheBroadData.getAddress(), broadData.getAddress())) {
                if (broadData.getDeviceType() == AicareBleConfig.BM_09) {
                    if (broadData.getSpecificData() != null) {
                        BM09Data data = AicareBleConfig
                                .getBm09Data(broadData.getAddress(), broadData.getSpecificData());
                        if (isNewData(data) && data.getWeight() != 0) {
                            showInfo(data.toString(), false);
                            tv_did.setText("DID:" + data.getDid());
                        }
                    }
                } else if (broadData.getDeviceType() == AicareBleConfig.BM_15) {
                    if (broadData.getSpecificData() != null) {
                        rg_change_unit.setOnCheckedChangeListener(null);
                        BM15Data data = AicareBleConfig
                                .getBm15Data(broadData.getAddress(), broadData.getSpecificData());
                        WeightData weightData = new WeightData();
                        weightData.setAdc(data.getAdc());
                        weightData.setCmdType(data.getAgreementType());
                        weightData.setDeviceType(broadData.getDeviceType());
                        switch (data.getUnitType()) {
                            case 1:
                                unit = AicareBleConfig.UNIT_KG;
                                rg_change_unit.check(R.id.rb_kg);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 2));
                                break;
                            case 2:
                                unit = AicareBleConfig.UNIT_LB;
                                rg_change_unit.check(R.id.rb_lb);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 2));
                                break;
                            case 3:
                                unit = AicareBleConfig.UNIT_ST;
                                rg_change_unit.check(R.id.rb_st);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 2));
                                break;
                            case 4:
                                unit = AicareBleConfig.UNIT_KG;
                                rg_change_unit.check(R.id.rb_kg);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 1));
                                break;
                            case 5:
                                unit = AicareBleConfig.UNIT_LB;
                                rg_change_unit.check(R.id.rb_lb);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 1));
                                break;
                            case 6:
                                unit = AicareBleConfig.UNIT_ST;
                                rg_change_unit.check(R.id.rb_st);
                                weightData.setDecimalInfo(new DecimalInfo(1, 1, 1, 1, 1, 1));
                                break;
                        }
                        weightData.setWeight(data.getWeight());
                        weightData.setTemp(data.getTemp());
                        onGetWeightData(weightData);

                        if (isNewData(data) && data.getWeight() != 0) {
                            tv_did.setText("DID:" + data.getDid());
                        }
                    }
                } else {
                    if (broadData.getSpecificData() != null) {
                        WeightData weightData = AicareBleConfig
                                .getWeightData(broadData.getSpecificData());
                        onGetWeightData(weightData);
                    }
                }
            }
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


    private BM15Data mBM15Data;

    private boolean isNewData(BM15Data data) {
        if (mBM15Data == null) {
            mBM15Data = data;
            return true;
        }
        if (mBM15Data.getWeight() != data.getWeight()) {
            mBM15Data = data;
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        stopScan();
        if (isDeviceConnected()) {
            this.binder.disconnect();
        }
        super.onDestroy();
    }

    private Handler handler = new Handler();

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
        if (device.getDeviceType() == AicareBleConfig.TYPE_WEI_BROAD || device
                .getDeviceType() == AicareBleConfig.TYPE_WEI_TEMP_BROAD || device
                .getDeviceType() == AicareBleConfig.BM_09 || device
                .getDeviceType() == AicareBleConfig.BM_15) {
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

    private void showInfo(String str, boolean showSnackBar) {
        if (showSnackBar)
            showSnackBar(str);
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
        if (weightData == null)
            return;
        setWeighDataText(AicareBleConfig
                .getWeight(weightData.getWeight(), unit, weightData.getDecimalInfo()));
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
                BodyFatData bm15BodyFatData = AicareBleConfig
                        .getBM15BodyFatData(weightData, user.getSex(), user.getAge(), user
                                .getHeight());
                showInfo(bm15BodyFatData.toString(), true);
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
                showInfo(getString(R.string.settings_status, getString(R.string.low_voltage)),
                        true);
                break;
            case AicareBleConfig.SettingStatus.ERROR:
                showInfo(getString(R.string.settings_status, getString(R.string.error)), true);
                break;
            case AicareBleConfig.SettingStatus.TIME_OUT:
                showInfo(getString(R.string.settings_status, getString(R.string.time_out)), true);
                break;
            case AicareBleConfig.SettingStatus.UNSTABLE:
                showInfo(getString(R.string.settings_status, getString(R.string.unstable)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_UNIT_SUCCESS:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.set_unit_success)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_UNIT_FAILED:
                showInfo(getString(R.string.settings_status, getString(R.string.set_unit_failed))
                        , true);
                break;
            case AicareBleConfig.SettingStatus.SET_TIME_SUCCESS:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.set_time_success)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_TIME_FAILED:
                showInfo(getString(R.string.settings_status, getString(R.string.set_time_failed))
                        , true);
                break;
            case AicareBleConfig.SettingStatus.SET_USER_SUCCESS:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.set_user_success)), true);
                break;
            case AicareBleConfig.SettingStatus.SET_USER_FAILED:
                showInfo(getString(R.string.settings_status, getString(R.string.set_user_failed))
                        , true);
                break;
            case AicareBleConfig.SettingStatus.UPDATE_USER_LIST_SUCCESS:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.update_user_list_success)), true);
                break;
            case AicareBleConfig.SettingStatus.UPDATE_USER_LIST_FAILED:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.update_user_list_failed)), true);
                break;
            case AicareBleConfig.SettingStatus.UPDATE_USER_SUCCESS:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.update_user_success)), true);
                break;
            case AicareBleConfig.SettingStatus.UPDATE_USER_FAILED:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.update_user_failed)), true);
                break;
            case AicareBleConfig.SettingStatus.NO_HISTORY:
                showInfo(getString(R.string.settings_status, getString(R.string.no_history)), true);
                break;
            case AicareBleConfig.SettingStatus.HISTORY_START_SEND:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.history_start_send)), true);
                break;
            case AicareBleConfig.SettingStatus.HISTORY_SEND_OVER:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.history_send_over)), true);
                break;
            case AicareBleConfig.SettingStatus.NO_MATCH_USER:
                showInfo(getString(R.string.settings_status, getString(R.string.no_match_user)),
                        true);
                break;
            case AicareBleConfig.SettingStatus.ADC_MEASURED_ING:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.adc_measured_ind)), true);
                break;
            case AicareBleConfig.SettingStatus.ADC_ERROR:
                showInfo(getString(R.string.settings_status, getString(R.string.adc_error)), true);
                break;
            case AicareBleConfig.SettingStatus.UNKNOWN:
                showInfo(getString(R.string.settings_status, getString(R.string.unknown)), true);
                break;
            case AicareBleConfig.SettingStatus.REQUEST_DISCONNECT:
                showInfo(getString(R.string.settings_status,
                        getString(R.string.request_disconnect)), true);
                break;

            case AicareBleConfig.SettingStatus.DATA_SEND_END:
                showInfo(getString(R.string.settings_status, getString(R.string.data_send_end)),
                        true);
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
        L.e(TAG, "isHistory = " + isHistory + "; BodyFatData = " + bodyFatData.toString());
        if (isHistory) {
            showInfo(getString(R.string.history_data, bodyFatData.toString()), true);
        } else {
            showInfo(getString(R.string.body_fat_data, bodyFatData.toString()), true);
            seek_bar_weight.setProgress((int) (Double.valueOf(AicareBleConfig
                    .getWeight(bodyFatData.getWeight(), AicareBleConfig.UNIT_KG, bodyFatData
                            .getDecimalInfo())) * 10));
            if (bodyFatData.getAdc() != 0) {
                seek_bar_adc.setProgress(bodyFatData.getAdc());
            }
            if (isDeviceConnected() && bodyFatData.getAdc() != 0) {
                /*userList.clear();
                userList.add(user);*/
                handler.postDelayed(updateRunnable, 50);
            }
        }
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            binder.updateUser(user);
        }
    };


    @Override
    public void onGetDID(int did) {
        showInfo(getString(R.string.did, did), true);
        tv_did.setText("DID:" + did);
    }

    @Override
    protected void onGetDecimalInfo(DecimalInfo decimalInfo) {
        if (decimalInfo == null)
            return;
        L.e(TAG, decimalInfo.toString());
        String decimalStr = (getString(R.string.source_decimal, decimalInfo
                .getSourceDecimal())) + (getString(R.string.kg_decimal, decimalInfo
                .getKgDecimal())) + (getString(R.string.lb_decimal, decimalInfo
                .getLbDecimal())) + (getString(R.string.st_decimal, decimalInfo
                .getStDecimal())) + (getString(R.string.kg_graduation, decimalInfo
                .getKgGraduation())) + (getString(R.string.lb_graduation, decimalInfo
                .getLbGraduation()));
        showInfo(decimalStr, true);
    }

    @Override
    protected void onGetAlgorithmInfo(AlgorithmInfo algorithmInfo) {
        if (algorithmInfo == null)
            return;
        String algorithmStr = (getString(R.string.adc, String
                .valueOf(algorithmInfo.getAdc())) + (getString(R.string.algorithm_id, algorithmInfo
                .getAlgorithmId())));
        showInfo(algorithmStr, true);
    }


    private void showSnackBar(String info) {
        Snackbar snackbar = Snackbar.make(coordinator_layout, info, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }


    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat
                    .requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 1) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                new AlertDialog.Builder(this).setTitle(R.string.tips).setMessage(R.string.tips_hint)
                        .setPositiveButton(R.string.query, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getApplicationContext()
                                        .getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (dialog != null) {
                                    dialog.cancel();
                                }

                            }
                        }).show();
            } else {
                new AlertDialog.Builder(this).setTitle(R.string.tips).setMessage(R.string.tips_hint)
                        .setPositiveButton(R.string.query, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getApplicationContext()
                                        .getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (dialog != null) {
                                    dialog.cancel();
                                }

                            }
                        }).show();
            }

        }

    }

}
