package com.example.autofilldemo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.app.slice.Slice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.util.Log
import android.util.Size
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.inputmethod.InlineSuggestion
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.edit

class SimpleAutoFillService: AutofillService() {

    companion object{
        private const val GETFILLS = 0
        private const val GETSAVES = 1

        const val PASSWORD_MANAGER = "PasswordManager"
        const val IDAUTH = "IdAuth"

        const val REQUEST_CODE = 2333

        const val AUTOFILLIDS = "AUTOFILLIDS"
        const val AUTOFILLHINTS = "AUTOFILLHINTS"
        const val AUTOFILLDATAS = "AUTOFILLDATAS"
    }


    private lateinit var needFills: MutableMap<Array<String> , AutofillId?>
    private lateinit var needSaves: MutableMap<Array<String> , String>
    private val sp by lazy {
        getSharedPreferences(PASSWORD_MANAGER , Context.MODE_PRIVATE)
    }

    // 触发自动填充时回调
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // Get the structure from the request
        val context: List<FillContext> = request.fillContexts
        val structures: List<AssistStructure> = context.map { it.structure }
        parseAssistStructure(structures , GETFILLS)
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        val idAuth = sp.getBoolean(IDAUTH , false)
        if(!this::needFills.isInitialized){
            callback.onFailure("needFills is null")
            return
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            request.inlineSuggestionsRequest?.let {
            }
        }
        val dataset = Dataset.Builder().also {
            for(entry in needFills.entries){
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU){
                    entry.value?.let {
                            autofillId ->
                        if(entry.key[0] == View.AUTOFILL_HINT_USERNAME){
                            usernameId = autofillId
                            sp.getString(View.AUTOFILL_HINT_USERNAME , "")?.let {
                                    username ->
                                if(username.isNotEmpty()) {
                                    it.setValue(
                                        autofillId,
                                        AutofillValue.forText(username),
                                        createPresentation("Form 1")
                                    )
                                }else{
                                    it.setValue(autofillId , null)
                                }
                            }

                        }else if(entry.key[0] == View.AUTOFILL_HINT_PASSWORD){
                            passwordId = autofillId
                            sp.getString(View.AUTOFILL_HINT_PASSWORD , "")?.let {
                                    password ->
                                if(password.isNotEmpty()){
                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                                        it.setValue(
                                            autofillId,
                                            AutofillValue.forText(sp.getString(View.AUTOFILL_HINT_PASSWORD , "")),
                                            createPresentation("Form 1")
                                        )
                                    }else{
                                        it.setValue(
                                            autofillId,
                                            AutofillValue.forText(sp.getString(View.AUTOFILL_HINT_PASSWORD , "")),
                                            createPresentation("Form 1"),
                                        )
                                    }
                                }else{
                                    it.setValue(autofillId , null)
                                }
                            }
                        } else {

                        }
                    }
                }else{
                    entry.value?.let { autofillId ->
                        if(entry.key[0] == View.AUTOFILL_HINT_USERNAME) {
                            usernameId = autofillId
                            it.setField(
                                autofillId,
                                Field.Builder()
                                    .also {
                                        sp.getString(View.AUTOFILL_HINT_USERNAME , "")?.let {
                                                username ->
                                            if(username.isNotEmpty()){
                                                it.setValue(AutofillValue.forText(username))
                                                    .setPresentations(
                                                        Presentations.Builder()
                                                            .setMenuPresentation(createPresentation("Form 1"))
                                                            .build()
                                                    )
                                            }
                                        }
                                    }
                                    .build()
                            )
                        }else if(entry.key[0] == View.AUTOFILL_HINT_PASSWORD) {
                            passwordId = autofillId
                            it.setField(
                                autofillId,
                                Field.Builder()
                                    .also {
                                        sp.getString(View.AUTOFILL_HINT_PASSWORD , "")?.let {
                                                password ->
                                            if(password.isNotEmpty()){
                                                it.setValue(AutofillValue.forText(password))
                                                    .setPresentations(
                                                        Presentations.Builder()
                                                            .setMenuPresentation(createPresentation("Form 1"))
                                                            .build()
                                                    )
                                            }
                                        }
                                    }
                                    .build()
                            )
                        }else{

                        }
                    }
                }
            }
        }.build()
        val fillResponse: FillResponse = FillResponse.Builder()
            .also {
                if(idAuth){
                    val intent = Intent(this, AuthActivity::class.java).apply {
                        val autoFillDatas = ArrayList<AutoFillData>().apply {
                            needFills.entries.forEach {
                               it.value?.let {
                                   autofillId ->
                                   add(AutoFillData(it.key[0] , autofillId , "Form 1")  )
                               }
                            }
                        }
                        putExtra(
                            AUTOFILLDATAS,
                            autoFillDatas
                        )
                    }
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                        it.setAuthentication(
                            needFills.values.toTypedArray(),
                            PendingIntent.getActivity(
                                this,
                                REQUEST_CODE,
                                intent,
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            ).intentSender,
                            Presentations.Builder()
                                .setMenuPresentation(createPresentation("Form 1"))
                                .build()
                        )
                    }else{
                        it.setAuthentication(
                            needFills.values.toTypedArray(),
                            PendingIntent.getActivity(
                                this,
                                REQUEST_CODE,
                                intent,
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            ).intentSender,
                            createPresentation("Form 1")
                        )
                    }
                }else{
                    it.addDataset(dataset)
                }
            }
            .setSaveInfo(SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                arrayOf(usernameId , passwordId)
            ).build())
            .build()

        callback.onSuccess(fillResponse)
    }

    // 获取所有AutoFill节点的HintType
    private fun parseAssistStructure(structures: List<AssistStructure> , type: Int){
        for (structure in structures){
            val windowNodeCount = structure.windowNodeCount
            for(windowNodeIndex in 0 until windowNodeCount){
                val windowNode = structure.getWindowNodeAt(windowNodeIndex)
                val viewNode = windowNode.rootViewNode
                parseViewNode(viewNode , type)
            }
        }
    }


    private fun parseViewNode(viewNode: ViewNode , type: Int){
        if (viewNode.autofillHints?.isNotEmpty() == true) {
            // If the client app provides autofill hints, you can obtain them using:
            // viewNode.getAutofillHints();
            viewNode.autofillHints?.also {
                if(type == GETFILLS){
                    if(!this::needFills.isInitialized){
                        needFills = HashMap()
                    }
                    needFills[it] = viewNode.autofillId
                }else{
                    if(!this::needSaves.isInitialized){
                        needSaves = HashMap()
                    }
                    needSaves[it] = "${viewNode.text}"
                }
            }
        } else {
            // Or use your own heuristics to describe the contents of a view
            // using methods such as getText() or getHint().
        }

        val children = viewNode.run {
            (0 until childCount).map { getChildAt(it) }
        }

        children.forEach {
            parseViewNode(it , type)
        }
    }

    // 触发保存时回调
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val context: List<FillContext> = request.fillContexts
        parseAssistStructure(context.map { it.structure } , GETSAVES)
            if(!this::needSaves.isInitialized){
                callback.onFailure("needSaves not init")
            }else{
                if(needSaves.entries.isEmpty()){
                    callback.onFailure("needSaves is null")
                    return
                }
                needSaves.forEach { (t, u) ->
                    val saveResult = sp.edit().putString(t[0] , u).commit()
                    if(!saveResult){
                        callback.onFailure("save fail")
                        return
                    }
                }
                callback.onSuccess()
            }

    }

    private fun createPresentation(name: String): RemoteViews {
        val presentation = RemoteViews(this.getPackageName(), android.R.layout.simple_list_item_1)
        presentation.setTextViewText(android.R.id.text1, name)
        return presentation
    }
}