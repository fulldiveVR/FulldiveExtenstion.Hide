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
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class FragmentBackupBinding implements ViewBinding {
  @NonNull
  private final LinearLayoutCompat rootView;

  @NonNull
  public final AppCompatButton btnResetSettings;

  @NonNull
  public final AppCompatButton btnRestoreBackup;

  @NonNull
  public final AppCompatButton btnSaveBackup;

  @NonNull
  public final CardView cardBackupButtons;

  @NonNull
  public final CardView cardBackupPath;

  @NonNull
  public final View divider4;

  @NonNull
  public final View divider5;

  @NonNull
  public final AppCompatEditText etPathBackup;

  @NonNull
  public final LinearLayoutCompat llCardBackup;

  @NonNull
  public final LinearLayoutCompat llFragmentBackup;

  @NonNull
  public final AppCompatTextView textView2;

  private FragmentBackupBinding(@NonNull LinearLayoutCompat rootView,
      @NonNull AppCompatButton btnResetSettings, @NonNull AppCompatButton btnRestoreBackup,
      @NonNull AppCompatButton btnSaveBackup, @NonNull CardView cardBackupButtons,
      @NonNull CardView cardBackupPath, @NonNull View divider4, @NonNull View divider5,
      @NonNull AppCompatEditText etPathBackup, @NonNull LinearLayoutCompat llCardBackup,
      @NonNull LinearLayoutCompat llFragmentBackup, @NonNull AppCompatTextView textView2) {
    this.rootView = rootView;
    this.btnResetSettings = btnResetSettings;
    this.btnRestoreBackup = btnRestoreBackup;
    this.btnSaveBackup = btnSaveBackup;
    this.cardBackupButtons = cardBackupButtons;
    this.cardBackupPath = cardBackupPath;
    this.divider4 = divider4;
    this.divider5 = divider5;
    this.etPathBackup = etPathBackup;
    this.llCardBackup = llCardBackup;
    this.llFragmentBackup = llFragmentBackup;
    this.textView2 = textView2;
  }

  @Override
  @NonNull
  public LinearLayoutCompat getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentBackupBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentBackupBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_backup, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentBackupBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnResetSettings;
      AppCompatButton btnResetSettings = rootView.findViewById(id);
      if (btnResetSettings == null) {
        break missingId;
      }

      id = R.id.btnRestoreBackup;
      AppCompatButton btnRestoreBackup = rootView.findViewById(id);
      if (btnRestoreBackup == null) {
        break missingId;
      }

      id = R.id.btnSaveBackup;
      AppCompatButton btnSaveBackup = rootView.findViewById(id);
      if (btnSaveBackup == null) {
        break missingId;
      }

      id = R.id.cardBackupButtons;
      CardView cardBackupButtons = rootView.findViewById(id);
      if (cardBackupButtons == null) {
        break missingId;
      }

      id = R.id.cardBackupPath;
      CardView cardBackupPath = rootView.findViewById(id);
      if (cardBackupPath == null) {
        break missingId;
      }

      id = R.id.divider4;
      View divider4 = rootView.findViewById(id);
      if (divider4 == null) {
        break missingId;
      }

      id = R.id.divider5;
      View divider5 = rootView.findViewById(id);
      if (divider5 == null) {
        break missingId;
      }

      id = R.id.etPathBackup;
      AppCompatEditText etPathBackup = rootView.findViewById(id);
      if (etPathBackup == null) {
        break missingId;
      }

      id = R.id.llCardBackup;
      LinearLayoutCompat llCardBackup = rootView.findViewById(id);
      if (llCardBackup == null) {
        break missingId;
      }

      id = R.id.llFragmentBackup;
      LinearLayoutCompat llFragmentBackup = rootView.findViewById(id);
      if (llFragmentBackup == null) {
        break missingId;
      }

      id = R.id.textView2;
      AppCompatTextView textView2 = rootView.findViewById(id);
      if (textView2 == null) {
        break missingId;
      }

      return new FragmentBackupBinding((LinearLayoutCompat) rootView, btnResetSettings,
          btnRestoreBackup, btnSaveBackup, cardBackupButtons, cardBackupPath, divider4, divider5,
          etPathBackup, llCardBackup, llFragmentBackup, textView2);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
