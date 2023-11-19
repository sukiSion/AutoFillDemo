package com.example.autofilldemo

import android.os.Parcelable
import android.view.autofill.AutofillId
import kotlinx.parcelize.Parcelize

/**
 * @author Sion
 * @date 2023/11/29 14:18
 * @version 1.0.0
 * @description
 **/
@Parcelize
data class AutoFillData(
    val autoFillHint: String,
    val autoFillId: AutofillId,
    val autoFillPresentationName: String
): Parcelable