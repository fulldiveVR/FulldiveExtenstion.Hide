/*
 * Copyright (c) 2022 FullDive
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pan.alexander.tordnscrypt.appextension

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import pan.alexander.tordnscrypt.R

object StringUtils {

    fun setTextColor(
        context: Context,
        string: SpannableString,
        @ColorRes colorRes: Int,
        startIndex: Int = 0,
        endIndex: Int = string.length
    ): SpannableString {
        if (string.isNotEmpty()) {
            string.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        context,
                        colorRes
                    )
                ),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return string
    }

    fun getSubstring(string: String): SubString {
        val openTag = "<u>"
        val closeTag = "</u>"
        val startIndex = string.indexOf(openTag)
        val substring = string.replace(openTag, "")
        val endIndex = substring.indexOf(closeTag)
        return SubString(substring.replace(closeTag, ""), startIndex, endIndex)
    }

    fun getTextWithLink(context: Context, text: String, callback: () -> Unit): SpannableString {
        val (substring, startIndex, endIndex) = getSubstring(text)
        val stringSpanned = SpannableString(substring)
        if (startIndex >= 0) {
            stringSpanned.setSpan(
                UnderlineSpan(),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            context.let {
                setTextColor(
                    it,
                    stringSpanned,
                    R.color.colorAccent,
                    startIndex,
                    endIndex
                )
            }
            stringSpanned.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        callback.invoke()
                    }
                },
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return stringSpanned
    }

    data class SubString(
        val text: String,
        val startIndex: Int,
        val endIndex: Int
    )
}