// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class SelectTorTransportBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatRadioButton rbObfs3;

  @NonNull
  public final AppCompatRadioButton rbObfs4;

  @NonNull
  public final AppCompatRadioButton rbObfsNone;

  @NonNull
  public final AppCompatRadioButton rbObfsScrambleSuit;

  @NonNull
  public final RadioGroup rbgTorTransport;

  private SelectTorTransportBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatRadioButton rbObfs3, @NonNull AppCompatRadioButton rbObfs4,
      @NonNull AppCompatRadioButton rbObfsNone, @NonNull AppCompatRadioButton rbObfsScrambleSuit,
      @NonNull RadioGroup rbgTorTransport) {
    this.rootView = rootView;
    this.rbObfs3 = rbObfs3;
    this.rbObfs4 = rbObfs4;
    this.rbObfsNone = rbObfsNone;
    this.rbObfsScrambleSuit = rbObfsScrambleSuit;
    this.rbgTorTransport = rbgTorTransport;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static SelectTorTransportBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static SelectTorTransportBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.select_tor_transport, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static SelectTorTransportBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.rbObfs3;
      AppCompatRadioButton rbObfs3 = rootView.findViewById(id);
      if (rbObfs3 == null) {
        break missingId;
      }

      id = R.id.rbObfs4;
      AppCompatRadioButton rbObfs4 = rootView.findViewById(id);
      if (rbObfs4 == null) {
        break missingId;
      }

      id = R.id.rbObfsNone;
      AppCompatRadioButton rbObfsNone = rootView.findViewById(id);
      if (rbObfsNone == null) {
        break missingId;
      }

      id = R.id.rbObfsScrambleSuit;
      AppCompatRadioButton rbObfsScrambleSuit = rootView.findViewById(id);
      if (rbObfsScrambleSuit == null) {
        break missingId;
      }

      id = R.id.rbgTorTransport;
      RadioGroup rbgTorTransport = rootView.findViewById(id);
      if (rbgTorTransport == null) {
        break missingId;
      }

      return new SelectTorTransportBinding((LinearLayoutCompat) rootView, rbObfs3, rbObfs4,
          rbObfsNone, rbObfsScrambleSuit, rbgTorTransport);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}