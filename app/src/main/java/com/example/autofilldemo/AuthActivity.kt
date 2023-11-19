package com.example.autofilldemo

import android.R
import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * @author Sion
 * @date 2023/11/28 18:28
 * @version 1.0.0
 * @description 填充前的识别
 **/
class AuthActivity : AppCompatActivity(){

    private lateinit var keyguardLauncher: ActivityResultLauncher<Intent>
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var biometricManager: BiometricManager
    private var autoFillDatas: List<AutoFillData>? = null
    private val sp by lazy {
         getSharedPreferences(SimpleAutoFillService.PASSWORD_MANAGER , Context.MODE_PRIVATE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!initData()){
            authFail()
            return
        }
        keyguardManager = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            biometricManager = getSystemService(BiometricManager::class.java)
        }
        auth()
    }

    private fun initData(): Boolean{

        autoFillDatas = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent.getParcelableArrayListExtra(SimpleAutoFillService.AUTOFILLDATAS , AutoFillData::class.java)
        }else{
            intent.getParcelableArrayListExtra<AutoFillData>(SimpleAutoFillService.AUTOFILLDATAS)
        }

        if(autoFillDatas == null){
            return false
        }
        if(!this::keyguardLauncher.isInitialized){
            keyguardLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ){
                if(it.resultCode == RESULT_OK){
                    authSuccess()
                }else{
                    authFail()
                }
            }
        }
        return true
    }

    private fun auth(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && this::biometricManager.isInitialized){
            biometricAuth(
                CancellationSignal().apply {
                    setOnCancelListener {

                    }
                },
                object : AuthenticationCallback(){
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(
                            this@AuthActivity,
                            "auth Error",
                            Toast.LENGTH_SHORT
                        ).show()
                        authFail()
                    }

                    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                        super.onAuthenticationHelp(helpCode, helpString)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        authSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(
                            this@AuthActivity,
                            "auth Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        authFail()
                    }
                }
            )
        }else{
            keyguardAuth()
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun biometricAuth(cancellationSignal: CancellationSignal , authenticationCallback: AuthenticationCallback){
        // 该设备支持双识别
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(supportAuth()){
                BiometricPrompt.Builder(this)
                    .setTitle("Auth")
                    .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
                    .setConfirmationRequired(true)
                    .setDescription("Log in using your biometric credential")
                    .build().authenticate(
                        cancellationSignal,
                        mainExecutor,
                        authenticationCallback
                    )
            }
        }else{
            keyguardAuth()
        }
    }

    private fun keyguardAuth(){
        if(keyguardManager.isKeyguardSecure){
            keyguardLauncher.launch(
                keyguardManager.createConfirmDeviceCredentialIntent(
                    "Auth" , "Log in using your biometric credential"
                )
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun supportAuth(): Boolean{
        return biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun authFail(){
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun authSuccess(){
        val fillResponse = FillResponse.Builder()
            .addDataset(
                Dataset.Builder()
                    .also {
                        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU){
                            autoFillDatas?.forEach {
                                autoFillData ->
                                if(autoFillData.autoFillHint.equals(View.AUTOFILL_HINT_USERNAME)){
                                    sp.getString(View.AUTOFILL_HINT_USERNAME , "").let {
                                            username ->
                                        if(!username.isNullOrEmpty()){
                                            it.setValue(
                                                autoFillData.autoFillId,
                                                AutofillValue.forText(username),
                                                createPresentation(autoFillData.autoFillPresentationName)
                                            )
                                        }else{
                                            it.setValue(autoFillData.autoFillId , null)
                                        }
                                    }
                                }else if(autoFillData.autoFillHint.equals(View.AUTOFILL_HINT_PASSWORD)){
                                    sp.getString(View.AUTOFILL_HINT_PASSWORD , "").let {
                                            password ->
                                        if(!password.isNullOrEmpty()){
                                            it.setValue(
                                                autoFillData.autoFillId,
                                                AutofillValue.forText(password),
                                                createPresentation(autoFillData.autoFillPresentationName)
                                            )
                                        }else{
                                            it.setValue(autoFillData.autoFillId , null)
                                        }
                                    }
                                }
                            }
                        }else{
                            autoFillDatas?.forEach {autoFillData ->
                                if (autoFillData.autoFillHint.equals(View.AUTOFILL_HINT_USERNAME)) {
                                    sp.getString(View.AUTOFILL_HINT_USERNAME, "").let { username ->
                                        if(!username.isNullOrEmpty()){
                                            it.setField(
                                                autoFillData.autoFillId,
                                                Field.Builder()
                                                    .setValue(AutofillValue.forText(username))
                                                    .build()
                                            )
                                        }
                                    }
                                }else if(autoFillData.autoFillHint.equals(View.AUTOFILL_HINT_PASSWORD)){
                                    sp.getString(View.AUTOFILL_HINT_PASSWORD , "").let {
                                            password ->
                                        if(!password.isNullOrEmpty()){
                                            it.setField(
                                                autoFillData.autoFillId,
                                                Field.Builder()
                                                    .setValue(AutofillValue.forText(password))
                                                    .build()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .build()
            )
            .build()

        setResult(RESULT_OK , Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
        })
        finish()
    }

    private fun createPresentation(name: String): RemoteViews {
        val presentation = RemoteViews(this.getPackageName(), R.layout.simple_list_item_1)
        presentation.setTextViewText(android.R.id.text1, name)
        return presentation
    }
}