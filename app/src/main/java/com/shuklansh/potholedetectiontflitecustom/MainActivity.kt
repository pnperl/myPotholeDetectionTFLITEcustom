package com.shuklansh.potholedetectiontflitecustom

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shuklansh.potholedetectiontflitecustom.ml.ModelPot
import com.shuklansh.potholedetectiontflitecustom.ml.PotModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class MainActivity : AppCompatActivity() {

    var paint = Paint()
    lateinit var cameraDevice: CameraDevice
    lateinit var textureView: TextureView
    lateinit var cameraMAnager: CameraManager
    lateinit var handler: Handler
    lateinit var imageView: ImageView
    lateinit var bitmap: Bitmap
    lateinit var model: PotModel
    lateinit var texta : TextView
    lateinit var textb : TextView
    lateinit var textc : TextView
    lateinit var imageProcessor : ImageProcessor
    var imageSize = 512


    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(512,512,ResizeOp.ResizeMethod.BILINEAR)).build()
        getPermission()

        model = PotModel.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        paint.setColor(Color.RED)

        imageView = findViewById(R.id.BitmapDisplayImageView)
        textureView = findViewById(R.id.textureView)
        texta = findViewById(R.id.texta)
        textb = findViewById(R.id.textb)
        textb = findViewById(R.id.textc)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                var bitmap = textureView.bitmap!!

                var tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)
                tensorImage = imageProcessor.process(tensorImage)

// Creates inputs for reference.
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 512, 512, 3), DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

// Runs model inference and gets result.
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray
                val classes = listOf<String>("caution_pothole","caution_speedbump","caution_speedbump_plastic","nothing")
                var mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true)
                var canvas = Canvas(mutableBitmap)
                var h = bitmap.height
                var w = bitmap.width
                var x=0
                var indexofclass = 3
                var maxindx=0
                outputFeature0.forEachIndexed { index, fl ->

                    if(outputFeature0[maxindx]<fl) {
                        maxindx = index
                        if(maxindx<=120000){
                            indexofclass = 0
                        }else if(maxindx>120000){
                            indexofclass = 1
                        }
                    }
                    texta.text = indexofclass.toString()
                    textb.text = classes[indexofclass]

                    x = index
                    x *= 4
//                    while (x <= 49) {
//                        if (outputFeature0.get(x + 2) >= 0.3) {
//                            canvas.drawCircle(
//                                outputFeature0.get(x + 1) * w,
//                                outputFeature0.get(x) * h,
//                                10f,
//                                paint
//                            )
//                        }
//                        x += 3
//                    }
                }

                imageView.setImageBitmap(mutableBitmap)


// Releases model resources if no longer used.
                //model.close()



            }

        }
        cameraMAnager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraMAnager.openCamera(
            cameraMAnager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    var surfaceTexture = textureView.surfaceTexture
                    var surface = Surface(surfaceTexture)
                    var captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {

                            }
                        },
                        handler
                    )

                }

                override fun onDisconnected(camera: CameraDevice) {

                }

                override fun onError(camera: CameraDevice, error: Int) {

                }
            }, handler
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }
}