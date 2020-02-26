package edu.cnm.deepdive.nasaapod.controller;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import edu.cnm.deepdive.nasaapod.controller.DateTimePickerFragment.Mode;
import edu.cnm.deepdive.nasaapod.R;
import edu.cnm.deepdive.nasaapod.service.GoogleSignInRepository;
import edu.cnm.deepdive.nasaapod.viewmodel.MainViewModel;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements PermissionsFragment.OnAcknowledgeListener, DateTimePickerFragment.OnChangeListener {

  private static final int EXTERNAL_STORAGE_REQUEST_CODE = 1000;

  private MainViewModel viewModel;
  private NavController navController;
  private ProgressBar loading;
  private Calendar calendar;
  private BottomNavigationView navigator;
  private NavOptions navOptions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    loading = findViewById(R.id.loading);
    setupNavigation();
    setupViewModel();
    setupCalendarPicker();
    checkPermissions();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.main_options, menu);
    return true;
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    boolean handled = true;
    switch (item.getItemId()) {
      case R.id.sign_out:
        GoogleSignInRepository.getInstance().signOut()
            .addOnCompleteListener((task) -> {
              Intent intent = new Intent(this, LoginActivity.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            });
        break;
      default:
        handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == EXTERNAL_STORAGE_REQUEST_CODE) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int result = grantResults[i];
        if (result == PackageManager.PERMISSION_GRANTED) {
          viewModel.grantPermission(permission);
        } else {
          viewModel.revokePermission(permission);
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void onAcknowledge(String[] permissionsToRequest) {
    ActivityCompat.requestPermissions(this, permissionsToRequest, EXTERNAL_STORAGE_REQUEST_CODE);
  }

  @Override
  public void onChange(Calendar calendar) {
    loadApod(calendar.getTime());
  }

  public void loadApod(Date date) {
    setProgressVisibility(View.VISIBLE);
    viewModel.setApodDate(date);
  }

  public void setProgressVisibility(int visibility) {
    loading.setVisibility(visibility);
  }

  public void showToast(String message) {
    setProgressVisibility(View.GONE);
    Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.BOTTOM, 0,
        getResources().getDimensionPixelOffset(R.dimen.toast_vertical_margin));
    toast.show();
  }

  private void setupViewModel() {
    viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    viewModel.getApod().observe(this, (apod) -> {
      calendar.setTime(apod.getDate());
      navigateTo(R.id.navigation_image);
    });
    viewModel.getThrowable().observe(this, (throwable) -> {
      if (throwable != null) {
        showToast(getString(R.string.error_message, throwable.getMessage()));
      }
    });
    getLifecycle().addObserver(viewModel);
  }

  private void setupNavigation() {
    navOptions = new NavOptions.Builder()
        .setPopUpTo(R.id.navigation_map, true)
        .build();
    AppBarConfiguration appBarConfiguration =
        new AppBarConfiguration.Builder(R.id.navigation_image, R.id.navigation_history)
            .build();
    navController = Navigation.findNavController(this, R.id.nav_host_fragment);
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    navigator = findViewById(R.id.navigator);
    navigator.setOnNavigationItemSelectedListener((item) -> {
      navigateTo(item.getItemId());
      return true;
    });
  }

  private void setupCalendarPicker() {
    calendar = Calendar.getInstance();
    FloatingActionButton calendarFab = findViewById(R.id.calendar_fab);
    calendarFab.setOnClickListener((v) -> {
      DateTimePickerFragment fragment = DateTimePickerFragment.createInstance(Mode.DATE, calendar);
      fragment.show(getSupportFragmentManager(), fragment.getClass().getName());
    });
  }

  private void navigateTo(int itemId) {
    if (navController.getCurrentDestination().getId() != itemId) {
      navController.navigate(itemId, null, navOptions);
      if (navigator.getSelectedItemId() != itemId) {
        navigator.setSelectedItemId(itemId);
      }
    }
  }

  private void checkPermissions() {
    String[] permissions = null;
    try {
      PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),
          PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
      permissions = info.requestedPermissions;
    } catch (NameNotFoundException e) {
      throw new RuntimeException(e);
    }
    List<String> permissionsToRequest = new LinkedList<>();
    List<String> permissionsToExplain = new LinkedList<>();
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(this, permission)
          != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(permission);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
          permissionsToExplain.add(permission);
        }
      } else {
        viewModel.grantPermission(permission);
      }
    }
    if (!permissionsToExplain.isEmpty()) {
      explainPermissions(
          permissionsToExplain.toArray(new String[0]), permissionsToRequest.toArray(new String[0]));
    } else if (!permissionsToRequest.isEmpty()) {
      onAcknowledge(permissionsToRequest.toArray(new String[0]));
    }
  }

  private void explainPermissions(String[] permissionsToExplain, String[] permissionsToRequest) {
    PermissionsFragment fragment =
        PermissionsFragment.createInstance(permissionsToExplain, permissionsToRequest);
    fragment.show(getSupportFragmentManager(), fragment.getClass().getName());
  }

}
