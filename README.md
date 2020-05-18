# Warning - 温馨提示

&emsp;

+ This SDK repository has been stopped for update and maintenance on 2020-05-01. To download and use the latest SDK, please click the following link:
 
	> [ElinkThings - Support - SDK Instructions - AIFit](http://elinkthings.com/en/help-default.html)




# Body fat scale SDK Instructions - Android 

[![](https://jitpack.io/v/inet2018/BodyFatScaleRepositoryAndroid.svg)](https://jitpack.io/#inet2018/BodyFatScaleRepositoryAndroid)

[aar package download link](https://github.com/inet2018/BodyFatScale_SDK_Demo_Android/releases)

[中文文档](README_CN.md)

This document is a guide for Android developers to integrate good figure-SDK-Android in Android 4.4 and above systems, mainly for some key usage examples

## 1 Import SDK


```
repositories {
    flatDir {
        dirs 'libs'
    }
}


Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency
	dependencies {
	        implementation 'com.github.inet2018:BodyFatScaleRepositoryAndroid:1.2.2'
	}


You can also use aar package dependency,Please download it into the project's libs yourself




```

## 2 permission settings

```
<!-In most cases, you need to ensure that the device supports BLE .-->
<uses-feature
    android: name = "android.hardware.bluetooth_le"
    android: required = "true" />

<uses-permission android: name = "android.permission.BLUETOOTH" />
<uses-permission android: name = "android.permission.BLUETOOTH_ADMIN" />

<!-Android 6.0 and above. Bluetooth scanning requires one of the following two permissions. You need to apply at run time .-->
<uses-permission android: name = "android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android: name = "android.permission.ACCESS_FINE_LOCATION" />

<!-Optional. If your app need dfu function .-->
<uses-permission android: name = "android.permission.INTERNET" />
```

> 6.0 and above systems need to locate permissions and need to obtain permissions manually

## 3 start integration

> Add below AndroidManifest.xml application tag
```
<application>
    ...

    <service android:name="aicare.net.cn.iweightlibrary.wby.WBYService"/>

</application>

```


> initialization

You can directly make your own `Activity` class extend` BleProfileServiceReadyActivity`

```
public class MyActivity extends BleProfileServiceReadyActivity

      @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        // Judge whether the mobile device supports Ble
        if (! ensureBLESupported ()) {
            T.showShort (this, R.string.not_support_ble);
            finish ();
        }
        // Judge whether there is positioning permission, this method is not encapsulated. The specific code can be obtained in the demo, or you can call the permission method in your own way.
        initPermissions ();
        // Judge whether Bluetooth is on, if you need to change the style, you can do it yourself
        if (! isBLEEnabled ()) {
            showBLEDialog ();
        }
    }
 
    
```

## 4 scan the device, stop scanning the device, check the scan status
The APIs related to scanning are as follows. For details, please refer to the BleProfileServiceReadyActivity class. For details, refer to the sample project.

```
  // Call the startScan method to start scanning
  startScan ();
  // The getAicareDevice (final BroadData broadData) interface will call back to get a body fat scale device that complies with the Aicare protocol
    @Override
    protected void getAicareDevice (BroadData broadData) {
               // Body fat scale equipment in compliance with Aicare protocol
               
      
    }
// Call the stopScan method to stop scanning. This convenience is not recommended for customers to call
 stopScan ();
// Call the isScanning method to see if it is scanning true: scanning; false: scanning stopped
 isScanning ();
```
> Note: If it is a broadcast scale, you do not need to perform the connection operation. You can directly get the body fat data in the scan callback getAicareDevice (BroadData broadData) method. The broadcast scale calls stopScan () to get back no data.

```
 @Override
    protected void getAicareDevice (BroadData broadData) {
        // Broadcast scale can get data directly here
        if (broadData.getDeviceType () == AicareBleConfig.BM_09) {
            if (broadData.getSpecificData ()! = null) {
                 BM09Data data = AicareBleConfig.
                       getBm09Data (broadData.getAddress (), broadData.getSpecificData ());
            } else if (broadData.getDeviceType () == AicareBleConfig.BM_15) {
                    if (broadData.getSpecificData ()! = null) {
                        BM15Data data = AicareBleConfig.getBm15Data (broadData.getAddress (),
                                broadData.getSpecificData ());
                    }
           } else {
                  if (broadData.getSpecificData ()! = null) {
                        WeightData weightData =
                                AicareBleConfig.getWeightData (broadData.getSpecificData ());
                        
                    }
           }
           
    }
```
      

## 5 connect the device, disconnect the device

The APIs related to the connection are as follows, please refer to the BleProfileServiceReadyActivity class for details.

```
// Call the startConnect method to connect the body fat scale device
// can be obtained in the getAicareDevice (BroadData broadData) callback method.
// If there is no filtering in getAicareDevice (BroadData broadData), the broadcast should be filtered when connecting, see the demo connection for details.
startConnect (String address)
// You can get the connection status in onStateChanged
@Override
    public void onStateChanged (String deviceAddress, int state) {
        super.onStateChanged (deviceAddress, state);
        // state See the class description for specific status
    }
// Call the disconnect method in WBYService.WBYBinder class to disconnect the body fat scale
 binder.disconnect ()
```

Use the `startConnect` method to connect to the body fat scale, use the` onStateChanged` method to monitor the status of the connection, and use the `onError` method to monitor for exceptions during the connection process, in order to facilitate additional processing and troubleshooting. Use the `isDeviceConnected` method to determine whether a connection has been established.

## 6 Successful connection, accept the data returned by the scale
The following methods or interfaces are automatically obtained directly after inheriting the BleProfileServiceReadyActivity class

```
// OnServiceBinded method to get an instance of WBYService.WBYBinder
    @Override
    protected void onServiceBinded (WBYService.WBYBinder binder) {
        this.binder = binder;
      
   }
 // The change and stable weight data and temperature returned by the device (supported by AC03)
    @Override
    protected void onGetWeightData (WeightData weightData) {
         // If you want to get broadcast scale data here. Need to be in getAicareDevice (final BroadData broadData) method
         // Call onGetWeightData (WeightData weightData) to pass the data through
         
         
    }
// Get the setting status in the onGetSettingStatus method. For details, see AicareBleConfig.SettingStatus
    @Override
    protected void onGetSettingStatus (int status) {

    }
// Get the version number, measurement time, user number, and impedance value in the onGetResul method
    @Override
    protected void onGetResult (int index, String result) {
          
          
    }
// Get the device to return historical data or body fat data. True is historical data.
//Body fat data will only be generated after the current user is synchronized after STATE_INDICATION_SUCCESS
    @Override
    protected void onGetFatData (boolean isHistory, BodyFatData bodyFatData) {

    }
// Get information about the number of decimal places returned by the device
    @Override
    protected void onGetDecimalInfo (DecimalInfo decimalInfo) {

    }
// Algorithm sequence information returned by the device
    @Override
    protected void onGetAlgorithmInfo (AlgorithmInfo algorithmInfo) {

    }
    

```
Note: Some of these interfaces or methods require APP to issue commands to body fat to return data.

## 7 Call the algorithm to calculate the data in the SDK
AicareBleConfig contains algorithms related to body fat data that can be called
```
If the device returns impedance, there is no body fat data that can be calculated by calling the getBodyFatData method, and the algorithm can be obtained by calling the algorithm on the data in the BodyFatData object.
as follows:
AicareBleConfig.getBodyFatData (AlgorithmUtil.AlgorithmType.TYPE_AIC
ARE, bodyFatData.getSex (), bodyFatData.getAge (),
Double.valueOf (ParseData.getKgWeight (bodyFatData.getWeight (),
bodyFatData.getDecimalInfo ())), bodyFatData .getHeight (),
bodyFatData.getAdc ());

If you need to obtain 6 additional physical indicators such as fat-free weight and weight control, please call getMoreFatData to get a MoreFatData object.
AicareBleConfig.getMoreFatData (int sex, int height, double weight,
double bfr, double rom, double pp)

```

## 8 Give instructions to the device
Get an instance of WBYService.WBYBinder in BleProfileServiceReadyActivity.onServiceBinded (WBYService.WBYBinder binder), and call the method in binder

```
    @Override
    protected void onServiceBinded (WBYService.WBYBinder binder) {
        this.binder = binder;
      
   }
   // If the history is obtained
      binder.syncHistory ();
      
      
      
   // Part of the method of WBYBinder
   public class WBYBinder extends LocalBinder {

        / **
         * Get history
         * /
        @Deprecated
        public void syncHistory () {
            mManager.sendCmd (AicareBleConfig.SYNC_HISTORY, AicareBleConfig.UNIT_KG);
        }

        / **
         * Synchronize the current user
         *
         * @param user
         * /
        public void syncUser (User user) {
            if (user == null) {
                return;
            }
            mManager.syncUser (user);
        }

        / **
         * Synchronized user list
         *
         * @param userList
         * /
        @Deprecated
        public void syncUserList (List <User> userList) {
            mManager.syncUserList (userList);
        }

        / **
         * Sync current unit
         *
         * @param unit {@link AicareBleConfig # UNIT_KG}
         * {@link AicareBleConfig # UNIT_LB}
         * {@link AicareBleConfig # UNIT_ST}
         * {@link AicareBleConfig # UNIT_JIN}
         * /
        public void syncUnit (byte unit) {
            mManager.sendCmd (AicareBleConfig.SYNC_UNIT, unit);
        }

        / **
         * synchronised time
         * /
        @Deprecated
        public void syncDate () {
            mManager.syncDate ();
        }

        / **
         * Query Bluetooth version information
         * /
        @Deprecated
        public void queryBleVersion () {
            mManager.sendCmd (AicareBleConfig.GET_BLE_VERSION, AicareBleConfig.UNIT_KG);
        }

        / **
         * Update user information
         *
         * @param user
         * /
        @Deprecated
        public void updateUser (User user) {
            if (user == null) {
                return;
            }
            mManager.updateUser (user);
        }

        / **
         * Setting mode
         * /
        public void setMode (@ AicareBleConfig.MODE int cmd) {
            mManager.setMode (cmd);
        }

        / **
         * Check if authorized
         * /
        @Deprecated
        public void auth () {
            mManager.auth ();
        }

        / **
         * Set DID
         *
         * @param did
         * /
        @Deprecated
        public void setDID (int did) {
            mManager.sendDIDCmd (AicareBleConfig.SET_DID, did);
        }

        / **
         * Query DID
         * /
        @Deprecated
        public void queryDID () {
            mManager.sendDIDCmd (AicareBleConfig.QUERY_DID, 0);
        }

        / **
         * Get the number of decimal places
         * /
        public void getDecimalInfo () {
            mManager.getDecimalInfo ();
        }
        
        ...
}
    
```

## 9 Class description

//aicare.net.cn.iweightlibrary.entity:

#### 1.AlgorithmInfo (Algorithm Sequence Information)

```
Type  Parameter  //Description
double weight   // weight 
int   algorithmId  //algorithm ID
int   adc         // impedance value
DecimalInfo decimalInfo //number of decimal places
```
####  2.BM09Data (BM09 data)

```
Type Parameter  //Description
int agreementType //agreement type
int unitType   //unit type
DecimalInfo decimalInfo //Decimal places
double weight //Weight
int adc //impedance value
double temp //temperature
int algorithmId //algorithm ID
int did //(currently useless)
String bleVersion //Bluetooth version
int bleType //Bluetooth type (0x09)
String address //device address
long timeMillis //measurement timestamp
whether boolean //isStable is stable

```
####  3.BM15Data (BM15 data)
```
Type Parameter name //Description
String version //Bluetooth version
int agreementType //agreementType
int unitType    //unitType 
double weight   // weight
int adc //impedance value
double temp //temperature (if temp = 6553.5, the scale does not support temperature)
int algorithmId //algorithm ID
int did //(currently useless)
int bleType// Bluetooth type (0x15)
String address// device address
```
####  4.BodyFatData
```
Type Parameter // Description
String date //measurement date
String time //time
double weight //weight
double bmi 
double bfr 
double sfr
int uvi //visceral fat index
double rom //muscle rate
double bmr //basal metabolic rate
double bm //bone mass
double vwc //moisture content
double bodyAge //
double pp //protein rate
int number
int sex
int age //(1; male; 2, female)
int height
int adc  //impedance value
```
####  5.BroadData (broadcast data)
```
Type Parameter  Description
String name //device name
String address //device address
boolean isBright //Whether the screen is bright
int rssi //signal value
byte [] specificData //broadcast data
int deviceType //device type
```
####  6.DecimalInfo (decimal point information)
```
Type Parameter  Description
int sourceDecimal // source data decimal places
int kgDecimal //kg number of decimal places
int lbDecimal //lb decimal places
int stDecimal //st number of decimal places
int kg //Graduation kg
int lb //Graduation lb
```
####  7.User (User Information)
```
Type Parameter name Description
int id
int sex
int age //(1; male; 2, female)
int height
int weight
int adc //impedance (deprecated)
```
####  8.WeightData (weight data)
```
Type Parameter name Description
int cmdType //command type (1, change; 2, stable; 3, in impedance measurement)
double weight
double temp //temperature (if the temperature is Double.MAX_VALUE, the scale does not support temperature)

DecimalInfo decimalInfo
int adc //impedance value
int algorithmType // algorithm ID
int unitType 
int deviceType //device type
```
#### 9.cn.net.aicare.algorithmutil.BodyFatData(Calculated body fat data)
```
Type Parameter name Description
double bmi;	Body mass index
double bfr;	 body fat rate
double sfr;	 Subcutaneous fat rate
int uvi;	Visceral fat index
double rom;  Rate of muscle
int bmr;  basal metabolic rate
double bm;  Bone Mass
double vwc; Water content
int bodyAge;  physical bodyAge
double pp;  protein percentage
```

#### 10.MoreFatData
```
Type Parameter name Description
double standardWeight;	Standard weight
double controlWeight;	Weight control
double fat;	Fat mass
double removeFatWeight;	Fat-free weight
double muscleMass; Muscle mass
double protein; Protein amount
MoreFatData.FatLevel fatLevel; Obesity grade
public static enum FatLevel {
        UNDER,  Underweight
        THIN,   Thin
        NORMAL,  standard
        OVER,  Favor
        FAT;  overweight
}
```
#### 11.BleProfileService Connection Status
```
public static final int STATE_CONNECTING = 4; // connecting
public static final int STATE_DISCONNECTED = 0; // disconnect
public static final int STATE_CONNECTED = 1; // The connection was successful
public static final int STATE_SERVICES_DISCOVERED = 2; // Discover services
public static final int STATE_INDICATION_SUCCESS = 3; // Enable success
public static final int STATE_TIME_OUT = 5; // connection timed out
```
#### 12.AicareBleConfig.SettingStatus Status information returned by the device
```
        int NORMAL = 0; // Normal
        int LOW_POWER = 1; // Low power
        int LOW_VOLTAGE = 2; // Low voltage
        int ERROR = 3; // overload
        int TIME_OUT = 4; // timeout
        int UNSTABLE = 5; // Unstable
        int SET_UNIT_SUCCESS = 6; // Set unit success
        int SET_UNIT_FAILED = 7; // Set unit failed
        int SET_TIME_SUCCESS = 8; // Successfully set time
        int SET_TIME_FAILED = 9; // Failed to set time
        int SET_USER_SUCCESS = 10; // Set user successfully
        int SET_USER_FAILED = 11; // Failed to set user
        int UPDATE_USER_LIST_SUCCESS = 12; // Update user list successfully
        int UPDATE_USER_LIST_FAILED = 13; // Update user list failed
        int UPDATE_USER_SUCCESS = 14; // Update user successfully
        int UPDATE_USER_FAILED = 15; // Update user failed
        int NO_HISTORY = 16; // There is no historical data
        int HISTORY_START_SEND = 17; // historical data starts to be sent
        int HISTORY_SEND_OVER = 18; // historical data transmission is complete
        int NO_MATCH_USER = 19; // No matching users
        int ADC_MEASURED_ING = 20; // Impedance measurement
        int ADC_ERROR = 21; // Impedance measurement failed
        int REQUEST_DISCONNECT = 22; // The device requested to disconnect
        int SET_DID_SUCCESS = 23; // DID set successfully
        int SET_DID_FAILED = 24; // Set DID failed
        int DATA_SEND_END = 25; // Measured data transmission is complete
        int UNKNOWN = -1; // unknown
```
#### 13.WBYService Bluetooth information returned by the device
```
    public final static int BLE_VERSION = 0; // Bluetooth version
    public final static int MCU_DATE = 1; // mcu date
    public final static int MCU_TIME = 2; // mcu time
    public final static int USER_ID = 3; // user number
    public final static int ADC = 4; // impedance value
```
