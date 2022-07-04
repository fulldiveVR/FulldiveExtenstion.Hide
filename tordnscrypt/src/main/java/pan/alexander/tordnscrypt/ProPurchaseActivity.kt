/*
    This file is part of VPN.

    VPN is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    VPN is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with VPN.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/
package pan.alexander.tordnscrypt

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pan.alexander.tordnscrypt.analytics.StatisticHelper
import pan.alexander.tordnscrypt.analytics.StatisticHelper.logAction
import pan.alexander.tordnscrypt.analytics.TrackerConstants
import pan.alexander.tordnscrypt.appextension.StringUtils.getHexColor
import pan.alexander.tordnscrypt.appextension.StringUtils.processCheckmarks
import pan.alexander.tordnscrypt.appextension.SubscriptionService
import pan.alexander.tordnscrypt.appextension.fromHtmlToSpanned

class ProPurchaseActivity : AppCompatActivity() {
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_tutorial)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val title = fromHtmlToSpanned(
            getString(R.string.str_subscription_tutorial_title).replace(
                "%tutorialTextColor%",
                getHexColor(baseContext, R.color.colorPrimary)
            )
        )
        val tutorial =
            SpannableString.valueOf(baseContext.getString(R.string.str_subscription_tutorial_checkmarks))
        processCheckmarks(baseContext, tutorial)
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = title
        val tutorialTextView = findViewById<TextView>(R.id.tutorialTextView)
        tutorialTextView.text = tutorial

        val discountCurrencyTextView = findViewById<TextView>(R.id.discountCurrencyTextView)
        val fullPriceCurrencyTextView = findViewById<TextView>(R.id.fullPriceCurrencyTextView)
        val discountTextView = findViewById<TextView>(R.id.discountTextView)
        val fullPriceTextView = findViewById<TextView>(R.id.fullPriceTextView)
        val subscribeButton = findViewById<TextView>(R.id.subscribeButton)

        val proSubscriptionInfo = SubscriptionService.getProSubscriptionInfo()
        discountCurrencyTextView.text = proSubscriptionInfo.currency
        fullPriceCurrencyTextView.text = proSubscriptionInfo.currency
        discountTextView.text = proSubscriptionInfo.salePrice
        fullPriceTextView.text = proSubscriptionInfo.price
        subscribeButton.setOnClickListener {
            StatisticHelper.logAction(TrackerConstants.EVENT_BUY_PRO_CLICKED)
            SubscriptionService.purchase(this)
            onBackPressed()
        }
        StatisticHelper.logAction(TrackerConstants.EVENT_PRO_TUTORIAL_OPENED)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // API 5+ solution
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onResume() {
        super.onResume()
        setTitle(R.string.str_subscription_title)


//        tvHelpBuildNo.setText(BuildConfig.VERSION_NAME);
//
//        Date buildDate = BuildConfig.BUILD_TIME;
//        tvHelpBuildDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(buildDate));
//
//        findViewById(R.id.dnscryptLicense).setOnClickListener(this);
//        findViewById(R.id.torLicense).setOnClickListener(this);
//        findViewById(R.id.itpdLicense).setOnClickListener(this);
//        findViewById(R.id.libsuperuserLicense).setOnClickListener(this);
//        findViewById(R.id.androidShellLicense).setOnClickListener(this);
//        findViewById(R.id.netGuardLicense).setOnClickListener(this);
//        findViewById(R.id.filepickerLicense).setOnClickListener(this);
//        findViewById(R.id.busyboxLicense).setOnClickListener(this);
    }
}