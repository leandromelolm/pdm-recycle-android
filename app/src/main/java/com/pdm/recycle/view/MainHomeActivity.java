package com.pdm.recycle.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
//import android.widget.SearchView;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
//import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.pdm.recycle.R;
import com.pdm.recycle.control.ConfiguracaoFirebase;
//import com.pdm.recycle.databinding.ActivityMainBinding;
import com.pdm.recycle.helper.Base64Custom;
import com.pdm.recycle.model.Coleta;
import com.pdm.recycle.model.Descarte;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainHomeActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnInfoWindowClickListener,
        GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private static final int FINE_LOCATION_REQUEST = 1;
    private boolean fine_location;
    private Double latitude;
    private Double longitude;
    private String tipoResiduo, status, dataDescarte, dataColeta;
    //private ArrayList<Descarte> listDescarte;
    private DataSnapshot locaisDescarte;
    private String idDescarte;
    private String userEmail;
    private String emailUserAutenticado;
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    //private AppBarConfiguration appBarConfiguration;
    //private FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabaseReference();
    //private MaterialSearchView searchView;
    private MenuItem listsearch;
    private ListChipFragment listfragment;

    private ChipGroup chipgroup;
    private String textoChip;
    private Boolean activeMenuFilter = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main_home);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setTitle("Recycle");

            inicializarComponentes();

            requestPermission();

            autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
            FirebaseUser usuarioAtual =  autenticacao.getCurrentUser();
            emailUserAutenticado = usuarioAtual.getEmail();

        }catch (Exception exception){
           Intent intent = new Intent(MainHomeActivity.this, MainHomeActivity.class);
            startActivity( intent );
        }
        //bloqueia na orientação retrato (PORTRAIT) a activity_main_home
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void requestPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        this.fine_location = (permissionCheck == PackageManager.PERMISSION_GRANTED);
        if (this.fine_location) return;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                FINE_LOCATION_REQUEST);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = (grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        this.fine_location = (requestCode == FINE_LOCATION_REQUEST) && granted;

        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(this.fine_location);
        }
    }

    private void pesquisarLocaisDescarte(String texto){
        //Log.d("pesquisa",texto);
        DatabaseReference coletas = referencia.child("coletas");

        mMap.clear();

        for (DataSnapshot objSnapshot:locaisDescarte.getChildren()){
            Descarte descarte = objSnapshot.getValue(Descarte.class);
            String descarteID = objSnapshot.getKey();
            tipoResiduo = descarte.getTipoResiduo().toLowerCase();
            latitude = descarte.getLatitude();
            longitude = descarte.getLongitude();
            status = descarte.getStatus().toLowerCase();
            dataDescarte = descarte.getDataDescarte();
            idDescarte = descarteID;
            userEmail =  descarte.getUserEmail();
            LatLng localDescarte = new LatLng(latitude, longitude);

            Coleta coleta = objSnapshot.getValue(Coleta.class);
            dataColeta = coleta.getDataColeta();

            //Log.i("local_descarte", localDescarte.toString());

            if (tipoResiduo.contains(texto.toLowerCase()) && status.startsWith("não coletado")) {
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(localDescarte)
                                .title("Tipo de resíduo: " + tipoResiduo.substring(1, tipoResiduo.length()-1))
                                .snippet(dataDescarte +" Toque aqui para mais detalhes")
                                //.icon( BitmapDescriptorFactory.fromResource(R.drawable.pin_icon))
                                .icon(vectorToBitmap(R.drawable.pin_icon_recycle))
                );
                marker.setTag("\nData descarte: " + dataDescarte +
                        "\n\nDescartado por: " + userEmail +
                        "\n\nCoordenada: " + localDescarte);

            //} if(status.contains(texto.toLowerCase()) && status.equals("coletado")){
            } if(status.contains(texto.toLowerCase()) && status.equals("coletado")){
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(localDescarte)
                                .title("Tipo de resíduo: " + tipoResiduo.substring(1, tipoResiduo.length()-1))
                                .snippet("data descarte: " + dataDescarte)
                                //.icon( BitmapDescriptorFactory.fromResource(R.drawable.pin_icon))
                                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .icon(vectorToBitmap(R.drawable.pin_icon_recycle_grey))
                );
                marker.setTag("\nData descarte: " + dataDescarte +
                        "\n\nDescartado por: " + userEmail +
                        "\n\nCoordenada: " + localDescarte);

            }  if(status.contains(texto.toLowerCase()) && status.equals("não encontrado")) {
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(localDescarte)
                                .title("Tipo de resíduo: " + tipoResiduo.substring(1, tipoResiduo.length()-1))
                                .snippet(dataDescarte +" Toque aqui para mais detalhes")
                                //.icon( BitmapDescriptorFactory.fromResource(R.drawable.pin_icon))
                                .icon(vectorToBitmap(R.drawable.pin_icon_recycle_red))
                );
                marker.setTag("\nData descarte: " + dataDescarte +
                        "\n\nDescartado por: " + userEmail +
                        "\n\nCoordenada: " + localDescarte);
            }

        }
    }

    private void recuperarTodosLocaisDescarte() {

        DatabaseReference descartes = referencia.child("descartes");

        descartes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                /* Salvando os dados do firebase na varíável locaisDescarte */
                locaisDescarte = snapshot;
                //Log.i("FIREBASE", snapshot.getValue().toString());

                /* Limpar os marcadores no Mapa*/
                mMap.clear();

                for (DataSnapshot objSnapshot:snapshot.getChildren()) {
                    Descarte descarte = objSnapshot.getValue(Descarte.class);
                    String descarteID = objSnapshot.getKey();
                    tipoResiduo = descarte.getTipoResiduo();
                    latitude = descarte.getLatitude();
                    longitude = descarte.getLongitude();
                    status = descarte.getStatus().toLowerCase();
                    dataDescarte = descarte.getDataDescarte();
                    idDescarte = descarteID;
                    userEmail =  descarte.getUserEmail();
                    LatLng localDescarte = new LatLng(latitude, longitude);

                    //Log.i("local_descarte", localDescarte.toString());

                    if (status.startsWith("não coletado")) {
                        Marker marker = mMap.addMarker(
                                new MarkerOptions()
                                        .position(localDescarte)
                                        .title("Tipo de resíduo: " + tipoResiduo.substring(1, tipoResiduo.length()-1))
                                        .snippet(dataDescarte +" Toque aqui para mais detalhes")
                                        //.icon( BitmapDescriptorFactory.fromResource(R.drawable.pin_icon))
                                        .icon(vectorToBitmap(R.drawable.pin_icon_recycle))
                        );
                        marker.setTag("\nData descarte: " + dataDescarte +
                                "\n\nDescartado por: " + userEmail +
                                "\n\nCoordenada: " + localDescarte);
                        marker.hideInfoWindow();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("Erro ", error.getMessage());
            }
        });
    }

    /* Método atualiza o status do descarte no firebase para "Coletado */
    private void informarColetaResiduo(LatLng position){

        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabaseReference();
        DatabaseReference descartes =  firebaseRef.child("descartes");

        Log.i("FIREBASE", locaisDescarte.getValue().toString());

        String identificadorDescarte = Base64Custom.codificarBase64(String.valueOf(position));

        Log.i("coletarResiduoDescartado: ",identificadorDescarte );

        for (DataSnapshot objSnapshot:locaisDescarte.getChildren()){
            Descarte descarte = objSnapshot.getValue(Descarte.class);
            String descarteID = objSnapshot.getKey();
            status="Coletado";
            Log.i(" status coletado: ", descarteID);

            /* atualizando apenas o status no firebase */
            descartes.child(identificadorDescarte).child("status").setValue(status);
            Toast.makeText(this, "Coleta Informada!",Toast.LENGTH_LONG).show();

            if(identificadorDescarte.matches(descarteID)){
                Coleta coleta = new Coleta();
                coleta.setTipoResiduo(descarte.getTipoResiduo());
                coleta.setDataDescarte(descarte.getDataDescarte());
                coleta.setLatitude(descarte.getLatitude());
                coleta.setLongitude(descarte.getLongitude());
                coleta.setUserEmail(emailUserAutenticado);

                Date data = new Date(System.currentTimeMillis());
                SimpleDateFormat formatarDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                formatarDate.format(data);

                coleta.setDataColeta(formatarDate.format(data));

                coleta.salvarColeta();
            }
        }
    }

    /* Método atualiza o status do descarte no firebase para "Não Encontrado */
    private void informarNaoEncontrado(LatLng position){

        DatabaseReference descartes =  firebaseRef.child("descartes");
        String identificadorDescarte = Base64Custom.codificarBase64(String.valueOf(position));

        for (DataSnapshot objSnapshot:locaisDescarte.getChildren()){
            String descarteID = objSnapshot.getKey();
            status="Não Encontrado";
            Log.i(" ID status no found: ", descarteID);

            /* atualiza apenas o status no firebase */
            descartes.child(identificadorDescarte).child("status").setValue(status);
            Toast.makeText(this, "Reportado!",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng ifRecife = new LatLng(-8.058320, -34.950611);

        // mMap.addMarker(new MarkerOptions().position(ifRecife).title("Local"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ifRecife, 12));

        /* caso não tenha nada salvo no banco de dados, o programa fecha inesperadamente*/
        recuperarTodosLocaisDescarte();

        mMap.setOnInfoWindowClickListener(this);

        mMap.setPadding(0,250,0,230);

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("O que deseja informar?");
            builder.setMessage(marker.getTitle() +
                    "\n" + marker.getTag());
            // add the buttons
            builder.setPositiveButton("Foi coletado", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    informarColetaResiduo(marker.getPosition());
                }
            });
            builder.setNegativeButton("Não encontrado", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    informarNaoEncontrado(marker.getPosition());
                }
            });
            builder.setNeutralButton("Cancelar", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater =  getMenuInflater();
        inflater.inflate(R.menu.menu_mainhome, menu);

        /** Configurações do botão de pesquisa */
        MenuItem.OnActionExpandListener onActionExpandListener = new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                /**  exibindo opções de chip ao usuário com listfragment */
                //listfragment = new ListChipFragment();
                //getSupportFragmentManager().beginTransaction().replace(R.id.map,new ListChipFragment()).commit();

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                //fecharOpcoesPesquisa();
                //Toast.makeText(MainHomeActivity.this, "teste fechando menu pesquisa", Toast.LENGTH_SHORT).show();
                recuperarTodosLocaisDescarte();
                return true;
            }
        };
        menu.findItem(R.id.menu_search).setOnActionExpandListener(onActionExpandListener);

        MenuItem pesquisa = menu.findItem(R.id.menu_search);
        SearchView editPesquisa = (SearchView) pesquisa.getActionView();
        editPesquisa.setQueryHint("Pesquise aqui, ex: plástico");

        editPesquisa.setOnQueryTextListener(new SearchView.OnQueryTextListener() {


            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Log.d("evento", "onQueryTextChange: ");
                if(newText !=null && !newText.isEmpty()){
                    pesquisarLocaisDescarte( newText );
                }
                if(newText.length() == 0){
                    recuperarTodosLocaisDescarte();
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ((item.getItemId())){
            case R.id.menuSair:
                autenticacao.signOut();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.menu_filter:
                openChipGroupList();
                return true;
            case R.id.menu_notification:
                openNotification();
                return true;
            case R.id.menu_setting:
                openSetting();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openNotification() {
        Toast.makeText(this, "Não implementado", Toast.LENGTH_SHORT).show();
    }

    private void openSetting() {
        Toast.makeText(this, "Não implementado", Toast.LENGTH_SHORT).show();
    }

    public void redirectDescarte(View v) {
        Intent intent = new Intent(this, DescarteSelectActivity.class);
        startActivity(intent);
    }

    private void inicializarComponentes(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapClick( LatLng latLng) {

    }

    private BitmapDescriptor vectorToBitmap(@DrawableRes int id) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        //DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /** Método exibe chips dentro da Pesquisa do menu principal*/
    public void fecharOpcoesPesquisa(){
        /**  limpando exibição dos chips que são exibidos ao selecionar o botão de pesquisa */
        closeChipGroup();
    }

    public void openChipGroupList(){

        if (!activeMenuFilter){
            listfragment = new ListChipFragment();

            /* exibir o groupChip na MainHomeActivity*/
//            getSupportFragmentManager().beginTransaction().replace(R.id.map,new ListChipFragment()).commit();

//            getSupportFragmentManager().beginTransaction().remove(listfragment).commit();
//            getSupportFragmentManager().beginTransaction().add(R.id.map,listfragment).commit();

            getSupportFragmentManager().beginTransaction().remove(listfragment).commit();
            getSupportFragmentManager().beginTransaction().replace(R.id.map,listfragment).commit();

            activeMenuFilter = true;

        }else if(activeMenuFilter) {
            activeMenuFilter = false;
            closeChipGroup();
            recuperarTodosLocaisDescarte();
            getSupportFragmentManager().beginTransaction().remove(listfragment).commit();
        }
    }

    public void closeChipGroup(){
        chipgroup = findViewById(R.id.chipGroup);
        chipgroup.removeAllViews();
    }

    public <chip> void checkChip(String chip){
        textoChip = chip;
        Toast.makeText(this, "Selecionado: " + textoChip, Toast.LENGTH_SHORT).show();

        switch (chip){
            case "resíduo coletado":
                pesquisarLocaisDescarte( "coletado");
                break;
            case "resíduo não encontrado":
                pesquisarLocaisDescarte( "não encontrado");
                break;
            case "outro tipo de residuo":
                pesquisarLocaisDescarte("outro");
                break;
            case "todos":
                recuperarTodosLocaisDescarte();
                break;
            default:
                pesquisarLocaisDescarte( textoChip);
        }
/*        
        if(chip.equals("resíduo coletado")){
            pesquisarLocaisDescarte( "coletado");
        }else if(chip.equals("resíduo não encontrado")){
                    pesquisarLocaisDescarte( "não encontrado");
              }else if(chip.equals("outro tipo de residuo")){
                        pesquisarLocaisDescarte( "outros");
                    }else
                            pesquisarLocaisDescarte( textoChip);
*/
    }
}