/*
 * This file is part of InviZible Pro.
 *     InviZible Pro is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     InviZible Pro is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public License
 *     along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.
 *     Copyright 2019-2022 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt;
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

import static pan.alexander.tordnscrypt.TopFragment.appVersion;
import static pan.alexander.tordnscrypt.assistance.AccelerateDevelop.accelerated;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.FAULT;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.PROXY_MODE;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.ROOT_MODE;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.UNDEFINED;
import static pan.alexander.tordnscrypt.utils.enums.OperationMode.VPN_MODE;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import pan.alexander.tordnscrypt.appextension.PopupManager;
import pan.alexander.tordnscrypt.appextension.SubscriptionService;
import pan.alexander.tordnscrypt.arp.ArpScanner;
import pan.alexander.tordnscrypt.arp.ArpScannerKt;
import pan.alexander.tordnscrypt.arp.DNSRebindProtectionKt;
import pan.alexander.tordnscrypt.assistance.AccelerateDevelop;
import pan.alexander.tordnscrypt.backup.BackupActivity;
import pan.alexander.tordnscrypt.dialogs.ChangeModeDialog;
import pan.alexander.tordnscrypt.dialogs.NotificationDialogFragment;
import pan.alexander.tordnscrypt.dnscrypt_fragment.DNSCryptRunFragment;
import pan.alexander.tordnscrypt.help.HelpActivity;
import pan.alexander.tordnscrypt.itpd_fragment.ITPDRunFragment;
import pan.alexander.tordnscrypt.main_fragment.MainFragment;
import pan.alexander.tordnscrypt.main_fragment.ViewPagerAdapter;
import pan.alexander.tordnscrypt.modules.ModulesAux;
import pan.alexander.tordnscrypt.modules.ModulesKiller;
import pan.alexander.tordnscrypt.modules.ModulesRestarter;
import pan.alexander.tordnscrypt.modules.ModulesService;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.tor_fragment.TorRunFragment;
import pan.alexander.tordnscrypt.utils.ApManager;
import pan.alexander.tordnscrypt.utils.ChangeModeInterface;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.Registration;
import pan.alexander.tordnscrypt.utils.enums.ModuleState;
import pan.alexander.tordnscrypt.utils.enums.OperationMode;
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHelper;

public class MainActivity extends LangAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ChangeModeInterface {

    private static final int CODE_IS_AP_ON = 100;
    private static final int CODE_IS_VPN_ALLOWED = 110;

    public boolean childLockActive = false;
    public AccelerateDevelop accelerateDevelop;
    private volatile boolean vpnRequested;
    private Timer checkHotspotStateTimer;
    private Handler handler;
    private TopFragment topFragment;
    private DNSCryptRunFragment dNSCryptRunFragment;
    private TorRunFragment torRunFragment;
    private ITPDRunFragment iTPDRunFragment;
    private MainFragment mainFragment;
    private ModulesStatus modulesStatus;
    private ViewPager viewPager;
    private static int viewPagerPosition = 0;
    private MenuItem newIdentityMenuItem;
    private MenuItem firewallNavigationItem;
    private ImageView animatingImage;
    private RotateAnimation rotateAnimation;
    private BroadcastReceiver mainActivityReceiver;
    MenuItem proActivated;
    MenuItem pro;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setDayNightTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setBackgroundColor(getResources().getColor(R.color.colorBackground));
        navigationView.setNavigationItemSelectedListener(this);
        Menu navMenu = navigationView.getMenu();
        proActivated = navMenu.findItem(R.id.nav_pro_activated);
        proActivated.setVisible(false);
        pro = navMenu.findItem(R.id.nav_pro);
        pro.setVisible(true);

        modulesStatus = ModulesStatus.getInstance();

        changeDrawerWithVersionAndDestination(navigationView);

        viewPager = findViewById(R.id.viewPager);
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(4);

            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), ViewPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

            MainFragment mainFragment = new MainFragment();

            DNSCryptRunFragment dnsCryptRunFragment = new DNSCryptRunFragment();

            TorRunFragment torRunFragment = new TorRunFragment();
            ITPDRunFragment itpdRunFragment = new ITPDRunFragment();

            adapter.addFragment(new ViewPagerAdapter.ViewPagerFragment("Main", mainFragment));
            adapter.addFragment(new ViewPagerAdapter.ViewPagerFragment("DNS", dnsCryptRunFragment));
            adapter.addFragment(new ViewPagerAdapter.ViewPagerFragment("Tor", torRunFragment));
            adapter.addFragment(new ViewPagerAdapter.ViewPagerFragment("I2P", itpdRunFragment));

            viewPager.setAdapter(adapter);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);

            viewPager.setCurrentItem(viewPagerPosition);
        }

        Looper looper = Looper.getMainLooper();
        if (looper != null) {
            handler = new Handler(looper);
        }

        new PopupManager().onAppStarted(this);

        SubscriptionService.INSTANCE.init(this);

        SubscriptionService.INSTANCE.observeIsPurchasedState(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        vpnRequested = false;
        childLockActive = isInterfaceLocked();
        checkUpdates();
        handleMitmAttackWarning();
        registerBroadcastReceiver();

        if (appVersion.equals("gp")) {
            accelerateDevelop = new AccelerateDevelop(this);
            accelerateDevelop.initBilling();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (new PrefManager(this).getBoolPref("refresh_main_activity")) {
            new PrefManager(this).setBoolPref("refresh_main_activity", false);
            recreate();
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof DNSCryptRunFragment) {
            dNSCryptRunFragment = (DNSCryptRunFragment) fragment;
        } else if (fragment instanceof TorRunFragment) {
            torRunFragment = (TorRunFragment) fragment;
        } else if (fragment instanceof ITPDRunFragment) {
            iTPDRunFragment = (ITPDRunFragment) fragment;
        } else if (fragment instanceof TopFragment) {
            topFragment = (TopFragment) fragment;
        } else if (fragment instanceof MainFragment) {
            mainFragment = (MainFragment) fragment;
            mainFragment.setArguments(getIntent().getExtras());
        }
    }

    @SuppressWarnings("deprecation")
    private void setDayNightTheme() {
        try {

            String theme;
            if (appVersion.startsWith("g") && !accelerated) {
                theme = "1";
            } else {
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                theme = defaultSharedPreferences.getString("pref_fast_theme", "4");
            }

            switch (Objects.requireNonNull(theme)) {
                case "1":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case "2":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case "3":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME);
                    break;
                case "4":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "MainActivity setDayNightTheme exception " + e.getMessage() + " " + e.getCause());
        }
    }

    private void checkUpdates() {
        if (appVersion.equals("gp") || appVersion.equals("fd")) {
            return;
        }

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), "check_update")) {
            if (topFragment != null) {
                topFragment.checkNewVer(this, true);
            }

            intent.setAction(null);
            setIntent(intent);
        }
    }

    private void handleMitmAttackWarning() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(ArpScannerKt.mitmAttackWarning, false)
                && (ArpScanner.INSTANCE.getArpAttackDetected() || ArpScanner.INSTANCE.getDhcpGatewayAttackDetected())) {

            handler.postDelayed(() -> {
                DialogFragment commandResult = NotificationDialogFragment.newInstance(getString(R.string.notification_mitm));
                commandResult.show(getSupportFragmentManager(), "NotificationDialogFragment");
            }, 1000);
        }

        String site = intent.getStringExtra(DNSRebindProtectionKt.dnsRebindingWarning);
        if (site != null) {
            final String siteFinal = String.format(getString(R.string.notification_dns_rebinding_text), site);
            handler.postDelayed(() -> {
                DialogFragment commandResult = NotificationDialogFragment.newInstance(siteFinal);
                commandResult.show(getSupportFragmentManager(), "NotificationDialogFragment");
            }, 1200);
        }
    }

    private boolean isInterfaceLocked() {
        boolean locked = false;
        try {
            String saved_pass = new String(Base64.decode(new PrefManager(this).getStrPref("passwd"), 16));
            locked = saved_pass.contains("-l-o-c-k-e-d");
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "MainActivity Child Lock Exception " + e.getMessage());
        }
        return locked;
    }

    public DNSCryptRunFragment getDNSCryptRunFragment() {
        return dNSCryptRunFragment;
    }

    public TorRunFragment getTorRunFragment() {
        return torRunFragment;
    }

    public ITPDRunFragment getITPDRunFragment() {
        return iTPDRunFragment;
    }

    public MainFragment getMainFragment() {
        return mainFragment;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean rootIsAvailable = new PrefManager(this).getBoolPref("rootIsAvailable");

        switchIconsDependingOnMode(menu, rootIsAvailable);

        switchChildLockIcon(menu);

        if (rootIsAvailable) {
            switchApIcon(menu);
        }

        showNewTorIdentityIcon(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    public void onPurchase() {
        proActivated.setVisible(true);
        pro.setVisible(false);
        // todo check if shown
        Intent intent = new Intent(this, ProPurchaseSuccessActivity.class);
        startActivity(intent);
    }

    private void switchIconsDependingOnMode(Menu menu, boolean rootIsAvailable) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean fixTTL = sharedPreferences.getBoolean("pref_common_fix_ttl", false);
        boolean useModulesWithRoot = sharedPreferences.getBoolean("swUseModulesRoot", false);

        boolean busyBoxIsAvailable = new PrefManager(this).getBoolPref("bbOK");

        boolean mitmDetected = ArpScanner.INSTANCE.getArpAttackDetected()
                || ArpScanner.INSTANCE.getDhcpGatewayAttackDetected();

        fixTTL = fixTTL && !useModulesWithRoot;

        OperationMode mode = UNDEFINED;

        String operationMode = new PrefManager(this).getStrPref("OPERATION_MODE");
        if (!operationMode.isEmpty()) {
            mode = OperationMode.valueOf(operationMode);
        }


        MenuItem menuRootMode = menu.findItem(R.id.menu_root_mode);
        MenuItem menuVPNMode = menu.findItem(R.id.menu_vpn_mode);
        MenuItem menuProxiesMode = menu.findItem(R.id.menu_proxies_mode);

        MenuItem hotSpot = menu.findItem(R.id.item_hotspot);
        MenuItem rootIcon = menu.findItem(R.id.item_root);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            menuVPNMode.setEnabled(false);
            menuVPNMode.setVisible(false);
        }

        if (rootIsAvailable) {

            if (mode == ROOT_MODE) {
                menuRootMode.setChecked(true);
            } else if (mode == PROXY_MODE) {
                menuProxiesMode.setChecked(true);
            } else if (mode == VPN_MODE) {
                menuVPNMode.setChecked(true);
            } else {
                menuRootMode.setChecked(true);
                modulesStatus.setMode(ROOT_MODE);
                mode = ROOT_MODE;
                new PrefManager(this).setStrPref("OPERATION_MODE", mode.toString());
            }

            if (mitmDetected) {
                rootIcon.setIcon(R.drawable.ic_arp_attack_notification);
            } else if (mode == ROOT_MODE && fixTTL) {
                rootIcon.setIcon(R.drawable.ic_ttl_main);
            } else if (mode == ROOT_MODE && busyBoxIsAvailable) {
                rootIcon.setIcon(R.drawable.ic_done_all_white_24dp);
            } else if (mode == ROOT_MODE) {
                rootIcon.setIcon(R.drawable.ic_done_white_24dp);
            } else if (mode == PROXY_MODE) {
                rootIcon.setIcon(R.drawable.ic_warning_white_24dp);
            } else {
                rootIcon.setIcon(R.drawable.ic_vpn_key_white_24dp);
            }

            if (mode == ROOT_MODE) {
                hotSpot.setVisible(true);
                hotSpot.setEnabled(true);
            } else {
                hotSpot.setVisible(false);
                hotSpot.setEnabled(false);
            }

            menuRootMode.setVisible(true);
            menuRootMode.setEnabled(true);

        } else {

            if (mode == PROXY_MODE) {
                menuProxiesMode.setChecked(true);
            } else if (mode == VPN_MODE) {
                menuVPNMode.setChecked(true);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                menuVPNMode.setChecked(true);
                modulesStatus.setMode(VPN_MODE);
                mode = VPN_MODE;
                new PrefManager(this).setStrPref("OPERATION_MODE", mode.toString());
            } else {
                menuProxiesMode.setChecked(true);
                modulesStatus.setMode(PROXY_MODE);
                mode = PROXY_MODE;
                new PrefManager(this).setStrPref("OPERATION_MODE", mode.toString());
            }

            if (mitmDetected) {
                rootIcon.setIcon(R.drawable.ic_arp_attack_notification);
            } else if (mode == PROXY_MODE) {
                rootIcon.setIcon(R.drawable.ic_warning_white_24dp);
            } else {
                rootIcon.setIcon(R.drawable.ic_vpn_key_white_24dp);
            }

            hotSpot.setVisible(false);
            hotSpot.setEnabled(false);

            menuRootMode.setVisible(false);
            menuRootMode.setEnabled(false);
        }

        if ((mode == PROXY_MODE || mode == ROOT_MODE) && firewallNavigationItem != null) {
            firewallNavigationItem.setVisible(false);
        } else if (firewallNavigationItem != null) {
            firewallNavigationItem.setVisible(true);
        }
    }

    private void switchApIcon(Menu menu) {
        ApManager apManager = new ApManager(this);
        int apState = apManager.isApOn();

        if (apState == ApManager.apStateON) {

            menu.findItem(R.id.item_hotspot).setIcon(R.drawable.ic_wifi_tethering_green_24dp);

            if (!new PrefManager(this).getBoolPref("APisON")) {

                new PrefManager(this).setBoolPref("APisON", true);

                modulesStatus.setIptablesRulesUpdateRequested(true);
                ModulesAux.requestModulesStatusUpdate(this);

            }

        } else if (apState == ApManager.apStateOFF) {
            menu.findItem(R.id.item_hotspot).setIcon(R.drawable.ic_portable_wifi_off_white_24dp);
            if (new PrefManager(this).getBoolPref("APisON")) {
                new PrefManager(this).setBoolPref("APisON", false);

                modulesStatus.setIptablesRulesUpdateRequested(true);
                ModulesAux.requestModulesStatusUpdate(this);
            }
        } else {
            menu.findItem(R.id.item_hotspot).setVisible(false);
            menu.findItem(R.id.item_hotspot).setEnabled(false);
        }
    }

    private void switchChildLockIcon(Menu menu) {
        MenuItem childLock = menu.findItem(R.id.item_unlock);

        try {
            if (childLockActive) {
                childLock.setIcon(R.drawable.ic_lock_white_24dp);
            } else {
                childLock.setIcon(R.drawable.ic_lock_open_white_24dp);
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "MainActivity Child Lock Exeption " + e.getMessage());
        }
    }

    private void showNewTorIdentityIcon(Menu menu) {
        newIdentityMenuItem = menu.findItem(R.id.item_new_identity);
        if (newIdentityMenuItem == null || modulesStatus == null) {
            return;
        }
        newIdentityMenuItem.setVisible(modulesStatus.getTorState() != STOPPED
                && modulesStatus.getTorState() != ModuleState.UNDEFINED);
    }

    public void showNewTorIdentityIcon(boolean show) {
        if (newIdentityMenuItem == null || modulesStatus == null) {
            return;
        }
        newIdentityMenuItem.setVisible(show);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (isInterfaceLocked() && id != R.id.item_unlock) {
            Toast.makeText(this, getText(R.string.action_mode_dialog_locked), Toast.LENGTH_LONG).show();
            return false;
        }

        if (id == R.id.item_unlock) {
            if (isInterfaceLocked()) {
                childUnlock(item);
            } else {
                childLock(item);
            }
        } else if (id == R.id.item_hotspot) {
            switchHotspot();
        } else if (id == R.id.item_root) {
            showInfoAboutRoot();
        } else if (id == R.id.item_new_identity) {
            newTorIdentity();
        } else if (id == R.id.menu_root_mode) {
            switchToRootMode(item);
        } else if (id == R.id.menu_vpn_mode) {
            switchToVPNMode(item);
        } else if (id == R.id.menu_proxies_mode) {
            switchToProxyMode(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchHotspot() {
        try {
            ApManager apManager = new ApManager(this);
            if (apManager.configApState()) {
                checkHotspotState();
            } else if (!isFinishing()) {
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivityForResult(intent, CODE_IS_AP_ON);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "MainActivity switchHotspot exception " + e.getMessage() + " " + e.getCause());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "MainActivity onOptionsItemSelected exception " + e.getMessage() + " " + e.getCause());
        }
    }

    @SuppressLint("InflateParams")
    private void newTorIdentity() {
        if (modulesStatus != null && newIdentityMenuItem != null && modulesStatus.getTorState() == RUNNING) {

            if (rotateAnimation == null || animatingImage == null) {
                rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(1000);
                rotateAnimation.setRepeatCount(3);

                LayoutInflater inflater = getLayoutInflater();
                animatingImage = (ImageView) inflater.inflate(R.layout.icon_image_new_tor_identity, null);
            }

            if (rotateAnimation != null && animatingImage != null) {
                animatingImage.startAnimation(rotateAnimation);
                newIdentityMenuItem.setActionView(animatingImage);
            }

            ModulesRestarter.restartTor(this);

            if (isFinishing() || handler == null) {
                return;
            }

            handler.postDelayed(() -> {
                if (!isFinishing() && newIdentityMenuItem != null && newIdentityMenuItem.getActionView() != null) {
                    Toast.makeText(this, this.getText(R.string.toast_new_tor_identity), Toast.LENGTH_SHORT).show();
                    newIdentityMenuItem.getActionView().clearAnimation();
                    newIdentityMenuItem.setActionView(null);
                }
            }, 3000);
        }
    }

    private void switchToRootMode(MenuItem item) {
        ChangeModeDialog dialog = ChangeModeDialog.INSTANCE.getInstance(this, item, ROOT_MODE);
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "ChangeModeDialog");
        }
    }

    private void switchToProxyMode(MenuItem item) {
        ChangeModeDialog dialog = ChangeModeDialog.INSTANCE.getInstance(this, item, PROXY_MODE);
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "ChangeModeDialog");
        }
    }

    private void switchToVPNMode(MenuItem item) {
        ChangeModeDialog dialog = ChangeModeDialog.INSTANCE.getInstance(this, item, VPN_MODE);
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "ChangeModeDialog");
        }
    }

    private void checkHotspotState() {
        checkHotspotStateTimer = new Timer();
        checkHotspotStateTimer.scheduleAtFixedRate(new TimerTask() {
            int loop = 0;

            @Override
            public void run() {
                if (++loop > 3 && checkHotspotStateTimer != null) {
                    checkHotspotStateTimer.cancel();
                    checkHotspotStateTimer.purge();
                    checkHotspotStateTimer = null;
                }

                runOnUiThread(() -> invalidateOptionsMenu());
            }
        }, 3000, 5000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODE_IS_AP_ON) {
            checkHotspotState();
        }
        if (requestCode == CODE_IS_VPN_ALLOWED) {
            vpnRequested = false;
            startVPNService(resultCode);
        }
    }

    private void childLock(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle(R.string.action_mode_child_lock);
        builder.setMessage(R.string.action_mode_dialog_message_lock);
        builder.setIcon(R.drawable.ic_lock_outline_blue_24dp);

        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View inputView = inflater.inflate(R.layout.edit_text_for_dialog, null, false);
        final EditText input = inputView.findViewById(R.id.etForDialog);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final MainActivity mainActivity = this;

        String saved_pass = new PrefManager(getApplicationContext()).getStrPref("passwd");
        if (!saved_pass.isEmpty()) {
            saved_pass = new String(Base64.decode(saved_pass, 16));
            input.setText(saved_pass);
            input.setSelection(saved_pass.length());
        }
        builder.setView(inputView);

        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            if (input.getText().toString().equals("debug")) {
                TopFragment.debug = !TopFragment.debug;
                Toast.makeText(getApplicationContext(), "Debug mode " + TopFragment.debug, Toast.LENGTH_LONG).show();
            } else if (!input.getText().toString().trim().isEmpty()) {
                String pass = Base64.encodeToString((input.getText().toString() + "-l-o-c-k-e-d").getBytes(), 16);
                new PrefManager(getApplicationContext()).setStrPref("passwd", pass);
                Toast.makeText(getApplicationContext(), getText(R.string.action_mode_dialog_locked), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.ic_lock_white_24dp);
                childLockActive = true;

                DrawerLayout mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();
    }

    private void childUnlock(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle(R.string.action_mode_child_lock);
        builder.setMessage(R.string.action_mode_dialog_message_unlock);
        builder.setIcon(R.drawable.ic_lock_outline_blue_24dp);

        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View inputView = inflater.inflate(R.layout.edit_text_for_dialog, null, false);
        final EditText input = inputView.findViewById(R.id.etForDialog);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(inputView);

        final MainActivity mainActivity = this;

        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            String saved_pass = new String(Base64.decode(new PrefManager(getApplicationContext()).getStrPref("passwd"), 16));
            if (saved_pass.replace("-l-o-c-k-e-d", "").equals(input.getText().toString())) {
                String pass = Base64.encodeToString((input.getText().toString()).getBytes(), 16);
                new PrefManager(getApplicationContext()).setStrPref("passwd", pass);
                Toast.makeText(getApplicationContext(), getText(R.string.action_mode_dialog_unlocked), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.ic_lock_open_white_24dp);
                childLockActive = false;

                DrawerLayout mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else {
                childUnlock(item);
                Toast.makeText(getApplicationContext(), getText(R.string.action_mode_dialog_wrong_pass), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (isInterfaceLocked()) {
            Toast.makeText(this, getText(R.string.action_mode_dialog_locked), Toast.LENGTH_LONG).show();
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return false;
        }
        if (id == R.id.nav_backup) {
            Intent intent = new Intent(this, BackupActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_DNS_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("DNS_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_Tor_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("Tor_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_I2PD_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("I2PD_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_fast_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("fast_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_common_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("common_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_firewall) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("firewall");
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_Donate) {
            if (appVersion.startsWith("g")) {
                if (accelerateDevelop != null && !accelerated) {
                    accelerateDevelop.launchBilling(AccelerateDevelop.mSkuId);
                }
            } else {
                String link;
                if (Locale.getDefault().getLanguage().equalsIgnoreCase("ru")) {
                    link = "https://invizible.net/ru/donate/";
                } else {
                    link = "https://invizible.net/en/donate/";
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "MainActivity ACTION_VIEW exception " + e.getMessage() + " " + e.getCause());
                }

            }
        } else if (id == R.id.nav_Code) {
            Registration registration = new Registration(this);
            registration.showEnterCodeDialog();
        } else if (id == R.id.nav_pro) {
            Intent intent = new Intent(this, ProPurchaseActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_pro_activated) {
            Intent intent = new Intent(this, ProPurchaseSuccessActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showInfoAboutRoot() {
        boolean rootIsAvailable = new PrefManager(this).getBoolPref("rootIsAvailable");
        boolean busyBoxIsAvailable = new PrefManager(this).getBoolPref("bbOK");
        boolean mitmDetected = ArpScanner.INSTANCE.getArpAttackDetected()
                || ArpScanner.INSTANCE.getDhcpGatewayAttackDetected();

        if (mitmDetected) {
            DialogFragment commandResult = NotificationDialogFragment.newInstance(getString(R.string.notification_mitm));
            commandResult.show(getSupportFragmentManager(), "NotificationDialogFragment");
        } else if (rootIsAvailable) {
            DialogFragment commandResult;
            if (busyBoxIsAvailable) {
                commandResult = NotificationDialogFragment.newInstance(TopFragment.verSU + "\n\t\n" + TopFragment.verBB);
            } else {
                commandResult = NotificationDialogFragment.newInstance(TopFragment.verSU);
            }
            commandResult.show(getSupportFragmentManager(), "NotificationDialogFragment");
        } else {
            DialogFragment commandResult;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                commandResult = NotificationDialogFragment.newInstance(R.string.message_no_root_used);
            } else {
                commandResult = NotificationDialogFragment.newInstance(R.string.message_no_root_used_kitkat);
            }

            commandResult.show(getSupportFragmentManager(), "NotificationDialogFragment");
        }
    }

    @Override
    public void prepareVPNService() {
        Log.i(LOG_TAG, "MainActivity prepare VPN Service");
        final Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent == null) {
            startVPNService(RESULT_OK);
        } else if (!vpnRequested && !isFinishing()) {
            vpnRequested = true;
            try {
                startActivityForResult(prepareIntent, CODE_IS_VPN_ALLOWED);
            } catch (Exception e) {
                if (!isFinishing()) {
                    Toast.makeText(this, getString(R.string.wrong), Toast.LENGTH_SHORT).show();
                }
                Log.e(LOG_TAG, "Main Activity prepareVPNService exception " + e.getMessage() + " " + e.getCause());
            }
        }
    }

    private void startVPNService(int resultCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("VPNServiceEnabled", resultCode == RESULT_OK).apply();
        if (resultCode == RESULT_OK) {
            ServiceVPNHelper.start("VPN Service is Prepared", this);
            Toast.makeText(this, getText(R.string.vpn_mode_active), Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, getText(R.string.vpn_mode_off), Toast.LENGTH_LONG).show();
            ModulesAux.stopModulesIfRunning(this);
        }
    }

    public void changeDrawerWithVersionAndDestination(NavigationView navigationView) {
        if (navigationView == null) {
            return;
        }

        MenuItem item = navigationView.getMenu().findItem(R.id.nav_Donate);
        if ((appVersion.startsWith("g") && accelerated) || appVersion.startsWith("p") || appVersion.startsWith("f")) {
            if (item != null) {
                item.setVisible(false);
            }
        } else {
            if (item != null) {
                item.setVisible(true);

                if (appVersion.startsWith("g")) {
                    item.setTitle(R.string.premium);
                }
            }
        }

        item = navigationView.getMenu().findItem(R.id.nav_Code);
        if (appVersion.startsWith("l")) {
            if (item != null) {
                item.setVisible(true);
            }
        } else {
            if (item != null) {
                item.setVisible(false);
            }
        }

        firewallNavigationItem = navigationView.getMenu().findItem(R.id.nav_firewall);
    }

    private void registerBroadcastReceiver() {
        mainActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ArpScannerKt.mitmAttackWarning.equals(intent.getAction())) {
                    handler.post(() -> invalidateOptionsMenu());
                }
            }
        };

        IntentFilter mitmDetected = new IntentFilter(ArpScannerKt.mitmAttackWarning);
        LocalBroadcastManager.getInstance(this).registerReceiver(mainActivityReceiver, mitmDetected);
    }

    private void unregisterBroadcastReceiver() {
        if (mainActivityReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainActivityReceiver);
        }
    }

    @Override
    public void setFirewallNavigationItemVisible(boolean visibility) {
        firewallNavigationItem.setVisible(visibility);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceiver();
        if (accelerateDevelop != null) {
            accelerateDevelop.removeActivity();
            accelerateDevelop = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ModulesService.serviceIsRunning && modulesStatus.getMode() == VPN_MODE
                && (modulesStatus.getDnsCryptState() == STOPPED || modulesStatus.getDnsCryptState() == FAULT || modulesStatus.getDnsCryptState() == ModuleState.UNDEFINED)
                && (modulesStatus.getTorState() == STOPPED || modulesStatus.getTorState() == FAULT || modulesStatus.getTorState() == ModuleState.UNDEFINED)
                && (modulesStatus.getItpdState() == STOPPED || modulesStatus.getItpdState() == FAULT || modulesStatus.getItpdState() == ModuleState.UNDEFINED)) {
            Intent intent = new Intent(this, ModulesService.class);
            intent.setAction(ModulesService.actionStopService);
            startService(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (checkHotspotStateTimer != null) {
            checkHotspotStateTimer.cancel();
            checkHotspotStateTimer.purge();
            checkHotspotStateTimer = null;
        }

        if (viewPager != null) {
            viewPagerPosition = viewPager.getCurrentItem();
            viewPager = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        topFragment = null;
        dNSCryptRunFragment = null;
        torRunFragment = null;
        iTPDRunFragment = null;
        mainFragment = null;

        newIdentityMenuItem = null;
        animatingImage = null;
        rotateAnimation = null;
        SubscriptionService.INSTANCE.onDestroy();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && handler != null) {
            Log.e(LOG_TAG, "FORCE CLOSE ALL");

            Toast.makeText(this, "Force Close ...", Toast.LENGTH_LONG).show();

            ModulesKiller.forceCloseApp(PathVars.getInstance(this));

            handler.postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ModulesService.class);
                intent.setAction(ModulesService.actionStopService);
                startService(intent);
            }, 3000);
            handler.postDelayed(() -> System.exit(0), 5000);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
}
