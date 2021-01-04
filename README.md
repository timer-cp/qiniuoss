# qiniuoss

A flutter plugin of Qiniu storage.

## Getting Started

上传所需要的Token由服务器获取，然后再上传文件。
可上传一个文件或多个文件。

单文件上传：
```
Future<String> uploadFile(String filePath, String key, String token)
Future<String> uploadData(Uint8List data, String key, String token)
```

多文件上传：
```
Future<List<String>> uploadFiles(List<FilePathEntity> entities)
```