// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class AddOwnServerBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatEditText etOwnServerDescription;

  @NonNull
  public final AppCompatEditText etOwnServerName;

  @NonNull
  public final AppCompatEditText etOwnServerSDNS;

  private AddOwnServerBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatEditText etOwnServerDescription, @NonNull AppCompatEditText etOwnServerName,
      @NonNull AppCompatEditText etOwnServerSDNS) {
    this.rootView = rootView;
    this.etOwnServerDescription = etOwnServerDescription;
    this.etOwnServerName = etOwnServerName;
    this.etOwnServerSDNS = etOwnServerSDNS;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static AddOwnServerBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static AddOwnServerBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.add_own_server, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static AddOwnServerBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.etOwnServerDescription;
      AppCompatEditText etOwnServerDescription = rootView.findViewById(id);
      if (etOwnServerDescription == null) {
        break missingId;
      }

      id = R.id.etOwnServerName;
      AppCompatEditText etOwnServerName = rootView.findViewById(id);
      if (etOwnServerName == null) {
        break missingId;
      }

      id = R.id.etOwnServerSDNS;
      AppCompatEditText etOwnServerSDNS = rootView.findViewById(id);
      if (etOwnServerSDNS == null) {
        break missingId;
      }

      return new AddOwnServerBinding((LinearLayoutCompat) rootView, etOwnServerDescription,
          etOwnServerName, etOwnServerSDNS);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}