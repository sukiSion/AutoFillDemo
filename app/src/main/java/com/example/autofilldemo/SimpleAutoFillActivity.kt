package com.example.autofilldemo

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.hardware.biometrics.BiometricPrompt.AuthenticationResult
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.autofilldemo.databinding.ActivitySimpleAutoFillBinding
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class SimpleAutoFillActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "Sion"
        private const val KEY_NAME = "passwork_key"
    }
    private lateinit var activitySimpleAutoFillBinding: ActivitySimpleAutoFillBinding
    private lateinit var autoFillManager: AutofillManager
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var biometricManager: BiometricManager
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var android10AuthMode = 0
    private lateinit var authenticationCallback: AuthenticationCallback
    private val executor by lazy {
        ContextCompat.getMainExecutor(this)
    }
    private val cancellationSignal by lazy {
        CancellationSignal().also {
            it.setOnCancelListener {

            }
        }
    }

    private val sp by lazy {
        getSharedPreferences(SimpleAutoFillService.PASSWORD_MANAGER , Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySimpleAutoFillBinding = ActivitySimpleAutoFillBinding.inflate(layoutInflater)
        setContentView(activitySimpleAutoFillBinding.root)
        inputMethodManager = getSystemService(InputMethodManager::class.java)
        autoFillManager = getSystemService(AutofillManager::class.java)
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            keyguardManager = getSystemService(KeyguardManager::class.java)
        }else{
            // 只有存在生物是被才会生成一个密钥
//            generateSecretKey(KeyGenParameterSpec.Builder(
//                KEY_NAME,
//                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
//                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
//                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//                .setUserAuthenticationRequired(true)
//                // Invalidate the keys if the user has registered a new biometric
//                // credential, such as a new fingerprint. Can call this method only
//                // on Android 7.0 (API level 24) or higher. The variable
//                // "invalidatedByBiometricEnrollment" is true by default.
//                .setInvalidatedByBiometricEnrollment(true)
//                .build())
            // 当存在生物识别和设备凭证识别时
            generateSecretKey(KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0 /* duration */,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or
                            KeyProperties.AUTH_DEVICE_CREDENTIAL)
                .build())
            biometricManager = getSystemService(BiometricManager::class.java)
            authenticationCallback = object : AuthenticationCallback(){
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                    super.onAuthenticationHelp(helpCode, helpString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
//                    val encryptedInfo: ByteArray? = result?.cryptoObject?.cipher?.doFinal("need_encrypt_string".toByteArray(
//                        Charset.defaultCharset()))
//                    Log.d(TAG, "Encrypted information: " +
//                            Arrays.toString(encryptedInfo) )
                    result?.let {
                        encryptSecretInformation(it)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            if(it.resultCode == RESULT_OK){
                Toast.makeText(
                    this,
                    "authentication success",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        activitySimpleAutoFillBinding.apply {
            etAccount.setAutofillHints(View.AUTOFILL_HINT_USERNAME)
            tvAutoFillManagerIsEnabled.text = "${autoFillManager.isEnabled}"
            tvAutoFillManagerIsAutoFillSupported.text = "${autoFillManager.isAutofillSupported}"
            tvAutoFillManagerHasEnabledAutoFillServices.text = "${autoFillManager.hasEnabledAutofillServices()}"
            btCommit.setOnClickListener {
                if(autoFillManager.isEnabled && autoFillManager.isAutofillSupported){
                    autoFillManager.commit()
                }
            }
            btGoAutoFillService.setOnClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).also {
                    it.data = Uri.parse("package:com.android.settings")
                })
            }
            btGoLock.setOnClickListener {
                // 判断是否设置了屏锁
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                    if(this@SimpleAutoFillActivity::keyguardManager.isInitialized){
                        if(keyguardManager.isKeyguardSecure){
                            launcher.launch(
                                keyguardManager.createConfirmDeviceCredentialIntent(
                                    null , null
                                )
                            )
                        }
                    }
                }else{
                    when(biometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_STRONG)){
                        // 未检测到错误。
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            Log.d(TAG, "App can authenticate using biometrics.")
                            getBiometricPromptBuilder().also {
                                when(android10AuthMode){
                                    0 -> {
                                        it.setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
                                            .setNegativeButton(
                                                "Cancel",
                                                executor
                                            ) { dialog, which -> Log.d(TAG , "cancel authentication") }
                                    }
                                    1 -> {
                                        it.setAllowedAuthenticators(Authenticators.DEVICE_CREDENTIAL)
                                    }
                                    2 -> {
                                        it.setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG  or Authenticators.DEVICE_CREDENTIAL)
                                    }
                                }
                            }.build()
                                .authenticate(
                                    BiometricPrompt.CryptoObject(getCipher().also { it.init(Cipher.ENCRYPT_MODE , getSecretKey()) }),
                                    cancellationSignal,
                                    executor,
                                    authenticationCallback
                                )
//                                .authenticate(
//                                cancellationSignal,
//                                executor,
//                                authenticationCallback
//                            )
                        }
                        // 硬件不可用。请稍后再试。
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                            Log.e(TAG, "No biometric features available on this device.")
                        }
                        // 用户未注册任何生物识别技术。
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            Log.e(TAG, "Biometric features are currently unavailable.")
                        }
                        // 没有生物识别硬件。
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                    Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
                            }
                            launcher.launch(enrollIntent)
                        }
                        // 已发现一个安全漏洞，在安全更新解决此问题之前，传感器不可用。
                        // 例如，如果使用 BiometricManager.Authenticators.BIOMETRIC_STRONG 请求身份验证，
                        // 但传感器的强度当前只能满足BiometricManager.Authenticators.BIOMETRIC_WEAK，则可以收到此错误。
                        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {

                        }
                    }
                }
            }
            rgAuthMode.setOnCheckedChangeListener  { group, checkedId ->
                when(checkedId){
                    R.id.rb_biometric_strong -> {
                        android10AuthMode = 0
                    }
                    R.id.rb_device_credential -> {
                        android10AuthMode = 1
                    }
                    R.id.rb_biometric_strong_device_credential -> {
                        android10AuthMode = 2
                    }
                }
            }
            etPassword.setOnTouchListener(
                object : View.OnTouchListener{
                    private var clickVisiableIcon = false
                    private var passwordVisiable = false
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        when(event?.action){
                            MotionEvent.ACTION_DOWN -> {
                                val downX = event.x
                                val downY = event.y
                                val visiableIconDrawable = etPassword.compoundDrawables[2]
                                if((downX <= etPassword.width && downX >= etPassword.width - visiableIconDrawable.intrinsicWidth - etPassword.paddingEnd)
                                    && (downY <= etPassword.height && downY >= etPassword.height - visiableIconDrawable.intrinsicHeight - etPassword.paddingBottom)){
                                    clickVisiableIcon = true
                                    return true
                                }else{
                                    clickVisiableIcon = false
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                val upX = event.x
                                val upY = event.y
                                val visiableIconDrawable = etPassword.compoundDrawables[2]
                                if(clickVisiableIcon){
                                    if((upX <= etPassword.width && upX >= etPassword.width - visiableIconDrawable.intrinsicWidth - etPassword.paddingEnd)
                                        && (upY <= etPassword.height && upY >= etPassword.height - visiableIconDrawable.intrinsicHeight - etPassword.paddingBottom)){
                                        if(passwordVisiable){
                                            etPassword.setCompoundDrawablesWithIntrinsicBounds(null , null , getDrawable(R.drawable.icon_invisiable) ,null )
                                            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                                        }else{
                                            etPassword.setCompoundDrawablesWithIntrinsicBounds(null , null ,getDrawable(R.drawable.icon_visiable), null  )
                                            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                                        }
                                        passwordVisiable = !passwordVisiable
                                        return true
                                    }
                                }
                            }
                        }
                        return false
                    }
                }
            )
            cbFillBeforeAuth.isChecked =  sp.getBoolean(SimpleAutoFillService.IDAUTH ,false)
            cbFillBeforeAuth.setOnCheckedChangeListener { buttonView, isChecked ->
                sp.edit (commit = false){
                    putBoolean(SimpleAutoFillService.IDAUTH , isChecked)
                }
            }
            btGoInputMethodServic.setOnClickListener {
                for (inputMethodInfo in inputMethodManager.enabledInputMethodList){
                    if(inputMethodInfo.serviceName.equals(SimpleIMEService::class.java.name)){
                        Log.d(TAG , "当前支持的键盘: ${inputMethodInfo}")
                    }
                }
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun getBiometricPromptBuilder(): BiometricPrompt.Builder{
        return BiometricPrompt.Builder(this)
            .setTitle("Auth")
            .setSubtitle("Log in using your biometric credential")
            // 目前该参仅在虹膜和人脸识别下有用，设置为false不需要用户在识别成功后进行再次确认
            // 能够让用户快速浏览对应的信息
            .setConfirmationRequired(false)
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)

        return keyStore.getKey(KEY_NAME, null) as SecretKey

    }


    private fun getCipher(): Cipher {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun encryptSecretInformation(result: AuthenticationResult) {
        // Exceptions are unhandled for getCipher() and getSecretKey().
//        val cipher = getCipher()
//        val secretKey = getSecretKey()
        try {
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedInfo: ByteArray = result.cryptoObject.cipher.doFinal(
                // plaintext-string text is whatever data the developer would
                // like to encrypt. It happens to be plain-text in this example,
                // but it can be anything
                "need_encrypt_string".toByteArray(Charset.defaultCharset())
            )
            Log.d(
                TAG, "Encrypted information: " +
                        Arrays.toString(encryptedInfo)
            )
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Key is invalid.")
        } catch (e: UserNotAuthenticatedException) {
            Log.d(TAG, "The key's validity timed out.")
            // 在此处进行重新检测
        }
    }
}