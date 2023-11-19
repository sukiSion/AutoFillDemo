package com.example.autofilldemo

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.autofill.AutofillManager
import com.example.autofilldemo.databinding.ActivitySimpleAutoFillBinding

class SimpleAutoFillActivity : AppCompatActivity() {
    companion object{
        const val TAG = "Sion"
    }
    private lateinit var activitySimpleAutoFillBinding: ActivitySimpleAutoFillBinding
    private lateinit var autoFillManager: AutofillManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySimpleAutoFillBinding = ActivitySimpleAutoFillBinding.inflate(layoutInflater)
        autoFillManager = getSystemService(AutofillManager::class.java)
        setContentView(activitySimpleAutoFillBinding.root)
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
            btGoService.setOnClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).also {
                    it.data = Uri.parse("package:com.android.settings")
                })
            }
        }
    }
}