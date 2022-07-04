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
import pan.alexander.tordnscrypt.appextension.AppSettingsService
import pan.alexander.tordnscrypt.appextension.StringUtils.getHexColor
import pan.alexander.tordnscrypt.appextension.StringUtils.processCheckmarks
import pan.alexander.tordnscrypt.appextension.fromHtmlToSpanned

class ProPurchaseSuccessActivity : AppCompatActivity() {
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_success)
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val title = fromHtmlToSpanned(
            getString(R.string.str_subscription_congrats_title).replace(
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

        AppSettingsService.setIsCongratsShow(baseContext, true)
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