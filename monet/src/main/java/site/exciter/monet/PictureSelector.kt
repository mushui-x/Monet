package site.exciter.monet

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 *
 * @Description: PictureSelector
 * @Author: ZhangJie
 * @CreateDate: 2021/12/28 4:46 下午
 */
class PictureSelector {

    companion object {
        //请求权限
        const val REQUEST_PERMISSION = 1000

        //拍照
        const val REQUEST_CAMERA_SELECT = 10001

        //相册
        const val REQUEST_GALLERY_SELECT = 1002

        //裁剪
        const val REQUEST_CROP = 1003

        var mListener: PictureSelectorListener? = null

        fun toCamera(context: Activity, config: PSConfig, listener: PictureSelectorListener) {
            mListener = listener
            val intent = Intent(context, PictureSelectorActivity::class.java).apply {
                putExtra("config", config)
                putExtra("type", 0)
            }
            context.startActivity(intent)
        }

        fun toGallery(context: Activity, config: PSConfig, listener: PictureSelectorListener) {
            mListener = listener
            val intent = Intent(context, PictureSelectorActivity::class.java).apply {
                putExtra("config", config)
                putExtra("type", 1)
            }
            context.startActivity(intent)
        }
    }

    interface PictureSelectorListener {
        fun onResult(uri: Uri?)
    }

    class PictureSelectorActivity : AppCompatActivity() {

        private lateinit var config: PSConfig
        private var type: Int = 0

        private var mPublicUri: Uri? = null
        private var mPrivateUri: Uri? = null
        private var mCropOutUri: Uri? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            config = intent.getSerializableExtra("config") as PSConfig
            type = intent.getIntExtra("type", 0)

            choose()
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_PERMISSION) {
                choose()
            }
        }

        private fun choose() {
            when (type) {
                0 -> camera()
                1 -> gallery()
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode != RESULT_OK) {
                mListener?.onResult(null)
                finish()
                return
            }

            when (requestCode) {
                REQUEST_CAMERA_SELECT -> {
                    if (config.needCrop) {
                        startCrop(mPublicUri)
                    } else {
                        moveUri(mPublicUri, mPrivateUri)
                        mListener?.onResult(mPrivateUri)
                        finish()
                    }
                }
                REQUEST_GALLERY_SELECT -> {
                    if (data != null) {
                        val uri = data.data
                        if (config.needCrop) {
                            startCrop(uri)
                        } else {
                            mListener?.onResult(uri)
                            finish()
                        }
                    }
                }
                REQUEST_CROP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        moveUri(mPublicUri, mPrivateUri)
                        mCropOutUri = mPrivateUri
                    }
                    mListener?.onResult(mCropOutUri)
                    finish()
                }
            }
        }

        private fun camera() {
            if (!PermissionHelper.hasCameraPermission(this) || !PermissionHelper.hasStoragePermission(
                    this
                )
            ) {
                PermissionHelper.requestCameraAndStoragePermission(this, REQUEST_PERMISSION)
                return
            }

            initPath()
            openCamera()
        }

        private fun openCamera() {
            val intent = Intent().apply {
                action = MediaStore.ACTION_IMAGE_CAPTURE
                putExtra(MediaStore.EXTRA_OUTPUT, mPublicUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_CAMERA_SELECT)
        }

        private fun gallery() {
            if (!PermissionHelper.hasStoragePermission(this)) {
                PermissionHelper.requestStoragePermission(this, REQUEST_PERMISSION)
                return
            }

            initPath()
            openGallery()
        }

        private fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALLERY_SELECT)
        }

        private fun startCrop(inUri: Uri?) {
            var uri = inUri
            val intent = Intent("com.android.camera.action.CROP")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            //兼容部分手机无法裁剪相册选择的图片
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val scheme = uri?.scheme
                val path = uri?.path
                //这个!.jpg是兼容小米有时候content://开头，.jpg结尾
                if (path == null || !path.endsWith(".jpg")) {
                    if (scheme != null && scheme == ContentResolver.SCHEME_CONTENT) {
                        val cursor = uri?.let { contentResolver.query(it, null, null, null, null) }
                            ?: return
                        if (cursor.moveToFirst()) {
                            val id =
                                cursor.getShort(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                            uri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )
                        }
                        cursor.close()
                    }
                }
            }

            //兼容Android11
            mCropOutUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //android 11以上，系统无法裁剪私有目录下的图片，所以将文件创建在公有目录
                mPublicUri
            } else {
                mPrivateUri
            }

            intent.apply {
                putExtra(MediaStore.EXTRA_OUTPUT, mCropOutUri)
                setDataAndType(uri, "image/*")
                putExtra("crop", "true")
                putExtra("aspectX", 9998) //修复华为手机默认为圆角裁剪的问题
                putExtra("aspectY", (9999 * 1.0f).toInt())
                putExtra("scale", true)
                putExtra("scaleUpIfNeeded", true)
                putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
                putExtra("return-data", false)
            }

            startActivityForResult(intent, REQUEST_CROP)

        }

        private fun initPath() {
            val publicFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path,
                "${System.currentTimeMillis()}.jpg"
            )
            val privateFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")

            mPublicUri = getContentUriByFile(publicFile)
            mPrivateUri = Uri.fromFile(privateFile)
        }

        private fun getContentUriByFile(file: File): Uri? {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            return contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }

        private fun moveUri(inUri: Uri?, outUri: Uri?) {
            val inStream = inUri?.let { contentResolver.openInputStream(it) } ?: return
            val file = File(outUri?.path)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()

            var fos: FileOutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fos = FileOutputStream(file)
                FileUtils.copy(inStream, fos)
            } else {
                val arrayOutputStream = ByteArrayOutputStream()
                val buffer = ByteArray(1024 * 10)
                while (true) {
                    val len = inStream.read(buffer)
                    if (len == -1) {
                        break
                    }
                    arrayOutputStream.write(buffer, 0, len)
                }
                arrayOutputStream.close()

                val dataByte = arrayOutputStream.toByteArray()
                if (dataByte.isNotEmpty()) {
                    fos = FileOutputStream(file)
                    fos.write(dataByte)
                }
            }

            fos?.close()
            inStream.close()

            //删除公共目录的图片
            mPublicUri?.let { contentResolver.delete(it, null, null) }
        }
    }

}