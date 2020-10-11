#import "QiniuossPlugin.h"
#import <Qiniu/QiniuSDK.h>

@interface QiniuossPlugin () <FlutterStreamHandler>

@end

@implementation QiniuossPlugin {
    FlutterEventSink _eventSink;
}
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    QiniuossPlugin* instance = [[QiniuossPlugin alloc] init];
    FlutterMethodChannel* methodChannel = [FlutterMethodChannel
                                           methodChannelWithName:@"qiniuoss_method"
                                           binaryMessenger:[registrar messenger]];
    [registrar addMethodCallDelegate:instance channel:methodChannel];
    FlutterEventChannel* eventChannel = [FlutterEventChannel eventChannelWithName:@"qiniuoss_event" binaryMessenger:[registrar messenger]];
    [eventChannel setStreamHandler:instance];
    
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"uploadFile" isEqualToString:call.method]) {
        //        result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
        [self uploadFileWithArguments:call.arguments result:result];
    } else if([@"uploadData" isEqualToString:call.method]){
        [self uploadDataWithArguments:call.arguments result:result];
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)uploadFileWithArguments:arguments result:(FlutterResult)result{
    NSString* filepath = arguments[@"filePath"];
    NSString* key = arguments[@"key"];
    NSString* token = arguments[@"token"];
    QNConfiguration *config = [QNConfiguration build:^(QNConfigurationBuilder *builder) {
        builder.useHttps = YES;
    }];
    QNUploadManager *upManager = [[QNUploadManager alloc] initWithConfiguration:config];
    QNUploadOption *option = [[QNUploadOption alloc] initWithMime:nil progressHandler:^(NSString *key, float percent) {
        self->_eventSink([NSString stringWithFormat:@"%f", percent]);
    } params:nil checkCrc:false cancellationSignal:nil];
    [upManager putFile:filepath key:key token:token complete:^(QNResponseInfo *info, NSString *key, NSDictionary *resp) {
        if (!resp) {
            result(@"");
        }else{
            result(key);
        }
    } option:option];
}

- (void)uploadDataWithArguments:arguments result:(FlutterResult)result{
    FlutterStandardTypedData* flutterData = (FlutterStandardTypedData*)arguments[@"filePath"];
    NSString* key = arguments[@"key"];
    NSString* token = arguments[@"token"];
    QNConfiguration *config = [QNConfiguration build:^(QNConfigurationBuilder *builder) {
        builder.useHttps = YES;
    }];
    QNUploadManager *upManager = [[QNUploadManager alloc] initWithConfiguration:config];
    QNUploadOption *option = [[QNUploadOption alloc] initWithMime:nil progressHandler:^(NSString *key, float percent) {
        self->_eventSink([NSString stringWithFormat:@"%f", percent]);
    } params:nil checkCrc:false cancellationSignal:nil];
    [upManager putData:flutterData.data key:key token:token complete:^(QNResponseInfo *info, NSString *key, NSDictionary *resp) {
        if (!resp) {
            result(@"");
        }else{
            result(key);
        }
    } option:option];
}

- (FlutterError *)onListenWithArguments:(id)arguments eventSink:(FlutterEventSink)events{
    _eventSink = events;
    return nil;
}

- (FlutterError *)onCancelWithArguments:(id)arguments{
    _eventSink = nil;
    return nil;
}

@end
