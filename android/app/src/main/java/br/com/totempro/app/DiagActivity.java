package br.com.totempro.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.browser.customtabs.CustomTabsIntent;

import com.google.androidbrowserhelper.trusted.LauncherActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * DIAGNÓSTICO TEMPORÁRIO: tela fixa com informações do aparelho e botões para abrir o app passo a passo.
 * Mostra qualquer erro que aconteça. Sai do projeto assim que o problema de abertura for corrigido.
 */
public class DiagActivity extends Activity {
    private TextView log;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("diag", MODE_PRIVATE);

        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    sp.edit().putString("crash", trace(e)).commit();
                } catch (Throwable ignored) {
                }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 64, 24, 24);

        TextView title = new TextView(this);
        title.setText("TotemPro - diagnostico");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        root.addView(title);

        log = new TextView(this);
        log.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        log.setTextIsSelectable(true);
        root.addView(log);

        Button b1 = new Button(this);
        b1.setText("1) Modo app (como esta hoje, tela cheia)");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrir(LauncherActivity.class, "botao 1");
            }
        });
        root.addView(b1);

        Button b3 = new Button(this);
        b3.setText("3) Modo app SEM tela cheia");
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrir(TwaPlain.class, "botao 3");
            }
        });
        root.addView(b3);

        Button b4 = new Button(this);
        b4.setText("4) Aba do Chrome (Custom Tab)");
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add("Tocou no botao 4.");
                try {
                    new CustomTabsIntent.Builder().build().launchUrl(DiagActivity.this, Uri.parse("https://totempro.com.br/app"));
                    add("Custom Tab chamada sem erro.");
                } catch (Throwable e) {
                    add("ERRO na Custom Tab:
" + trace(e));
                }
            }
        });
        root.addView(b4);

        Button b2 = new Button(this);
        b2.setText("2) Abrir o site no Chrome");
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add("Tocou no botao 2.");
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://totempro.com.br/app"));
                    i.setPackage("com.android.chrome");
                    startActivity(i);
                    add("Chrome chamado sem erro.");
                } catch (Throwable e) {
                    add("ERRO ao abrir o Chrome:\n" + trace(e));
                }
            }
        });
        root.addView(b2);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        add("Aparelho: " + Build.MANUFACTURER + " " + Build.MODEL + " - Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        add("Chrome: " + chrome());
        add("Navegador padrao: " + defaultBrowser());
        String last = sp.getString("crash", null);
        if (last != null) {
            sp.edit().remove("crash").apply();
            add("ERRO DA ABERTURA ANTERIOR:\n" + last);
        } else {
            add("Nenhum erro guardado da abertura anterior.");
        }
    }

    private void abrir(Class<?> alvo, String nome) {
        add("Tocou no " + nome + ". Pedindo para abrir...");
        try {
            Intent i = new Intent(this, alvo);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            add("Pedido enviado sem erro. Se nada aparecer, o app fechou sozinho.");
        } catch (Throwable e) {
            add("ERRO ao abrir:
" + trace(e));
        }
    }

    private void add(String s) {
        log.append("\n" + s + "\n");
    }

    private String chrome() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo("com.android.chrome", 0);
            return "instalado, versao " + p.versionName;
        } catch (Throwable e) {
            return "NAO encontrado (" + e.getClass().getSimpleName() + ")";
        }
    }

    private String defaultBrowser() {
        try {
            ResolveInfo r = getPackageManager().resolveActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://totempro.com.br")), 0);
            return r == null ? "nenhum" : r.activityInfo.packageName;
        } catch (Throwable e) {
            return "erro: " + e.getClass().getSimpleName();
        }
    }

    private static String trace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() > 3000 ? s.substring(0, 3000) : s;
    }
}
