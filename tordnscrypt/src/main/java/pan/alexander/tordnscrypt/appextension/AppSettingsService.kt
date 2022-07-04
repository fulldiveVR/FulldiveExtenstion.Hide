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

import android.annotation.SuppressLint
import android.content.Context

object AppSettingsService {

    private const val KEY_START_APP_COUNTER = "KEY_START_APP_COUNTER"
    private const val KEY_IS_CONGRATS_SHOW = "KEY_IS_CONGRATS_SHOW"
    private const val KEY_IS_PROMO_POPUP_CLOSED = "KEY_IS_PROMO_POPUP_CLOSED"
    private const val KEY_IS_PROMO_POPUP_CLOSED_START_COUNTER =
        "KEY_IS_PROMO_POPUP_CLOSED_START_COUNTER"

    fun getCurrentStartCounter(context: Context): Int {
        val sharedPreferences = context.getPrivateSharedPreferences()
        return sharedPreferences.getProperty(KEY_START_APP_COUNTER, 0).or(0)
    }

    fun setIsPromoPopupClosed(context: Context, isClosed: Boolean) {
        val sharedPreferences = context.getPrivateSharedPreferences()
        sharedPreferences.setProperty(KEY_IS_PROMO_POPUP_CLOSED, isClosed)
        sharedPreferences.setProperty(
            KEY_IS_PROMO_POPUP_CLOSED_START_COUNTER,
            getCurrentStartCounter(context)
        )
    }

    fun getIsPromoPopupClosed(context: Context): Boolean {
        val sharedPreferences = context.getPrivateSharedPreferences()
        return sharedPreferences.getProperty(KEY_IS_PROMO_POPUP_CLOSED, false).or(false)
    }

    fun getPromoCloseStartCounter(context: Context): Int {
        val sharedPreferences = context.getPrivateSharedPreferences()
        return sharedPreferences.getProperty(KEY_IS_PROMO_POPUP_CLOSED_START_COUNTER, 0).or(0)
    }

    fun getIsCongratsShow(context: Context): Boolean {
        val sharedPreferences = context.getPrivateSharedPreferences()
        return sharedPreferences.getProperty(KEY_IS_CONGRATS_SHOW, false).or(false)
    }

    fun setIsCongratsShow(context: Context, isShow: Boolean) {
        val sharedPreferences = context.getPrivateSharedPreferences()
        sharedPreferences.setProperty(KEY_IS_CONGRATS_SHOW, isShow)
    }
}