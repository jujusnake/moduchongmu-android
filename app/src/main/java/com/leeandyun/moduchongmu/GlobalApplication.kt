package com.leeandyun.moduchongmu

import android.app.Application
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK

class GlobalApplication : Application() {
    var googleRequest: GetCredentialRequest? = null

    override fun onCreate() {
        super.onCreate()
        // Naver SDK 초기화
        NaverIdLoginSDK.initialize(this, BuildConfig.NAVER_CLIENT_ID, BuildConfig.NAVER_CLIENT_SECRET, "모두의 총무")

        // Kakao SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // Google 세팅
        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_CLIENT_ID)
            .build()
        googleRequest =
            GetCredentialRequest.Builder().addCredentialOption(signInWithGoogleOption).build()
    }
}

//        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
//            .setFilterByAuthorizedAccounts(true)
//            .setServerClientId("780202279961-1sssf0238oncivgj5raenj77o4r9ts1c.apps.googleusercontent.com")
//            .setAutoSelectEnabled(true)
//            .build()
