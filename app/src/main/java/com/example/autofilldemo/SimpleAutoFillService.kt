package com.example.autofilldemo

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import java.lang.reflect.Array
import java.util.Arrays

class SimpleAutoFillService: AutofillService() {
    // 触发自动填充时回调
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // Get the structure from the request
        val context: List<FillContext> = request.fillContexts
        val structures: List<AssistStructure> = context.map { it.structure }
        parseAssistStructure(structures)
    }

    // 获取所有AutoFill节点的HintType
    private fun parseAssistStructure(structures: List<AssistStructure>){
        for (structure in structures){
            val windowNodeCount = structure.windowNodeCount
            for(windowNodeIndex in 0 until windowNodeCount){
                val windowNode = structure.getWindowNodeAt(windowNodeIndex)
                val viewNode = windowNode.rootViewNode
                parseViewNode(viewNode)
            }
        }
    }


    private fun parseViewNode(viewNode: ViewNode){
        if (viewNode.autofillHints?.isNotEmpty() == true) {
            // If the client app provides autofill hints, you can obtain them using:
            // viewNode.getAutofillHints();
            viewNode.autofillHints?.forEach {

            }
            Log.e("Sion" , Arrays.toString(viewNode.autofillHints))
            Log.e("Sion" ,"${viewNode.autofillId}")
        } else {
            // Or use your own heuristics to describe the contents of a view
            // using methods such as getText() or getHint().

        }

        val children = viewNode.run {
            (0 until childCount).map { getChildAt(it) }
        }

        children.forEach {
            parseViewNode(it)
        }
    }

    // 触发保存时回调
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
    }
}