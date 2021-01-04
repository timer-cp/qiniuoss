/*
 * @Author: your name
 * @Date: 2020-08-26 17:04:46
 * @LastEditTime: 2020-09-10 10:30:21
 * @LastEditors: your name
 * @Description: In User Settings Edit
 * @FilePath: /tejia_user/plugins/qiniuoss/example/lib/main.dart
 */
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'dart:async';

import 'package:qiniuoss/qiniuoss.dart';
import 'package:qiniuoss/file_path_entity.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String token;

  @override
  void initState() {
    super.initState();
    SchedulerBinding.instance.addPostFrameCallback((_) async {
      String _token = await getUploadToken();
      setState(() {
        token = _token;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('七牛存储示例'),
        ),
        body: Column(
          children: [
            FlatButton(
              child: Text('单文件上传'),
              color: Colors.blue,
              onPressed: () {
                Qiniuoss qiniu = Qiniuoss();
                qiniu.onProgressChanged().listen((percent) {
                  // 上传进度
                  debugPrint("$percent");
                });
                qiniu.uploadFile("xxx/xxx/hello.txt", "hello.txt", token);
              },
            ),
            FlatButton(
              child: Text('多文件上传'),
              color: Colors.blue,
              onPressed: () {
                Qiniuoss qiniu = Qiniuoss();
                List<FilePathEntity> entities = [];
                FilePathEntity entity1 = FilePathEntity(
                    filePath: "/xxxxxx/xxx1.jpg",
                    key: "xxx1.jpg",
                    token: token);
                FilePathEntity entity2 = FilePathEntity(
                    filePath: "/xxxxxx/xxx2.jpg",
                    key: "xxx2.jpg",
                    token: token);
                entities.add(entity1);
                entities.add(entity2);
                qiniu.uploadFiles(entities);
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<String> getUploadToken() async {
    //这里一般访问网络接口，从服务器返回上传所需的token
    return Future.delayed(Duration(seconds: 1), () {
      return "token123456";
    });
  }
}
