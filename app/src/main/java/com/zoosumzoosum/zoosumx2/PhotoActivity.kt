package com.zoosumzoosum.zoosumx2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zoosumzoosum.zoosumx2.dialog.FriendConfirmSubDialog
import com.zoosumzoosum.zoosumx2.dialog.ResidentConfirmSubDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.kakao.sdk.link.LinkClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoActivity : AppCompatActivity() {

    var timestamp: String? = null

    var fbAuth: FirebaseAuth? = null
    var fbFirestore: FirebaseFirestore? = null
    val storage = Firebase.storage

    private val requestImageCapture = 1 //카메라 사진 촬영 요청코드
    private lateinit var curPhotoPath: String //문자열 형태의 사진 경로 값
    private lateinit var scaledFile: File //카카오 서버에 전송하기 위해 용량 조절된 사진
    private lateinit var PhotoURLKakao: String //카카오 서버에 업로드 된 사진의 URL

    private var sendPermission = false //사진이 등록되지 않을 경우 0



    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        setPermission() //최초 카메라 권한 허용

        fbAuth = FirebaseAuth.getInstance()
        fbFirestore = FirebaseFirestore.getInstance()


        // status bar 색상 변경
        val window = this.window
        window.statusBarColor = ContextCompat.getColor(this, R.color.friendly_green)


        //뒤로 가기
        val backButton = findViewById<ImageButton>(R.id.photo_back)
        backButton.setOnClickListener {
            finish()
        }

        //카메라 연동
        square_photo.setOnClickListener{
            takeCapture() //기본 카메라 앱 실행 후 사진 촬영
        }

        //주민에게 인증
        confirm_to_resident.setOnClickListener{
            if (sendPermission) {
                val file = Uri.fromFile(File(curPhotoPath))
                val pathString = "images/${file.lastPathSegment}"
                //다이얼로그 호출
                val dlg = ResidentConfirmSubDialog(this, curPhotoPath, timestamp, pathString)
                dlg.start(this)
            }
            else{
                Toast.makeText(this, "먼저 사진을 등록해주세요.", Toast.LENGTH_LONG).show()
            }
        }

        //친구에게 인증
        //DB에 업로드 하지 않음
        confirm_to_friend.setOnClickListener {
            if(sendPermission){
                Toast.makeText(this, "메시지를 작성하는 중이에요! 약간의 시간이 소요될 수 있어요.", Toast.LENGTH_SHORT).show()
                LinkClient.instance.uploadImage(scaledFile){imageUploadResult, error ->
                    if(error!=null){
                        //Log.e("Kakao server upload","failed", error)
                    }
                    else if(imageUploadResult!=null){
//                        Log.i("Kakao server upload","success \n${imageUploadResult.infos.original}")
                        PhotoURLKakao = imageUploadResult.infos.original.url

                        //고정 카카오 피드 메시지 작성
                        val defaultFeed = FeedTemplate(
                            content = Content(
                                title = "재활용 인증 요청이 도착했어요!",
                                description = "섬이 깨끗해질 수 있도록, 친구의 분리배출 결과를 확인해주세요!",
                                imageUrl = PhotoURLKakao,
                                link = Link(
                                    webUrl = "https://www.zoosum.site/",
                                    mobileWebUrl = "https://www.zoosum.site/"
                                )
                            ),
                            buttons = listOf(
                                Button(
                                    "주섬주섬 홈",
                                    Link(
                                        webUrl = "https://www.zoosum.site/",
                                        mobileWebUrl = "https://www.zoosum.site/"
                                    )
                                ),
                                Button(
                                    "앱 설치하기",
                                    Link( //임시로 홈페이지 주소 연결
                                        webUrl = "https://play.google.com/store/apps/details?id=com.zoosumzoosum.zoosumx2",
                                        mobileWebUrl = "https://play.google.com/store/apps/details?id=com.zoosumzoosum.zoosumx2"
                                    )
                                )
                            )
                        )

                        //메시지 보내기
                        LinkClient.instance.defaultTemplate(applicationContext, defaultFeed){linkResult, error2 ->
                            if(error2!=null){
//                                Log.e("kakao link sending","failed", error2)
                                Toast.makeText(this, "카카오톡이 설치되어 있지 않거나, 카카오톡을 실행하는 데 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                            }
                            else if(linkResult!=null){
//                                Log.d("kakao link sending","successed ${linkResult.intent}")
                                startActivity(linkResult.intent)

                                // 메시지 보내기에 성공할 경우 리워드 화면 연결
                                //다이얼로그 호출
                                val dlg = FriendConfirmSubDialog(this)
                                dlg.start(this)

                                //카카오 링크로 보내기에 성공했지만 아래 경고 메시지가 있으면 일부 컨텐츠 오작동 가능성 있음
                                //Log.w("kakao link sending", "Warning Msg: ${linkResult.warningMsg}")
                                //Log.w("kakao link sending", "Argument Msg: ${linkResult.argumentMsg}")
                            }
                        }

                    }
                }
            }
            else{
                Toast.makeText(this, "먼저 사진을 등록해주세요.", Toast.LENGTH_LONG).show()
            }
        }

    }

    //사진 촬영
    private fun takeCapture() {
        //기본 카메라 앱 실행
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try{
                    createImageFile()
                } catch (ex: IOException){
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.zoosumzoosum.zoosumx2.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestImageCapture)
                }
            }
        }
    }

    //이미지 파일 생성
    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File? {
        timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile("JPEG_${timestamp}_",".jpg",storageDir).apply{curPhotoPath = absolutePath}
    }

    //테드 퍼미션 설정
    private fun setPermission() {
        val permission = object : PermissionListener{
            override fun onPermissionGranted() { //설정해놓은 위험권한들이 허용된 경우
                Toast.makeText(this@PhotoActivity, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@PhotoActivity, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }

        }

        TedPermission.with(this)
            .setPermissionListener(permission)
            .setRationaleMessage("카메라 앱을 사용하시려면 권한을 허용해주세요.")
            .setDeniedMessage("권한을 거부하셨습니다. [앱 설정] -> [권한] 항목에서 허용해주세요.")
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .check()
    }


    //startActivityforResult를 통해서 기본 카메라 앱으로부터 받아온 사진 결과값
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(this, "사진을 압축 중이에요. 약간의 시간이 소요될 수 있어요!", Toast.LENGTH_SHORT).show()

        //이미지를 성공적으로 가져온 경우
        if (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            val bitmap: Bitmap
            val file = File(curPhotoPath)

            camera_icon?.visibility = View.INVISIBLE
            btn_open_camera.visibility = View.INVISIBLE
            sendPermission = true

            if (Build.VERSION.SDK_INT < 28) { //안드로이드 9.0 보다 낮을 경우
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                //카카오링크 전송을 위해 사이즈 조정하여 카카오 서버에 업로드
                scaledFile = File(applicationContext.cacheDir,"sample.png")
                val scaledStream = FileOutputStream(scaledFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, scaledStream)
                scaledStream.close()

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 846, false)
                square_photo.setImageBitmap(scaledBitmap)
            } else {
                val decode = ImageDecoder.createSource(
                    this.contentResolver,
                    Uri.fromFile(file)
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                //카카오링크 전송을 위해 사이즈 조정하여 카카오 서버에 업로드
                scaledFile = File(applicationContext.cacheDir,"sample.png")
                val scaledStream = FileOutputStream(scaledFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, scaledStream)
                scaledStream.close()

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 600, 700, false)
                square_photo.setImageBitmap(scaledBitmap)
            }
            confirm_to_friend.isSelected = true
            confirm_to_friend.setTextColor(ContextCompat.getColor(this, R.color.friendly_green))
            confirm_to_resident.isSelected = true
            confirm_to_resident.setTextColor(ContextCompat.getColor(this, R.color.friendly_green))
        }
    }
}