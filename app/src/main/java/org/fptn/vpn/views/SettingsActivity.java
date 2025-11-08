package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.fptn.vpn.R;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.database.model.SniDto;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.repository.SniRepository;
import org.fptn.vpn.services.CustomVpnService;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.adapter.FptnServerAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private ListView serverListView;

    private MutableLiveData<String> SNIMutableLiveData;

    private SwitchCompat permissionShowNotificationButton;
    private SwitchCompat permissionBatteryOptimizationButton;
    private SwitchCompat permissionBackgroundDataTransferButton;
    private BottomNavigationView bottomNavigationView;

    private SniRepository sniRepository;

    private FptnServerRepository fptnServerRepository;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    private AlertDialog autoSelectDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        SNIMutableLiveData = new MutableLiveData<>(getApplication().getString(R.string.default_sni));

        sniRepository = new SniRepository(this);
        fptnServerRepository = new FptnServerRepository(this);

        // Register the activity result launcher
        // This must be done in onCreate or as a class member initializer.
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            Log.d(TAG, "File selected: " + uri.getPath());
                            readFileContent(uri);
                        }
                    } else {
                        Log.w(TAG, "File selection cancelled.");
                    }
                });

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuSettings));

        fptnServerRepository.getAllServersLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                serverListView.setAdapter(new FptnServerAdapter(fptnServerDtos, R.layout.settings_server_list_item)); // NEED TO CHANGE THE ITEM LAYOUT
                setListViewHeightBasedOnChildren(serverListView);
            } else {
                // goto Login activity
                Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
        serverListView = findViewById(R.id.settings_servers_list);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            final String version = pInfo.versionName;

            TextView versionTextView = findViewById(R.id.settings_fptn_version);
            versionTextView.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't show app version! ", e);
        }

        // about
        TextView about = findViewById(R.id.settings_about);
        about.setText(Html.fromHtml(getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        // token's info
        TextView tokenInfo = findViewById(R.id.settings_token_info_html);
        tokenInfo.setText(Html.fromHtml(getString(R.string.settings_token_info_html), Html.FROM_HTML_MODE_LEGACY));
        tokenInfo.setMovementMethod(LinkMovementMethod.getInstance());

        // SNI editField
        TextView sniTextField = findViewById(R.id.SNI_text_field);
        SNIMutableLiveData.observe(this, sniTextField::setText);
        SNIMutableLiveData.postValue(SharedPrefUtils.getSniHostname(this));

        // SNI Auto
        TextView sniCountLabel = findViewById(R.id.loaded_sni_count_label);
        sniRepository.getSniCountLiveData().observe(this,
                count -> sniCountLabel.setText(String.valueOf(count)));

        Button loadSniButton = findViewById(R.id.load_sni_button);
        loadSniButton.setOnClickListener(view -> onLoadButtonClicked());

        Button deleteSniButton = findViewById(R.id.delete_sni_button);
        deleteSniButton.setOnClickListener(view -> onDeleteButtonClicked());

        Button autoSelectSniButton = findViewById(R.id.auto_select_sni_button);
        autoSelectSniButton.setOnClickListener(v -> onAutoSelectSniClicked());

        // Permission settings
        permissionShowNotificationButton = findViewById(R.id.permission_show_notification_button);
        permissionShowNotificationButton.setOnClickListener(view -> requestNotificationPermission());

        permissionBatteryOptimizationButton = findViewById(R.id.permission_battery_optimization_button);
        permissionBatteryOptimizationButton.setOnClickListener(view -> requestBatteryOptimisationPermission());

        permissionBackgroundDataTransferButton = findViewById(R.id.permission_background_data_transfer_button);
        permissionBackgroundDataTransferButton.setOnClickListener(view -> requestBackgroundDataTransferPermission());

        // Our sponsors
        TextView textView = findViewById(R.id.sponsors_list);
        textView.setText(Html.fromHtml(getString(R.string.sponsors_usernames)));

        // Set on click listeners
        View sniLayout = findViewById(R.id.current_sni_layout);
        sniLayout.setOnClickListener(this::onEditSNIServer);

        View updateTokenLayout = findViewById(R.id.update_token_layout);
        updateTokenLayout.setOnClickListener(this::onUpdateToken);

        View experimentalFeaturesLayout = findViewById(R.id.experimental_features_layout);
        experimentalFeaturesLayout.setOnClickListener(this::showExperimentalSettingsDialog);

        View logoutLayout = findViewById(R.id.logout_layout);
        logoutLayout.setOnClickListener(this::onLogout);
    }

    private void onAutoSelectSniClicked() {
        // Fetch the list of servers and then show the dialog
        fptnServerRepository.getAllServersLiveData().observe(this, servers -> {
            if (servers != null && !servers.isEmpty()) {
                showAutoSelectDialog(servers);
            } else {
                Toast.makeText(this, "No servers available to select.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAutoSelectDialog(List<FptnServerDto> servers) {
        // Prevent creating multiple dialogs
        if (autoSelectDialog != null && autoSelectDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the custom layout
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_autoselect_sni, null);
        builder.setView(dialogView);

        // --- Setup Spinner ---
        Spinner serverSpinner = dialogView.findViewById(R.id.dialog_server_spinner);

        List<String> serverNames = new ArrayList<>();
        for (FptnServerDto server : servers) {
            serverNames.add(server.getServerInfo());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serverNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverSpinner.setAdapter(spinnerAdapter);
        // --- End Spinner Setup ---

        // --- Setup Buttons ---
        Button buttonCancel = dialogView.findViewById(R.id.dialog_button_cancel);
        Button buttonStart = dialogView.findViewById(R.id.dialog_button_start);

        // Create the dialog before setting click listeners to allow for dismissing it
        autoSelectDialog = builder.create();

        buttonCancel.setOnClickListener(v -> {
            Log.d(TAG, "Auto-select dialog cancelled.");
            autoSelectDialog.dismiss();
        });

        buttonStart.setOnClickListener(v -> {
            // Get the originally selected server object
            int selectedPosition = serverSpinner.getSelectedItemPosition();
            FptnServerDto selectedServer = servers.get(selectedPosition);

            Log.d(TAG, "Starting SNI auto-select for server: " + selectedServer.getServerInfo());
            Toast.makeText(this, "Starting auto-select for " + selectedServer.getServerInfo(), Toast.LENGTH_SHORT).show();

            CustomVpnService.startSNISearch(this, selectedServer);
            autoSelectDialog.dismiss();
        });

        autoSelectDialog.show();
    }

    private void onDeleteButtonClicked() {
        sniRepository.deleteAll();
        Toast.makeText(this, "All loaded SNI have been deleted.", Toast.LENGTH_SHORT).show();
    }

    private void onLoadButtonClicked() {
        // Create an intent to open the file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // We are looking for any kind of file, but you could restrict it,
        // for example, to "text/plain" for text files.
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            // Launch the intent using the ActivityResultLauncher
            filePickerLauncher.launch(Intent.createChooser(intent, "Select a SNI file"));
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially handle the case where the device has no file manager
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readFileContent(Uri uri) {
        List<SniDto> sniList = new ArrayList<>();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Trim whitespace and ignore empty or commented lines
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                    sniList.add(new SniDto(trimmedLine));
                }
            }

            if (!sniList.isEmpty()) {
                sniRepository.insertAll(sniList);
                Log.d(TAG, "Successfully inserted " + sniList.size() + " SNIs into the database.");
                Toast.makeText(this, "Loaded " + sniList.size() + " SNI from file.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "No valid SNIs found in the selected file.");
                Toast.makeText(this, "File is empty or contains no valid SNI entries.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading SNI file", e);
            Toast.makeText(this, "Error: Could not read the file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        bottomNavigationView.setSelectedItemId(R.id.menuSettings);

        setPermissionButtonState(PermissionsUtils.checkNotificationPermission(this), permissionShowNotificationButton);
        setPermissionButtonState(PermissionsUtils.checkBatteryOptimizations(this), permissionBatteryOptimizationButton);
        setPermissionButtonState(PermissionsUtils.checkBackgroundDataTransferRestrictions(this), permissionBackgroundDataTransferButton);
    }

    private void setPermissionButtonState(boolean isGranted, SwitchCompat switchView) {
        switchView.setEnabled(true);
        if (isGranted) {
            switchView.setClickable(false);
            switchView.setChecked(true);
        } else {
            switchView.setClickable(true);
            switchView.setChecked(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        // Permission is not granted, show a dialog to explain reason
        new AlertDialog.Builder(this)
                .setTitle(R.string.notifications_request_title)
                .setMessage(R.string.notifications_request_reason)
                .setPositiveButton(R.string.grant, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setOnDismissListener(v -> permissionShowNotificationButton.setChecked(false))
                .create()
                .show();
    }

    private void requestBatteryOptimisationPermission() {
        new AlertDialog.Builder(this)
                // todo: add in settings show all needed restrictions granted?
                .setTitle(getString(R.string.battery_optimization_request_dialog_title))
                .setMessage(getString(R.string.battery_optimization_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Battery optimisation permission denied!");
                    permissionBatteryOptimizationButton.setChecked(false);
                })
                .show();
    }

    private void requestBackgroundDataTransferPermission() {
        /* If somebody worry about low speed in background - disable restriction on network transfer data*/
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.background_data_request_dialog_title))
                .setMessage(getString(R.string.background_data_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Background data transfer permission denied!");
                    permissionBackgroundDataTransferButton.setChecked(false);
                })
                .show();
    }

    public void onLogout(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    fptnServerRepository.deleteAllServers();
                    // goto Login activity
                    Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void onUpdateToken(View v) {
        // Goto update token
        Intent intent = new Intent(SettingsActivity.this, SettingsActivityUpdateToken.class);
        startActivity(intent);
    }

    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public void onEditSNIServer(View view) {
        View inflated = View.inflate(this, R.layout.sni_dialog_layout, null);
        TextInputEditText sniEditText = inflated.findViewById(R.id.text_edit_sni);
        SNIMutableLiveData.observe(this, sniEditText::setText);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(inflated);
        alertDialogBuilder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: save_button");
            Optional.ofNullable(sniEditText.getText())
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .ifPresent(newSni -> {
                        //todo: add validation?
                        Log.d(TAG, "new SNI: " + newSni);
                        SharedPrefUtils.saveSniHostname(this, newSni);
                        SNIMutableLiveData.postValue(newSni);
                    });
        });
        alertDialogBuilder.setNeutralButton(getString(R.string.reset_default_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: reset_default_button");
            SharedPrefUtils.resetToDefaultSniHostname(this);
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "onEditSNIServer: cancel_button");
        });
        alertDialogBuilder.show();
    }

    public void showExperimentalSettingsDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.experimental_features_label);
        builder.setIcon(R.drawable.ic_experimental_features_24);

        View dialogView = getLayoutInflater().inflate(R.layout.experimental_settings_dialog, null);
        builder.setView(dialogView);

        /* Reconnect on change network type */
        SwitchCompat switchNetworkType = dialogView.findViewById(R.id.reconnect_on_change_network_type_switch);
        switchNetworkType.setChecked(SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this));

        /* Reconnect on change IP address */
        SwitchCompat switchIPAddress = dialogView.findViewById(R.id.reconnect_on_change_ip_address_switch);
        switchIPAddress.setChecked(SharedPrefUtils.getReconnectOnChangeIPEnabled(this));

        /* Quick tile request */
        Button buttonRequestTile = dialogView.findViewById(R.id.quick_settings_tile_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buttonRequestTile.setOnClickListener(l -> {
                @SuppressLint("WrongConstant") StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                try {
                    // Request to add a custom tile service
                    statusBarManager.requestAddTileService(
                            new ComponentName(this, FptnTileService.class),
                            "FPTN",
                            Icon.createWithResource(this, R.drawable.ic_logo),
                            this.getMainExecutor(),
                            (resultCode) -> {
                                if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Log.d(TAG, "Tile already added successfully. Nothing to do.");
                                    Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT)
                                            .show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Log.d(TAG, "Tile added successfully.");
                                    Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Log.d(TAG, "User cancel request.");
                                }
                            }
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Failed to request tile addition", e);
                    Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT)
                            .show();
                }
            });
            buttonRequestTile.setEnabled(true);
            buttonRequestTile.setVisibility(View.VISIBLE);
        } else {
            buttonRequestTile.setEnabled(false);
            buttonRequestTile.setVisibility(View.INVISIBLE);
        }

        /* Reconnects attempts count */
        SeekBar seekBarAttemptsCount = dialogView.findViewById(R.id.seekBarAttemptsCount);
        TextView textViewAttemptsCount = dialogView.findViewById(R.id.textViewAttemptsCount);
        seekBarAttemptsCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 3) {
                    textViewAttemptsCount.setText("∞");
                } else {
                    String format = getString(R.string.reconnect_attempts_text);
                    textViewAttemptsCount.setText(String.format(format, progress * 5));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarAttemptsCount.setProgress(0);
        int reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        if (reconnectAttemptsCount == Integer.MAX_VALUE) {
            seekBarAttemptsCount.setProgress(3);
        } else {
            seekBarAttemptsCount.setProgress(reconnectAttemptsCount / 5);
        }

        /* Reconnects delay between in seconds */
        SeekBar seekBarDelayBetween = dialogView.findViewById(R.id.seekBarDelayBetween);
        TextView textViewDelayBetween = dialogView.findViewById(R.id.textViewDelayBetween);
        seekBarDelayBetween.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String format = getString(R.string.delay_between_attempts_seconds);
                textViewDelayBetween.setText(String.format(format, progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(this);
        seekBarDelayBetween.setProgress(0);
        seekBarDelayBetween.setProgress(delayBetweenReconnect - 1);


        /* Reset selected server on disconnect */
        SwitchCompat resetServerAfterDisconnectSwitch = dialogView.findViewById(R.id.reset_selected_server_after_disconnect_switch);
        resetServerAfterDisconnectSwitch.setChecked(SharedPrefUtils.getResetSelectedServerEnabled(this));

        builder.setPositiveButton(getString(R.string.save_button), (dialog, which) -> {
            Log.d(TAG, "experimentalFeaturesDialog: save");
            SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, switchNetworkType.isChecked());
            SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, switchIPAddress.isChecked());
            SharedPrefUtils.saveResetSelectedServerEnabled(this, resetServerAfterDisconnectSwitch.isChecked());

            int attemptsCountProgress = seekBarAttemptsCount.getProgress();
            if (attemptsCountProgress == 3) {
                SharedPrefUtils.saveReconnectAttemptsCount(this, Integer.MAX_VALUE);
            } else {
                SharedPrefUtils.saveReconnectAttemptsCount(this, attemptsCountProgress * 5);
            }

            int delayBetweenProgress = seekBarDelayBetween.getProgress();
            SharedPrefUtils.saveDelayBetweenReconnect(this, delayBetweenProgress + 1);
        });
        builder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "experimentalFeaturesDialog: cancel");
            dialog.dismiss();
        });

        builder.create().show();
    }
}
