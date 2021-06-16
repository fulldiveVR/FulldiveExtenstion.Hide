// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class TorTransportCodeImageBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatEditText etCode;

  @NonNull
  public final AppCompatImageView imgCode;

  private TorTransportCodeImageBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatEditText etCode, @NonNull AppCompatImageView imgCode) {
    this.rootView = rootView;
    this.etCode = etCode;
    this.imgCode = imgCode;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static TorTransportCodeImageBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static TorTransportCodeImageBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.tor_transport_code_image, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static TorTransportCodeImageBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.etCode;
      AppCompatEditText etCode = rootView.findViewById(id);
      if (etCode == null) {
        break missingId;
      }

      id = R.id.imgCode;
      AppCompatImageView imgCode = rootView.findViewById(id);
      if (imgCode == null) {
        break missingId;
      }

      return new TorTransportCodeImageBinding((LinearLayoutCompat) rootView, etCode, imgCode);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}