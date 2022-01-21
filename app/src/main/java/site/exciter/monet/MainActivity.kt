package site.exciter.monet

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button1.setOnClickListener {
            PictureSelector.toGallery(
                this,
                PSConfig.Builder().needCrop(true).build(),
                object : PictureSelector.PictureSelectorListener {
                    override fun onResult(uri: Uri?) {
                        Glide.with(this@MainActivity).load(uri?.path).into(imageView)
                    }
                }
            )
        }

        button2.setOnClickListener {
            PictureSelector.toCamera(
                this,
                PSConfig.Builder().needCrop(true).build(),
                object : PictureSelector.PictureSelectorListener {
                    override fun onResult(uri: Uri?) {
                        Glide.with(this@MainActivity).load(uri?.path).into(imageView)
                    }
                }
            )
        }
    }
}