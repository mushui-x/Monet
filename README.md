# Monet

极简图片选择器，支持原生图库选选择图片、原生相机拍照、原生剪裁，一行代码即可调用。

#### 功能

一行代码即可跳转到原生相册或者相机，选图拍照后直接回调图片Uri，不需要在onActivityResult再去手动接收结果处理数据，简单易容，内置了相机和存储权限的判断和申请，不需要另外再处理权限问题。

* 跳转原生相册选图返回结果。
* 跳转原生相机选图返回结果。
* 可配置是否需要裁剪。
* 内置权限处理。

#### 演示
<img src="images/demo.gif" alt="演示1" width="270" height="480"/>

#### 使用

##### 1、添加权限
```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
##### 2、引入依赖
```
repositories {
    ...
    mavenCentral()
}
```

```
implementation 'io.github.exciter-z:monet:1.0.0'
```
##### 3、Api调用
相册选图
```
PictureSelector.toGallery(
                this,
                PSConfig.Builder().needCrop(true).build(),
                object : PictureSelector.PictureSelectorListener {
                    override fun onResult(uri: Uri?) {
                        //todo 
                    }
                }
            )
```
拍照选图
```
PictureSelector.toCamera(
                this,
                PSConfig.Builder().needCrop(true).build(),
                object : PictureSelector.PictureSelectorListener {
                    override fun onResult(uri: Uri?) {
                        //todo 
                    }
                }
            )
```
