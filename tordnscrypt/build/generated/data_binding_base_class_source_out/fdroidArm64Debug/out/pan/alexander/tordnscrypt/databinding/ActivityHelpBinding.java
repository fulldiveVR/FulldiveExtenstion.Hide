// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class ActivityHelpBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatButton btnSaveLogs;

  @NonNull
  public final CardView cardLogsButtons;

  @NonNull
  public final CardView cardLogsPath;

  @NonNull
  public final View dividerSaveLogs;

  @NonNull
  public final AppCompatEditText etLogsPath;

  @NonNull
  public final SwitchCompat swRootCommandsLog;

  @NonNull
  public final AppCompatTextView tvLogsPath;

  private ActivityHelpBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatButton btnSaveLogs, @NonNull CardView cardLogsButtons,
      @NonNull CardView cardLogsPath, @NonNull View dividerSaveLogs,
      @NonNull AppCompatEditText etLogsPath, @NonNull SwitchCompat swRootCommandsLog,
      @NonNull AppCompatTextView tvLogsPath) {
    this.rootView = rootView;
    this.btnSaveLogs = btnSaveLogs;
    this.cardLogsButtons = cardLogsButtons;
    this.cardLogsPath = cardLogsPath;
    this.dividerSaveLogs = dividerSaveLogs;
    this.etLogsPath = etLogsPath;
    this.swRootCommandsLog = swRootCommandsLog;
    this.tvLogsPath = tvLogsPath;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityHelpBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityHelpBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_help, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityHelpBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnSaveLogs;
      AppCompatButton btnSaveLogs = rootView.findViewById(id);
      if (btnSaveLogs == null) {
        break missingId;
      }

      id = R.id.cardLogsButtons;
      CardView cardLogsButtons = rootView.findViewById(id);
      if (cardLogsButtons == null) {
        break missingId;
      }

      id = R.id.cardLogsPath;
      CardView cardLogsPath = rootView.findViewById(id);
      if (cardLogsPath == null) {
        break missingId;
      }

      id = R.id.dividerSaveLogs;
      View dividerSaveLogs = rootView.findViewById(id);
      if (dividerSaveLogs == null) {
        break missingId;
      }

      id = R.id.etLogsPath;
      AppCompatEditText etLogsPath = rootView.findViewById(id);
      if (etLogsPath == null) {
        break missingId;
      }

      id = R.id.swRootCommandsLog;
      SwitchCompat swRootCommandsLog = rootView.findViewById(id);
      if (swRootCommandsLog == null) {
        break missingId;
      }

      id = R.id.tvLogsPath;
      AppCompatTextView tvLogsPath = rootView.findViewById(id);
      if (tvLogsPath == null) {
        break missingId;
      }

      return new ActivityHelpBinding((LinearLayoutCompat) rootView, btnSaveLogs, cardLogsButtons,
          cardLogsPath, dividerSaveLogs, etLogsPath, swRootCommandsLog, tvLogsPath);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
