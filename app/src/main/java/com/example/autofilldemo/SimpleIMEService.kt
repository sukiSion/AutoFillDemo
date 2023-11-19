package com.example.autofilldemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodManager
import android.widget.inline.InlinePresentationSpec
import com.example.autofilldemo.databinding.LayoutSimpleInputMethodBinding

/**
 * @author Sion
 * @date 2023/11/29 15:42
 * @version 1.0.0
 * @description
 **/
class SimpleIMEService: InputMethodService() {

    private val sp by lazy {
        getSharedPreferences(SimpleAutoFillService.PASSWORD_MANAGER, Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View {
        val layoutSimpleInputMethodBinding = LayoutSimpleInputMethodBinding.inflate(layoutInflater)
        initView(layoutSimpleInputMethodBinding)
        return layoutSimpleInputMethodBinding.root
    }

    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            return super.onCreateInlineSuggestionsRequest(uiExtras)
        }else{
            return InlineSuggestionsRequest.Builder(
                listOf(
                    InlinePresentationSpec.Builder(
                        Size(0 , 0),
                        Size(1000 , 1000),
                    )
                        .build()
                )
            )
                .setMaxSuggestionCount(2)
                .build()
        }
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        Log.d("Sion" , "response: ${response}")
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return super.onInlineSuggestionsResponse(response)
        }else{
            response.inlineSuggestions.forEach {

            }
            return true
        }

    }



    private fun initView(layoutSimpleInputMethodBinding: LayoutSimpleInputMethodBinding){
        layoutSimpleInputMethodBinding.let {
            binding ->
            binding.ivClose.setOnClickListener {
                requestHideSelf(0)
            }
            binding.btFillUsername.setOnClickListener {
                this.currentInputConnection.let {
                    it.commitText("sion" , 1)
                }
            }
            binding.btFillPassword.setOnClickListener {
                this.currentInputConnection.let {
                    it.commitText("842143912" , 1)
                }

            }
        }
    }


}