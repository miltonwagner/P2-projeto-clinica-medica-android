package com.example.clinicavidaplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Nossa clínica: mapa OpenStreetMap (osmdroid, sem API Key) com o pin fixo da clínica
 * e o marcador do usuário atualizado pelo GPS (FusedLocationProviderClient) a cada 3 s.
 */
public class EnderecoActivity extends AppCompatActivity {

    private static final String ENDERECO = "Rua Bernardino de Campos, 933, Centro, Indaiatuba, SP";
    private static final GeoPoint CLINICA = new GeoPoint(-23.0887583, -47.2159389);
    private static final long INTERVALO_GPS_MS = 3000;


    private MapView mapa;
    private Marker marcadorUsuario;
    private TextView txtDistancia;
    private FusedLocationProviderClient gps;
    private boolean enquadrou = false;

    private final LocationCallback aoMover = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            Location local = resultado.getLastLocation();
            if (local != null) {
                mostrarUsuario(local);
            }
        }
    };

    private final ActivityResultLauncher<String[]> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::aoResponderPermissao);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid: identifica o app nos servidores de mapa (pacote "com.example" é bloqueado
        // pelos servidores do OpenStreetMap) e guarda o cache na pasta do app.
        Configuration.getInstance().setUserAgentValue("VidaPlusClinica/" + BuildConfig.VERSION_CODE
                + " (Android; +https://github.com/miltonwagner/P2-projeto-clinica-medica-android)");
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        setContentView(R.layout.activity_endereco);
        NavegacaoInferior.configurar(this, R.id.nav_clinica);

        // Quando a clínica é uma aba do menu (admin), o "Voltar" do topo é dispensável.
        View btnVoltar = findViewById(R.id.btnVoltarEnd);
        View nav = findViewById(R.id.navInferior);
        com.google.android.material.bottomnavigation.BottomNavigationView menu =
                (com.google.android.material.bottomnavigation.BottomNavigationView) nav;
        boolean ehAba = nav.getVisibility() == View.VISIBLE
                && menu.getMenu().findItem(R.id.nav_clinica) != null;
        btnVoltar.setVisibility(ehAba ? View.GONE : View.VISIBLE);
        btnVoltar.setOnClickListener(v -> finish());

        txtDistancia = findViewById(R.id.txtDistancia);
        gps = LocationServices.getFusedLocationProviderClient(this);
        configurarMapa();

        Button btnMaps = findViewById(R.id.btnAbrirMaps);
        btnMaps.setOnClickListener(v -> abrirRota());

        if (temPermissao()) {
            iniciarGps();
        } else {
            pedirPermissao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarMapa() {
        mapa = findViewById(R.id.mapaClinica);
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(17.0);
        mapa.getController().setCenter(CLINICA);
        // Dentro do ScrollView: arrastar o dedo mexe o mapa, não a tela.
        mapa.setOnTouchListener((v, evento) -> {
            v.getParent().requestDisallowInterceptTouchEvent(evento.getAction() != MotionEvent.ACTION_UP);
            return false;
        });

        Marker clinica = new Marker(mapa);
        clinica.setPosition(CLINICA);
        clinica.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        clinica.setTitle("Clínica Médica Vida Plus");
        clinica.setSnippet(ENDERECO);
        mapa.getOverlays().add(clinica);
    }

    private boolean temPermissao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void aoResponderPermissao(Map<String, Boolean> respostas) {
        if (respostas.containsValue(Boolean.TRUE)) {
            iniciarGps();
        } else {
            txtDistancia.setText("Permita a localização para ver a distância até a clínica.");
        }
    }

    @SuppressLint("MissingPermission")
    private void iniciarGps() {
        if (!temPermissao()) {
            return;
        }
        gps.getLastLocation().addOnSuccessListener(local -> {
            if (local != null) {
                mostrarUsuario(local);
            }
        });
        LocationRequest pedido = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_GPS_MS)
                .setMinUpdateIntervalMillis(INTERVALO_GPS_MS)
                .build();
        gps.requestLocationUpdates(pedido, aoMover, Looper.getMainLooper());
    }

    private void mostrarUsuario(Location local) {
        GeoPoint ponto = new GeoPoint(local.getLatitude(), local.getLongitude());
        if (marcadorUsuario == null) {
            marcadorUsuario = new Marker(mapa);
            marcadorUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marcadorUsuario.setIcon(ContextCompat.getDrawable(this, R.drawable.ponto_usuario));
            marcadorUsuario.setTitle("Você está aqui");
            mapa.getOverlays().add(marcadorUsuario);
        }
        marcadorUsuario.setPosition(ponto);

        float[] metros = new float[1];
        Location.distanceBetween(ponto.getLatitude(), ponto.getLongitude(),
                CLINICA.getLatitude(), CLINICA.getLongitude(), metros);
        txtDistancia.setText(metros[0] < 1000
                ? String.format(Locale.ROOT, "Você está a %d m da clínica.", Math.round(metros[0]))
                : String.format(new Locale("pt", "BR"), "Você está a %.1f km da clínica.", metros[0] / 1000f));

        // Na primeira posição, enquadra você e a clínica (se estiver na região).
        if (!enquadrou && metros[0] < 50_000) {
            enquadrou = true;
            BoundingBox caixa = BoundingBox.fromGeoPointsSafe(Arrays.asList(ponto, CLINICA));
            mapa.post(() -> mapa.zoomToBoundingBox(caixa.increaseByScale(1.6f), true));
        }
        mapa.invalidate();
    }

    private void abrirRota() {
        Uri destino = Uri.parse("google.navigation:q=" + CLINICA.getLatitude() + "," + CLINICA.getLongitude());
        Intent rota = new Intent(Intent.ACTION_VIEW, destino).setPackage("com.google.android.apps.maps");
        if (rota.resolveActivity(getPackageManager()) == null) {
            rota = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(ENDERECO)));
        }
        startActivity(rota);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
        if (temPermissao()) {
            iniciarGps();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause();
        gps.removeLocationUpdates(aoMover);
    }
}
