package com.yanyi.tejia.plugin.qiniu.qiniuoss;

import androidx.annotation.NonNull;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class QiniuossPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    private MethodChannel methodChannel;
    private EventChannel eventChannel;

    private EventChannel.EventSink eventSink;
    private boolean isCancelled = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "qiniuoss_method");
        methodChannel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "qiniuoss_event");
        eventChannel.setStreamHandler(this);
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel methodChannel = new MethodChannel(registrar.messenger(), "qiniuoss_method");
        methodChannel.setMethodCallHandler(new QiniuossPlugin());

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "qiniuoss_event");
        eventChannel.setStreamHandler(new QiniuossPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("uploadFile")) {
            uploadFile(call, result);
        } else if (call.method.equals("uploadData")) {
            uploadData(call, result);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }

    private void uploadFile(MethodCall call, final Result result) {
        String filePath = call.argument("filePath");
        String key = call.argument("key");
        String token = call.argument("token");
        String zoneRaw = call.argument("zone");

        Configuration config = new Configuration.Builder()
                // 分片上传时，每片的大小。 默认256K
                .chunkSize(512 * 1024)
                // 启用分片上传阀值。默认512K
                .putThreshhold(1024 * 1024)
                // 链接超时。默认10秒
                .connectTimeout(10)
                // 是否使用https上传域名
                .useHttps(true)
                // 禁用外部dns
                .dns(null)
                // 服务器响应超时。默认60秒
                .responseTimeout(60)
                // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
//                .zone(getZone(zoneRaw))
                .build();

        // 重用uploadManager。一般地，只需要创建一个uploadManager对象
        UploadManager uploadManager = new UploadManager(config);
        Map<String, String> params = new HashMap<>();
        UploadOptions options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (eventSink != null) {
                    eventSink.success(percent);
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return false;
            }
        });

        uploadManager.put(filePath, key, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo info, JSONObject res) {
                        // res包含hash、key等信息，具体字段取决于上传策略的设置
                        if (info.isOK()) {
                            result.success(key);
                        } else {
                            result.success("");
                        }
                    }
                }, options);
    }

    private void uploadData(MethodCall call, final Result result) {
        byte[] data = call.argument("data");
        String key = call.argument("key");
        String token = call.argument("token");
        String zoneRaw = call.argument("zone");

        Configuration config = new Configuration.Builder()
                // 分片上传时，每片的大小。 默认256K
                .chunkSize(512 * 1024)
                // 启用分片上传阀值。默认512K
                .putThreshhold(1024 * 1024)
                // 链接超时。默认10秒
                .connectTimeout(10)
                // 是否使用https上传域名
                .useHttps(true)
                // 禁用外部dns
                .dns(null)
                // 服务器响应超时。默认60秒
                .responseTimeout(60)
                // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
//                .zone(getZone(zoneRaw))
                .build();

        // 重用uploadManager。一般地，只需要创建一个uploadManager对象
        UploadManager uploadManager = new UploadManager(config);

        UploadOptions options = new UploadOptions(null, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (eventSink != null) {
                    eventSink.success(percent);
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return false;
            }
        });

        uploadManager.put(data, key, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo info, JSONObject res) {
                        // res包含hash、key等信息，具体字段取决于上传策略的设置
                        if (info.isOK()) {
                            result.success(key);
                        } else {
                            //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                            result.success("");
                        }
                    }
                }, options);
    }

    private Zone getZone(String raw) {
        Zone zone = FixedZone.zone0;

        switch (raw) {
            case "0":
                zone = FixedZone.zone0;
                break;
            case "1":
                zone = FixedZone.zone1;
                break;
            case "2":
                zone = FixedZone.zone2;
                break;
            case "3":
                zone = FixedZone.zoneNa0;
                break;
            case "4":
                zone = FixedZone.zoneAs0;
                break;
            default:
                break;
        }
        return zone;
    }
}
