
# 蓝牙体脂秤SDK使用说明 - Android

[![](https://jitpack.io/v/inet2018/BodyFatScaleRepositoryAndroid.svg)](https://jitpack.io/#inet2018/BodyFatScaleRepositoryAndroid)

[aar包下载地址](https://github.com/inet2018/BodyFatScale_SDK_Demo_Android/releases)

[English documentation](README.md)

该文档为指导Android开发人员在Android 4.4及以上系统中集成好身材-SDK-Android，主要为一些关键的使用示例

## 一、导入SDK


```
repositories {
    flatDir {
        dirs 'libs'
    }
}


步骤1.将JitPack存储库添加到您的构建文件中
将其添加到存储库末尾的root build.gradle中：
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

步骤2.添加依赖项
	dependencies {
	        implementation 'com.github.inet2018:BodyFatScaleRepositoryAndroid:1.2.1'
	}


也可以使用aar包依赖,请自行下载放到项目的libs中



```

## 二、权限设置

```
<!--In most cases, you need to ensure that the device supports BLE.-->
<uses-feature
    android:name="android.hardware.bluetooth_le"
    android:required="true"/>

<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

<!--Android 6.0 and above. Bluetooth scanning requires one of the following two permissions. You need to apply at run time.-->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

<!--Optional. If your app need dfu function.-->
<uses-permission android:name="android.permission.INTERNET"/>
```

>  6.0及以上系统必须要定位权限，且需要手动获取权限

## 三、初始化

你可以直接让你自己的`Activity`类继承`BleProfileServiceReadyActivity`

```
public class MyActivity extends BleProfileServiceReadyActivity

```

```
      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //判断手机设备是否支持Ble
        if (!ensureBLESupported()) {
            T.showShort(this, R.string.not_support_ble);
            finish();
        }
        //判断是否有定位权限,此方法没有进行封装具体代码可在demo中获得，也可按照自己方式去调用请求权限方法
        initPermissions();
        //判断蓝牙是否打开，若需要换样式，可自己去实现
        if (!isBLEEnabled()) {
            showBLEDialog();
        }
    }


```

## 四、扫描设备，停止扫描设备,查看扫描状态
与扫描相关的API如下，详情参考BleProfileServiceReadyActivity类，具体使用参考sample工程

```
  //调用startScan方法开启扫描
  startScan();
  //getAicareDevice(final BroadData broadData)接口会回调获取到符合Aicare协议的体脂秤设备
    @Override
    protected void getAicareDevice(BroadData broadData) {
               //符合Aicare协议的体脂秤设备


    }
//调用stopScan方法停止扫描 该方便不建议客户自己调用
 stopScan();
//调用isScanning方法查看是否在扫描 true:正在扫描; false:已停止扫描
 isScanning();

```
> 注意： 如果为广播秤，不需要去执行连接操作，在扫描的回调getAicareDevice(BroadData broadData)方法中可以直接去获取到体脂数据,广播秤调用 stopScan()回获取不到数据

```
 @Override
    protected void getAicareDevice(BroadData broadData) {
        //广播秤可以直接在此获取数据
        if (broadData.getDeviceType() == AicareBleConfig.BM_09) {
            if (broadData.getSpecificData() != null) {
                 BM09Data data = AicareBleConfig.
                       getBm09Data(broadData.getAddress(),broadData.getSpecificData());
            }else if (broadData.getDeviceType() == AicareBleConfig.BM_15) {
                    if (broadData.getSpecificData() != null) {
                        BM15Data data = AicareBleConfig.getBm15Data(broadData.getAddress(),
                                broadData.getSpecificData());
                    }
           }else{
                  if (broadData.getSpecificData() != null) {
                        WeightData weightData =
                                AicareBleConfig.getWeightData(broadData.getSpecificData());

                    }
           }

    }
```


## 五、连接设备，断开设备

与连接相关的API如下，详情参考BleProfileServiceReadyActivity类，具体使用参考sample工程。

```
//调用startConnect方法去连接体脂秤设备 需要传入体脂秤设备的mac地址 mac地址
//可以在getAicareDevice（BroadData broadData）回调方法中获得 。
//如果在getAicareDevice(BroadData broadData) 没有做过滤，连接时要去过滤掉广播称，详情见demo连接处
startConnect(String address)
//在onStateChanged可以获取到连接状态
@Override
    public void onStateChanged(String deviceAddress, int state) {
        super.onStateChanged(deviceAddress, state);
        //state 具体状态看类说明
    }
//调用WBYService.WBYBinder类中disconnect方法去断开体脂秤
 binder.disconnect()
```

使用`startConnect`方法连接体脂秤，，使用`onStateChanged`方法监听连接的状态，使用`onError`方法监听连接过程中的异常，以便于进行额外的处理和问题排查。使用`isDeviceConnected`方法判断连接是否已经建立。

## 六 连接成功，接受秤返回的数据
以下方法或接口可直接在继承BleProfileServiceReadyActivity类后自动获得

```
//onServiceBinded方法中获得WBYService.WBYBinder的实例
    @Override
    protected void onServiceBinded(WBYService.WBYBinder binder) {
        this.binder = binder;

   }
 //设备返回的变化和稳定的体重数据和温度(AC03才支持)
    @Override
    protected void onGetWeightData(WeightData weightData) {
         //如果想要在这里获取到广播秤数据。需要在getAicareDevice(final BroadData broadData)方法中
         //调用onGetWeightData(WeightData weightData)去把数据透传过来


    }
//onGetSettingStatus方法中获得设置状态 详情查看AicareBleConfig.SettingStatus
    @Override
    protected void onGetSettingStatus(int status) {

    }
//onGetResul方法中获得获得版本号，测量时间，用户编号，阻抗值
    @Override
    protected void onGetResult(int index, String result) {


    }
// 获得设备返回据历史数据或体脂数据  true为历史数据
    @Override
    protected void onGetFatData(boolean isHistory, BodyFatData bodyFatData) {

    }
//获取设备返回的小数点位数信息
    @Override
    protected void onGetDecimalInfo(DecimalInfo decimalInfo) {

    }
//设备返回的算法序列信息
    @Override
    protected void onGetAlgorithmInfo(AlgorithmInfo algorithmInfo) {

    }


```
> 注意：这些接口或方法部分需要APP给体脂下发命令才会有返回数据.

## 七 调用SDK中的算法计算数据
在AicareBleConfig中包含有体脂数据相关的算法可供调用
```
如果设备返回阻抗,没有体脂数据可以调用getBodyFatData方法计算,通过 BodyFatData 对象中的数据调用算法得到cn.net.aicare.algorithmutil.BodyFatData
如下:
AicareBleConfig.getBodyFatData(AlgorithmUtil.AlgorithmType.TYPE_AIC
ARE, bodyFatData.getSex(), bodyFatData.getAge(),
Double.valueOf(ParseData.getKgWeight(bodyFatData.getWeight(),
bodyFatData.getDecimalInfo())), bodyFatData .getHeight(),
bodyFatData.getAdc());

如需要获取去脂体重，体重控制量等额外的 6 项身体指标，请调用getMoreFatData计算得到 MoreFatData 对象
AicareBleConfig.getMoreFatData(int sex, int height, double weight,
double bfr, double rom, double pp)

```

## 八 给设备下发指令
在BleProfileServiceReadyActivity.onServiceBinded(WBYService.WBYBinder binder)获得WBYService.WBYBinder的实例，调用binder里面方法

```
    @Override
    protected void onServiceBinded(WBYService.WBYBinder binder) {
        this.binder = binder;

   }
   //如获取到历史记录
      binder.syncHistory();



   //WBYBinder的部分方法
   public class WBYBinder extends LocalBinder {

        /**
         * 获取历史记录
         */
        @Deprecated
        public void syncHistory() {
            mManager.sendCmd(AicareBleConfig.SYNC_HISTORY, AicareBleConfig.UNIT_KG);
        }

        /**
         * 同步当前用户
         *
         * @param user
         */
        public void syncUser(User user) {
            if (user == null) {
                return;
            }
            mManager.syncUser(user);
        }

        /**
         * 同步用户列表
         *
         * @param userList
         */
        @Deprecated
        public void syncUserList(List<User> userList) {
            mManager.syncUserList(userList);
        }

        /**
         * 同步当前单位
         *
         * @param unit {@link AicareBleConfig#UNIT_KG}
         *             {@link AicareBleConfig#UNIT_LB}
         *             {@link AicareBleConfig#UNIT_ST}
         *             {@link AicareBleConfig#UNIT_JIN}
         */
        public void syncUnit(byte unit) {
            mManager.sendCmd(AicareBleConfig.SYNC_UNIT, unit);
        }

        /**
         * 同步时间
         */
        @Deprecated
        public void syncDate() {
            mManager.syncDate();
        }

        /**
         * 查询蓝牙版本信息
         */
        @Deprecated
        public void queryBleVersion() {
            mManager.sendCmd(AicareBleConfig.GET_BLE_VERSION, AicareBleConfig.UNIT_KG);
        }

        /**
         * 更新用户信息
         *
         * @param user
         */
        @Deprecated
        public void updateUser(User user) {
            if (user == null) {
                return;
            }
            mManager.updateUser(user);
        }

        /**
         * 设置模式
         */
        public void setMode(@AicareBleConfig.MODE int cmd) {
            mManager.setMode(cmd);
        }

        /**
         * 校验是否授权
         */
        @Deprecated
        public void auth() {
            mManager.auth();
        }

        /**
         * 设置DID
         *
         * @param did
         */
        @Deprecated
        public void setDID(int did) {
            mManager.sendDIDCmd(AicareBleConfig.SET_DID, did);
        }

        /**
         * 查询DID
         */
        @Deprecated
        public void queryDID() {
            mManager.sendDIDCmd(AicareBleConfig.QUERY_DID, 0);
        }

        /**
         * 获取小数位数
         */
        public void getDecimalInfo() {
            mManager.getDecimalInfo();
        }

}

```


## 九 类说明

//aicare.net.cn.iweightlibrary.entity

#### 1.AlgorithmInfo(算法序列信息)
```
类型	参数名	说明
double	weight	体重
int	algorithmId	算法ID
int	adc	阻抗值
DecimalInfo	decimalInfo	小数点位数
```

#### 2.BM09Data(BM09数据)
```
类型	参数名	说明
int	agreementType	协议类型
int	unitType	单位类型
DecimalInfo	decimalInfp	小数点位数
double	weight	体重
int	adc	阻抗值
double	temp	温度
int	algorithmId	算法ID
int	did	（目前无用）
String	bleVersion	蓝牙版本
int	bleType	蓝牙类型（0x09）
String	address	设备地址
long	timeMillis	测量时间戳
boolean	isStable	是否稳定
```
#### 3.BM15Data(BM15数据)
```
类型	参数名	说明
String	version	蓝牙版本
int	agreementType	协议类型
int	unitType	单位类型
double	weight	体重
int	adc	阻抗值
double	temp	温度（若temp=6553.5，则表示秤不支持温度）
int	algorithmId	算法ID
int	did	（目前无用）
int	bleType	蓝牙类型（0x15）
String	address	设备地址
```

#### 4.BodyFatData(体脂数据)
```
类型	参数名	说明
String	date	测量日期
String	time	测量时间
double	weight	体重
double	bmi	身体质量指数
double	bfr	体脂率
double	sfr	皮下脂肪率
int	uvi	内脏脂肪指数
double	rom	肌肉率
double	bmr	基础代谢率
double	bm	骨量
double	vwc	水分率
double	bodyAge	身体年龄
double	pp	蛋白率
int	number	编号
int	sex	性别
int	age	年龄（1、男；2、女）
int	height	身高
int	adc	阻抗值
```

#### 5.BroadData(广播数据)
```
类型	参数名	说明
String	name	设备名
String	address	设备地址
boolean	isBright	是否亮屏
int	rssi	信号值
byte[]	specificData	广播数据
int	deviceType	设备类型
```
#### 6.DecimalInfo(小数点位数信息)
```
类型	参数名	说明
int	sourceDecimal	源数据小数点位数
int	kgDecimal	kg小数点位数
int	lbDecimal	lb小数点位数
int	stDecimal	st小数点位数
int	kgGraduation	kg分度
int	lbGraduation	lb分度
```
#### 7.User(用户信息)
```
类型	参数名	说明
int	id	编号
int	sex	性别
int	age	年龄（1、男；2、女）
int	height	身高
int	weight	体重
int	adc	阻抗值（弃用）
```
#### 8.WeightData(体重数据)
```
类型	参数名	说明
int	cmdType	命令类型（1、变化；2、稳定；3、阻抗测量中）
double	weight	体重
double	temp	温度（若温度为Double.MAX_VALUE则表示秤不支持温度）
DecimalInfo	decimalInfo	小数点位数信息
int	adc	阻抗值
int	algorithmType	算法ID
int	unitType	单位类型
int	deviceType	设备类型
```
#### 9.cn.net.aicare.algorithmutil.BodyFatData(计算得到的体脂数据)
```
类型	参数名	说明
double bmi;	身体质量指数
double bfr;	体脂率 body fat rate
double sfr;	皮下脂肪率 Subcutaneous fat rate
int uvi;	内脏脂肪指数
double rom; 肌肉率 Rate of muscle
int bmr; 基础代谢率 basal metabolic rate
double bm; 骨骼质量 Bone Mass
double vwc; 水含量
int bodyAge; 身体年龄 physical bodyAge
double pp; 蛋白率 protein percentage
```

#### 10.MoreFatData
```
类型	参数名	说明
double standardWeight;	标准体重
double controlWeight;	体重控制量
double fat;	脂肪量
double removeFatWeight;	去脂体重
double muscleMass; 肌肉量
double protein; 蛋白量
MoreFatData.FatLevel fatLevel; 肥胖等级
public static enum FatLevel {
        UNDER,  体重不足
        THIN,   偏瘦
        NORMAL,  标准
        OVER,  偏重
        FAT;  超重
}
```
#### 11.BleProfileService 连接状态
```
public static final int STATE_CONNECTING = 4; //连接中
public static final int STATE_DISCONNECTED = 0; //断开连接
public static final int STATE_CONNECTED = 1;//连接成功
public static final int STATE_SERVICES_DISCOVERED = 2;//发现服务
public static final int STATE_INDICATION_SUCCESS = 3;//使能成功
public static final int STATE_TIME_OUT = 5;//连接超时
```
#### 12.AicareBleConfig.SettingStatus 设备返回的状态信息
```
        int NORMAL = 0;//正常
        int LOW_POWER = 1;//低功耗
        int LOW_VOLTAGE = 2;//低电压
        int ERROR = 3;//超载
        int TIME_OUT = 4;//超时
        int UNSTABLE = 5;//称不稳定
        int SET_UNIT_SUCCESS = 6;//设置单位成功
        int SET_UNIT_FAILED = 7;//设置单位失败
        int SET_TIME_SUCCESS = 8;//设置时间成功
        int SET_TIME_FAILED = 9;//设置时间失败
        int SET_USER_SUCCESS = 10;//设置用户成功
        int SET_USER_FAILED = 11;//设置用户失败
        int UPDATE_USER_LIST_SUCCESS = 12;//更新用户列表成功
        int UPDATE_USER_LIST_FAILED = 13;//更新用户列表失败
        int UPDATE_USER_SUCCESS = 14;//更新用户成功
        int UPDATE_USER_FAILED = 15;//更新用户失败
        int NO_HISTORY = 16;//没有历史数据
        int HISTORY_START_SEND = 17;//历史数据开始发送
        int HISTORY_SEND_OVER = 18;//历史数据发送完成
        int NO_MATCH_USER = 19;//没有匹配的用户
        int ADC_MEASURED_ING = 20;//阻抗测量中
        int ADC_ERROR = 21;//阻抗测量失败
        int REQUEST_DISCONNECT = 22;//设备请求断开
        int SET_DID_SUCCESS = 23;//设置DID成功
        int SET_DID_FAILED = 24;//设置DID失败
        int DATA_SEND_END = 25;//测量数据发送完成
        int UNKNOWN = -1;//未知
```
#### 13.WBYService 设备返回的蓝牙信息
```
    public final static int BLE_VERSION = 0; //蓝牙版本
    public final static int MCU_DATE = 1;  //mcu日期
    public final static int MCU_TIME = 2;  //mcu 时间
    public final static int USER_ID = 3; //用户编号
    public final static int ADC = 4; //阻抗值
```