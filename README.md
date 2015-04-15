UpdateChecker
======================================================================
用于android自动检测软件更新的库，使用简单的xml配置更新信息
该库提供程序的更新检测、下载推送功能
使用方式：
-----------------------------------------------
#1.配置manifest.
### 添加如下信息
    <!-- 配置服务信息 -->
    <service android:name="me.olivine.updatechecker.UpdateService" />
    <!-- 配置广播 -->
    <receiver android:name="me.olivine.updatechecker.UpdateReceiver" />
#2.检查更新.
###
    调用函数UpdateChecker.getInstance().checkUpdate(Context context,UpdateChecker.OnResponseListener listener).
    其中实现listener中的onResponse和onError方法处理正常响应和错误处理，若缓存过更新信息，则在onCache中进行缓存处理
#3.启动下载更新.
###
    调用函数UpdateChecker.getInstance().startUpdate(Context context,String url).
    如需监听下载处理，可分别设置setOnUpdateFinishListener和setOnUpdateCancelListener，前者为更新完毕，后者为更新取消（包括正常取和非正常取消，可通过返回码判断)
  
xml文件配置方式
-----------------------------------------------------
###example:
    <?xml version="1.0" encoding="UTF-8"?>
    <Application
      versionCode="2"
      versionName="2.0"
      downloadUrl="http://115.182.0.156/static/updatecheckerdemo/updatecheckerdemo.apk">
      <UpdateInfo>
      1.更新XX
      2.更新YY
      3.更新ZZ</UpdateInfo>
    </Application>
###
    其中，versionCode为版本号，versionName为版本名，downloadUrl为更新文件下载地址，UpdateInfo标签中内容为更新信息
    请将配置文件放到服务器中
    
#设置服务器地址
### example:
    UpdateChecker.setServerAddress("http://XX.XX.XX.XX/static/updatecheckerdemo");
