// Generated by view binder compiler. Do not edit!
package pan.alexander.tordnscrypt.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import pan.alexander.tordnscrypt.R;

public final class FragmentCountrySelectBinding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final CardView cardTorCountryFragment;

  @NonNull
  public final AppCompatCheckBox chbTorCountriesSelectorAll;

  @NonNull
  public final LinearLayoutCompat linearLayout4;

  @NonNull
  public final FrameLayout linerLayout;

  @NonNull
  public final RecyclerView rvSelectCountries;

  @NonNull
  public final SearchView searhTorCountry;

  private FragmentCountrySelectBinding(@NonNull FrameLayout rootView,
      @NonNull CardView cardTorCountryFragment,
      @NonNull AppCompatCheckBox chbTorCountriesSelectorAll,
      @NonNull LinearLayoutCompat linearLayout4, @NonNull FrameLayout linerLayout,
      @NonNull RecyclerView rvSelectCountries, @NonNull SearchView searhTorCountry) {
    this.rootView = rootView;
    this.cardTorCountryFragment = cardTorCountryFragment;
    this.chbTorCountriesSelectorAll = chbTorCountriesSelectorAll;
    this.linearLayout4 = linearLayout4;
    this.linerLayout = linerLayout;
    this.rvSelectCountries = rvSelectCountries;
    this.searhTorCountry = searhTorCountry;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentCountrySelectBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentCountrySelectBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_country_select, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentCountrySelectBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.cardTorCountryFragment;
      CardView cardTorCountryFragment = rootView.findViewById(id);
      if (cardTorCountryFragment == null) {
        break missingId;
      }

      id = R.id.chbTorCountriesSelectorAll;
      AppCompatCheckBox chbTorCountriesSelectorAll = rootView.findViewById(id);
      if (chbTorCountriesSelectorAll == null) {
        break missingId;
      }

      id = R.id.linearLayout4;
      LinearLayoutCompat linearLayout4 = rootView.findViewById(id);
      if (linearLayout4 == null) {
        break missingId;
      }

      FrameLayout linerLayout = (FrameLayout) rootView;

      id = R.id.rvSelectCountries;
      RecyclerView rvSelectCountries = rootView.findViewById(id);
      if (rvSelectCountries == null) {
        break missingId;
      }

      id = R.id.searhTorCountry;
      SearchView searhTorCountry = rootView.findViewById(id);
      if (searhTorCountry == null) {
        break missingId;
      }

      return new FragmentCountrySelectBinding((FrameLayout) rootView, cardTorCountryFragment,
          chbTorCountriesSelectorAll, linearLayout4, linerLayout, rvSelectCountries,
          searhTorCountry);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
