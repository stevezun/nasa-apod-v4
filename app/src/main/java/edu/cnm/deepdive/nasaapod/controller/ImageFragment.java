package edu.cnm.deepdive.nasaapod.controller;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import edu.cnm.deepdive.nasaapod.R;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.model.entity.Apod.MediaType;
import edu.cnm.deepdive.nasaapod.viewmodel.MainViewModel;

public class ImageFragment extends Fragment {

  private static final int SCOPED_STORAGE_BUILD_VERSION = VERSION_CODES.Q;

  private WebView contentView;
  private Apod apod;
  private boolean showDownload = false;
  private MainViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    setRetainInstance(true);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container, Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_image, container, false);
    setupWebView(root);
    return root;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
    viewModel.getApod().observe(getViewLifecycleOwner(), (apod) -> {
      this.apod = apod;
      getActivity().invalidateOptionsMenu();
      viewModel.getImage(apod, contentView::loadUrl);
    });
    viewModel.getPermissions().observe(getViewLifecycleOwner(), (permissions) -> {
      boolean downloadAllowed = permissions.contains(WRITE_EXTERNAL_STORAGE);
      if (showDownload != downloadAllowed) {
        showDownload = downloadAllowed;
        getActivity().invalidateOptionsMenu();
      }
    });
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.image_options, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem download = menu.findItem(R.id.download);
    download.setVisible(
        apod != null
        && apod.getMediaType() == MediaType.IMAGE
        && (
            showDownload
            || VERSION.SDK_INT >= SCOPED_STORAGE_BUILD_VERSION
        )
    );
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    boolean handled = true;
    switch (item.getItemId()) {
      case R.id.info:
        showInfo();
        break;
      case R.id.download:
        downloadImage();
        break;
      default:
        handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  private void showInfo() {
    if (apod != null) {
      new InfoFragment().show(getChildFragmentManager(), InfoFragment.class.getName());
    }
  }

  private void downloadImage() {
    MainActivity activity = (MainActivity) getActivity();
    activity.setProgressVisibility(View.VISIBLE);
    viewModel.downloadImage(apod, () -> {
      activity.setProgressVisibility(View.GONE);
      activity.showToast(getString(R.string.image_downloaded));
    });
  }

  @SuppressLint({"SetJavaScriptEnabled"})
  private void setupWebView(View root) {
    contentView = root.findViewById(R.id.content_view);
    contentView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        ((MainActivity) getActivity()).showToast(apod.getTitle());
      }
    });
    WebSettings settings = contentView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setCacheMode(WebSettings.LOAD_DEFAULT);
    settings.setSupportZoom(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
  }

}
