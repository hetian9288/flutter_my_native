#import "FlutterMyNativePlugin.h"
#import <QiniuSDK.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <Photos/Photos.h>
#import <UIKit/UIKit.h>

@interface FlutterMyNativePlugin() <FlutterStreamHandler>

@property BOOL isCanceled;
@property FlutterEventSink eventSink;

@end

@implementation FlutterMyNativePlugin

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    NSString *eventChannelName  = @"cn.cnganen.flutter_my_native.qiniu_upload_event";
    FlutterEventChannel *eventChannel = [FlutterEventChannel eventChannelWithName:eventChannelName binaryMessenger:registrar.messenger];
    
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"cn.cnganen.flutter_my_native"
            binaryMessenger:[registrar messenger]];
  FlutterMyNativePlugin* instance = [[FlutterMyNativePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
    [eventChannel setStreamHandler:instance];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"goToHome" isEqualToString:call.method]) {
    [self goToHome:call result:result];
  } else if ([@"hasOpen" isEqualToString:call.method]) {
      [self hasOpen:call result:result];
  } else if ([@"openNativeLink" isEqualToString:call.method]) {
      [self openNativeLink:call result:result];
  } else if ([@"saveFile" isEqualToString:call.method]) {
      
      FlutterStandardTypedData* fileData = [call.arguments objectForKey:@"fileData"] ;
      
      //NSLog(@"fileData.data.length  :%ul",fileData.data.length);
      UIImage *image=[UIImage imageWithData:fileData.data];
      
      UNAuthorizationOptions status = [PHPhotoLibrary authorizationStatus];
      if (status == PHAuthorizationStatusRestricted) {
          NSLog(@"not allow to access photo library");
          result([FlutterError errorWithCode:@"权限错误" message:@"请同意访问相册权限" details:nil]);
      } else if (status == PHAuthorizationStatusDenied) { // if user chosen"Not Allow"
          result([FlutterError errorWithCode:@"权限错误" message:@"请同意访问相册权限" details:nil]);
      } else if (status == PHAuthorizationStatusAuthorized) { // if user chosen"Allow"
          [self saveImage:image result:result];
      } else if (status == PHAuthorizationStatusNotDetermined) { // if user not chosen before
          // Requests authorization with dialog
          [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
              if (status == PHAuthorizationStatusAuthorized) { //  if user  chosen "Allow"
                  //Save Image to Directory
                  [self saveImage:image result:result];
              }
          }];
      }
  } else if ([@"upload" isEqualToString:call.method]) {
      [self upload:call result:result];
  } else if ([@"cancelUpload" isEqualToString:call.method]) {
      [self cancelUpload:call result:result];
  } else {
    result(FlutterMethodNotImplemented);
  }
}

-(void)saveImage:(UIImage *)image result:(FlutterResult)result  {
    __block NSString* fileName;
    __block NSString* localId;
    [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
        PHAssetChangeRequest *assetChangeRequest = [PHAssetChangeRequest creationRequestForAssetFromImage:image];
        
        // [assetCollectionChangeRequest addAssets:@[[assetChangeRequest placeholderForCreatedAsset]]];
        
        localId = [[assetChangeRequest placeholderForCreatedAsset] localIdentifier];
    } completionHandler:^(BOOL success, NSError *error) {
        
        if (success) {
            NSLog(@"save image successful ");
            PHFetchResult* assetResult = [PHAsset fetchAssetsWithLocalIdentifiers:@[localId] options:nil];
            PHAsset *asset = [assetResult firstObject];
            [[PHImageManager defaultManager] requestImageDataForAsset:asset options:nil resultHandler:^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info) {
                NSLog(@"Success %@ %@",dataUTI,info);
                
                NSLog(@"Success PHImageFileURLKey %@  ", (NSString *)[info objectForKey:@"PHImageFileURLKey"]);
                fileName=((NSURL *)[info objectForKey:@"PHImageFileURLKey"]).absoluteString;
                result(fileName);
            }];
            
        } else {
            NSLog(@"save image failed!%@",error);
            
            fileName= @"";
            result(fileName);
        }
    }];
}

// IOS 端不需要该功能
- (void) goToHome:(FlutterMethodCall*)call result:(FlutterResult)result
{
    result(@"success");
}

- (void) hasOpen:(FlutterMethodCall*)call result:(FlutterResult)result
{
    NSString *url = call.arguments[@"uri"];
    NSURL *uri = [NSURL URLWithString:url];
    if ([[UIApplication sharedApplication]
         canOpenURL:uri]) {
        result(@"success");
    }else {
        result([FlutterError errorWithCode:@"110" message:@"not install" details:@""]);
    }
}

- (void) openNativeLink:(FlutterMethodCall*)call result:(FlutterResult)result
{
    NSString *url = call.arguments[@"uri"];
    NSURL *uri = [NSURL URLWithString:url];
    if ([[UIApplication sharedApplication]
         canOpenURL:uri]) {
        [[UIApplication sharedApplication] openURL:uri];
        result(@"success");
    }else {
        result([FlutterError errorWithCode:@"110" message:@"not install" details:@""]);
    }
}

- (void)upload:(FlutterMethodCall*)call result:(FlutterResult)result{
    self.isCanceled = FALSE;
    
    NSString *filepath = call.arguments[@"filepath"];
    NSString *key = call.arguments[@"key"];
    NSString *token = call.arguments[@"token"];
    
    QNUploadOption *opt = [[QNUploadOption alloc] initWithMime:nil progressHandler:^(NSString *key, float percent) {
        NSLog(@"progress %f",percent);
        self.eventSink(@{
                         @"key": key,
                         @"percent": @(percent),
                         });
    } params:nil checkCrc:NO cancellationSignal:^BOOL{
        return self.isCanceled;
    }];
    
    QNUploadManager *manager = [[QNUploadManager alloc] init];
    [manager putFile:filepath key:key token:token complete:^(QNResponseInfo *info, NSString *key, NSDictionary *resp) {
        NSLog(@"info %@", info);
        NSLog(@"resp %@",resp);
        result(@(info.isOK));
    } option:(QNUploadOption *) opt];
}

- (void)cancelUpload:(FlutterMethodCall*)call result:(FlutterResult)result{
    self.isCanceled = TRUE;
    result(@"success");
}

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    self.isCanceled = TRUE;
    self.eventSink = nil;
    return nil;
}

- (FlutterError * _Nullable)onListenWithArguments:(id _Nullable)arguments eventSink:(nonnull FlutterEventSink)events {
    self.isCanceled = FALSE;
    self.eventSink = events;
    return nil;
}

@end
