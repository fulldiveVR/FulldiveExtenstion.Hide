// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class FragmentProxyBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatButton btnProxySave;

  @NonNull
  public final AppCompatButton btnSelectWoProxyApps;

  @NonNull
  public final AppCompatCheckBox chbProxyDNSCrypt;

  @NonNull
  public final AppCompatCheckBox chbProxyITPD;

  @NonNull
  public final AppCompatCheckBox chbProxyTor;

  @NonNull
  public final View divider2;

  @NonNull
  public final View divider3;

  @NonNull
  public final AppCompatEditText etProxyPass;

  @NonNull
  public final AppCompatEditText etProxyPort;

  @NonNull
  public final AppCompatEditText etProxyServer;

  @NonNull
  public final AppCompatEditText etProxyUserName;

  @NonNull
  public final NestedScrollView scrollProxy;

  @NonNull
  public final AppCompatTextView tvProxyHint;

  @NonNull
  public final AppCompatTextView tvProxyPass;

  @NonNull
  public final AppCompatTextView tvProxyPort;

  @NonNull
  public final AppCompatTextView tvProxyServer;

  @NonNull
  public final AppCompatTextView tvProxyUserName;

  @NonNull
  public final AppCompatTextView tvSelectWoProxyApps;

  private FragmentProxyBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatButton btnProxySave, @NonNull AppCompatButton btnSelectWoProxyApps,
      @NonNull AppCompatCheckBox chbProxyDNSCrypt, @NonNull AppCompatCheckBox chbProxyITPD,
      @NonNull AppCompatCheckBox chbProxyTor, @NonNull View divider2, @NonNull View divider3,
      @NonNull AppCompatEditText etProxyPass, @NonNull AppCompatEditText etProxyPort,
      @NonNull AppCompatEditText etProxyServer, @NonNull AppCompatEditText etProxyUserName,
      @NonNull NestedScrollView scrollProxy, @NonNull AppCompatTextView tvProxyHint,
      @NonNull AppCompatTextView tvProxyPass, @NonNull AppCompatTextView tvProxyPort,
      @NonNull AppCompatTextView tvProxyServer, @NonNull AppCompatTextView tvProxyUserName,
      @NonNull AppCompatTextView tvSelectWoProxyApps) {
    this.rootView = rootView;
    this.btnProxySave = btnProxySave;
    this.btnSelectWoProxyApps = btnSelectWoProxyApps;
    this.chbProxyDNSCrypt = chbProxyDNSCrypt;
    this.chbProxyITPD = chbProxyITPD;
    this.chbProxyTor = chbProxyTor;
    this.divider2 = divider2;
    this.divider3 = divider3;
    this.etProxyPass = etProxyPass;
    this.etProxyPort = etProxyPort;
    this.etProxyServer = etProxyServer;
    this.etProxyUserName = etProxyUserName;
    this.scrollProxy = scrollProxy;
    this.tvProxyHint = tvProxyHint;
    this.tvProxyPass = tvProxyPass;
    this.tvProxyPort = tvProxyPort;
    this.tvProxyServer = tvProxyServer;
    this.tvProxyUserName = tvProxyUserName;
    this.tvSelectWoProxyApps = tvSelectWoProxyApps;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentProxyBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentProxyBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_proxy, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentProxyBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnProxySave;
      AppCompatButton btnProxySave = rootView.findViewById(id);
      if (btnProxySave == null) {
        break missingId;
      }

      id = R.id.btnSelectWoProxyApps;
      AppCompatButton btnSelectWoProxyApps = rootView.findViewById(id);
      if (btnSelectWoProxyApps == null) {
        break missingId;
      }

      id = R.id.chbProxyDNSCrypt;
      AppCompatCheckBox chbProxyDNSCrypt = rootView.findViewById(id);
      if (chbProxyDNSCrypt == null) {
        break missingId;
      }

      id = R.id.chbProxyITPD;
      AppCompatCheckBox chbProxyITPD = rootView.findViewById(id);
      if (chbProxyITPD == null) {
        break missingId;
      }

      id = R.id.chbProxyTor;
      AppCompatCheckBox chbProxyTor = rootView.findViewById(id);
      if (chbProxyTor == null) {
        break missingId;
      }

      id = R.id.divider2;
      View divider2 = rootView.findViewById(id);
      if (divider2 == null) {
        break missingId;
      }

      id = R.id.divider3;
      View divider3 = rootView.findViewById(id);
      if (divider3 == null) {
        break missingId;
      }

      id = R.id.etProxyPass;
      AppCompatEditText etProxyPass = rootView.findViewById(id);
      if (etProxyPass == null) {
        break missingId;
      }

      id = R.id.etProxyPort;
      AppCompatEditText etProxyPort = rootView.findViewById(id);
      if (etProxyPort == null) {
        break missingId;
      }

      id = R.id.etProxyServer;
      AppCompatEditText etProxyServer = rootView.findViewById(id);
      if (etProxyServer == null) {
        break missingId;
      }

      id = R.id.etProxyUserName;
      AppCompatEditText etProxyUserName = rootView.findViewById(id);
      if (etProxyUserName == null) {
        break missingId;
      }

      id = R.id.scrollProxy;
      NestedScrollView scrollProxy = rootView.findViewById(id);
      if (scrollProxy == null) {
        break missingId;
      }

      id = R.id.tvProxyHint;
      AppCompatTextView tvProxyHint = rootView.findViewById(id);
      if (tvProxyHint == null) {
        break missingId;
      }

      id = R.id.tvProxyPass;
      AppCompatTextView tvProxyPass = rootView.findViewById(id);
      if (tvProxyPass == null) {
        break missingId;
      }

      id = R.id.tvProxyPort;
      AppCompatTextView tvProxyPort = rootView.findViewById(id);
      if (tvProxyPort == null) {
        break missingId;
      }

      id = R.id.tvProxyServer;
      AppCompatTextView tvProxyServer = rootView.findViewById(id);
      if (tvProxyServer == null) {
        break missingId;
      }

      id = R.id.tvProxyUserName;
      AppCompatTextView tvProxyUserName = rootView.findViewById(id);
      if (tvProxyUserName == null) {
        break missingId;
      }

      id = R.id.tvSelectWoProxyApps;
      AppCompatTextView tvSelectWoProxyApps = rootView.findViewById(id);
      if (tvSelectWoProxyApps == null) {
        break missingId;
      }

      return new FragmentProxyBinding((LinearLayoutCompat) rootView, btnProxySave,
          btnSelectWoProxyApps, chbProxyDNSCrypt, chbProxyITPD, chbProxyTor, divider2, divider3,
          etProxyPass, etProxyPort, etProxyServer, etProxyUserName, scrollProxy, tvProxyHint,
          tvProxyPass, tvProxyPort, tvProxyServer, tvProxyUserName, tvSelectWoProxyApps);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
