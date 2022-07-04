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

package pan.alexander.tordnscrypt.analytics

object TrackerConstants {

    const val PARAM_SCREEN_KEY = "screen_key"
    const val PARAM_START_TIME = "start_time"
    const val PARAM_END_TIME = "end_time"
    const val PARAM_DURATION = "duration"
    const val FLURRY_SCREEN_TIME = "Fulldive.ScreenTime"
    const val FIREBASE_SCREEN_TIME = "screen_time"

    const val EVENT_PRO_TUTORIAL_PRO_POPUP_SHOWN = "pro_tutorial_pro_popup_shown"
    const val EVENT_PRO_TUTORIAL_OPENED_FROM_PRO_POPUP = "pro_tutorial_opened_from_pro_popup"
    const val EVENT_PRO_TUTORIAL_CLOSE_PRO_POPUP = "pro_tutorial_close_pro_popup"
    const val EVENT_PRO_TUTORIAL_OPENED_FROM_TOOLBAR = "pro_tutorial_opened_from_toolbar"
    const val EVENT_PRO_TUTORIAL_OPENED = "pro_tutorial_opened"
    const val EVENT_BUY_PRO_CLICKED = "buy_pro_clicked"
    const val EVENT_BUY_PRO_SUCCESS = "buy_pro_success"
}
