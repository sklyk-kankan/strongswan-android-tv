package org.strongswan.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;

import org.strongswan.android.R;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.StrongSwanApplication;
import org.strongswan.android.logic.TrustedCertificateManager;
import org.strongswan.android.security.TrustedCertificateEntry;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TVMainActivity extends AppCompatActivity {

	private UUID uuid = UUID.fromString("1111-1111-1111-1111-1111");

	private TextView mServer;
	private TextView mUsername;
	private TextView mPassword;
	private Button mButton;
	private VpnProfile vpnProfile;
	private VpnProfileDataSource dataSource;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tv_main);

		dataSource = new VpnProfileDataSource(this);
		dataSource.open();

		mServer = findViewById(R.id.server);
		mUsername = findViewById(R.id.username);
		mPassword = findViewById(R.id.password);

		mButton = findViewById(R.id.connect);

		mButton.setOnClickListener(listener);


		/* load CA certificates in a background thread */
		((StrongSwanApplication)getApplication()).getExecutor().execute(() -> {
			TrustedCertificateManager.getInstance().load();
		});
	}

	private View.OnClickListener listener = v -> {
		String name = mServer.getText().toString();
		String username = mUsername.getText().toString();
		String password = mPassword.getText().toString();

		vpnProfile = dataSource.getVpnProfile(uuid);
		if (vpnProfile==null) {
			vpnProfile = new VpnProfile();
			vpnProfile.setUUID(uuid);
			vpnProfile.setVpnType(VpnType.IKEV2_EAP);
			vpnProfile.setName(name);
			vpnProfile.setGateway(name);
			vpnProfile.setUsername(username);
			vpnProfile.setPassword(password);
			vpnProfile.setCertificateAlias("kankan.sklykx.com");
			dataSource.insertProfile(vpnProfile);
		}else {
			vpnProfile.setName(name);
			vpnProfile.setGateway(name);
			dataSource.updateVpnProfile(vpnProfile);
		}

		try {
			KeyStore store = KeyStore.getInstance("LocalCertificateStore");
			store.load(null, null);
			if (! store.containsAlias(vpnProfile.getCertificateAlias())) {
				CertificateFactory factory = CertificateFactory.getInstance("X.509");
				InputStream in = getResources().openRawResource(R.raw.servercert);
				X509Certificate certificate = (X509Certificate)factory.generateCertificate(in);
				store.setCertificateEntry(null, certificate);
				TrustedCertificateManager.getInstance().reset();
			}
		} catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			e.printStackTrace();
		}

		Intent intent = new Intent(this, VpnProfileControlActivity.class);
		intent.setAction(VpnProfileControlActivity.START_PROFILE);
		intent.putExtra(VpnProfileControlActivity.EXTRA_VPN_PROFILE_ID, vpnProfile.getUUID().toString());
		startActivity(intent);
	};


	@Override
	protected void onDestroy() {
		super.onDestroy();

		dataSource.close();
	}

}
