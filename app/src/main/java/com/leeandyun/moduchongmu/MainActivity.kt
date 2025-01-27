package com.leeandyun.moduchongmu

import android.animation.Animator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Objects


class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh : SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webview)
        val splashScreen: View = findViewById(R.id.splashScreen)
        val lottieAnimationView = findViewById<LottieAnimationView>(R.id.splashLottie)
        val progressBar: View = findViewById(R.id.splashProgress)

        var lottiePlayedThrough = false
        var isWebLoaded = false
        var isFirstLoadingFinished= false

        // Swipe to Refresh
        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        lottieAnimationView.addAnimatorListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                Log.e("Animation:", "start")
            }

            override fun onAnimationEnd(animation: Animator) {
                    lottiePlayedThrough = true
                    if (isWebLoaded) {
                        splashScreen.animate().translationY(-50f).alpha(0f).setDuration(500).withEndAction {
                            splashScreen.visibility = View.GONE
                            animation.removeAllListeners()
                            isFirstLoadingFinished = true
                        }.start()
                        progressBar.visibility = View.GONE
                    }
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.e("Animation:", "cancel")
            }

            override fun onAnimationRepeat(animation: Animator) {
                Log.e("Animation:", "repeat")
            }
        })

        // Enable JavaScript (optional)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(MyJavaScriptInterface(this, webView), "AndroidWV")
        webView.webChromeClient = MyWebChromeClient(this)

        // Set a WebViewClient to handle navigation inside the WebView
        webView.webViewClient = object: WebViewClient () {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (isFirstLoadingFinished == false) {
                    super.onPageStarted(view, url, favicon)
                    splashScreen.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isWebLoaded = true
                if (lottiePlayedThrough && isFirstLoadingFinished == false) {
                    progressBar.visibility = View.GONE
                    splashScreen.animate().translationY(-50f).alpha(0f).setDuration(500).withEndAction {
                        splashScreen.visibility = View.GONE
                        lottieAnimationView.removeAllAnimatorListeners()
                    }.start()
                }
                swipeRefresh.isRefreshing = false
            }
        }


        // Load a URL
        webView.loadUrl("http://172.30.1.107:4173/signin")
    }

    // Handle back navigation in WebView
    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webview)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val webView: WebView = findViewById(R.id.webview)
        (webView.webChromeClient as? MyWebChromeClient)?.handleActivityResult(requestCode, resultCode, data)
    }

    fun getSwipeRefreshLayout(): SwipeRefreshLayout {
        return swipeRefresh
    }
}


class MyJavaScriptInterface(private val context: Context, private val webView: WebView) {

    @JavascriptInterface
    fun sendMessage(message: String) {
        // This is where you handle the message sent from JavaScript
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @JavascriptInterface
    fun updateSwipeRefresher(enabled: Boolean) {
        if (context is MainActivity) {
            val swipeRefresh = context.getSwipeRefreshLayout()
            swipeRefresh.isEnabled = enabled
        }
    }

    @JavascriptInterface
    fun oAuthSignin(type: String) {
        when (type) {
            "naver" -> {
                Toast.makeText(context, "네이버 간편 로그인 중", Toast.LENGTH_LONG).show()
                NaverIdLoginSDK.authenticate(context, object : OAuthLoginCallback {
                    override fun onError(errorCode: Int, message: String) {
                        Log.e("Naver oAuth","Naver error! ${errorCode} : ${message}")
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        Log.e("Naver oAuth","Naver Failure! ${httpStatus} : ${message}")
                    }

                    override fun onSuccess() {
                        val accessToken = NaverIdLoginSDK.getAccessToken()
                        val refreshToken = NaverIdLoginSDK.getRefreshToken()
                        Log.e("Naver oAuth", "$accessToken, $refreshToken")
                        webView.evaluateJavascript("javascript:window.kotlin.handleKotlinToken('$type', '$accessToken', '$refreshToken')", null)
                    }
                })
            }
            "kakao" -> {
                // 카카오계정으로 로그인 공통 callback 구성
                // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
                val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                    if (error != null) {
                        Log.e("Kakao oAuth", "카카오계정으로 로그인 실패", error)
                    } else if (token != null) {
                        Log.i("Kakao oAuth", "카카오계정으로 로그인 성공 ${token.accessToken}")
                    }
                }

                // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
                if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        if (error != null) {
                            Log.e("Kakao oAuth", "카카오톡으로 로그인 실패", error)

                            // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                            // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                return@loginWithKakaoTalk
                            }

                            // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                        } else if (token != null) {
                            Log.i("Kakao oAuth", "카카오톡으로 로그인 성공 ${token.accessToken}")
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                }
            }
            "google" -> {
                // Handle Google OAuth
                Log.e("Google oAuth", "google signin start!")
                val credentialManager = CredentialManager.create(context)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val app = context.applicationContext as GlobalApplication
                        val request = app.googleRequest

                        val result = credentialManager.getCredential(context, request!!)
                        when (val data = result.credential) {
                            is CustomCredential -> {
                                if (data.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(data.data)
                                    val token = googleIdTokenCredential.idToken
                                    webView.evaluateJavascript("javascript:window.kotlin.handleKotlinToken('$type', '$token')", null)
                                }
                            }
                        }
                    } catch (error: Exception) {
                        if (error is NoCredentialException) {
                            Log.e("asdfsd", "asdfaklsdfjlkwarelwlre")
                        }
                        Log.e("Google oAuth", "${error.cause}")
                        Log.e("Google oAuth", "${error.message}")
                        Log.e("Google oAuth", error.localizedMessage)
                    }
                }
            }
            else -> {
                // Handle unknown type or fallback
                Toast.makeText(context,  "Unknown OAuth type: $type", Toast.LENGTH_SHORT)
            }
        }

    }

    @JavascriptInterface
    fun shareText(message: String) {
        val multilineMessage = message.replace("\\n", "\n") // Replace escaped \n from JavaScript with actual newlines
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, multilineMessage)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    @JavascriptInterface
    fun shareImage(base64Image: String, fileName: String) {
        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Save Bitmap to a file
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        // Get URI using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            Objects.requireNonNull(context),
            "${BuildConfig.APPLICATION_ID}.provider",
            file
        )

        // Share the image using Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to the receiving app
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }
}

class MyWebChromeClient(
    private val activity: Activity
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback = filePathCallback

        val intent = fileChooserParams?.createIntent()
        try {
            activity.startActivityForResult(intent, REQUEST_SELECT_FILE)
        } catch (e: ActivityNotFoundException) {
            this.filePathCallback = null
            Toast.makeText(activity, "Cannot open file chooser", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            filePathCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    companion object {
        const val REQUEST_SELECT_FILE = 100
    }
}
