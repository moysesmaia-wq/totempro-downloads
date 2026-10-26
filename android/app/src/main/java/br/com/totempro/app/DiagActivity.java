package br.com.totempro.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.androidbrowserhelper.trusted.LauncherActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * DIAGNÓSTICO TEMPORÁRIO: porta de entrada do app. Se a abertura anterior travou, mostra o erro na tela
 * (para tirar uma captura); senão, guarda qualquer erro futuro e abre o app normalmente.
 * Sai do projeto assim que o travamento for corrigido.
 */
public class DiagActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sp = getSharedPreferences("diag", MODE_PRIVATE);
        String last = sp.getString("crash", null);
        if (last != null) {
            sp.edit().remove("crash").apply();
            show(last);
            return;
        }

        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    sp.edit().putString("crash", header() + trace(e)).commit();
                } catch (Throwable ignored) {
                }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });

        try {
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
        } catch (Throwable e) {
            show(header() + trace(e));
        }
    }

    private static String header() {
        return "TotemPro " + "- Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") - "
                + Build.MANUFACTURER + " " + Build.MODEL + "\n\n";
    }

    private static String trace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() > 3500 ? s.substring(0, 3500) : s;
    }

    private void show(String text) {
        TextView tv = new TextView(this);
        tv.setText("O app travou. Tire uma captura desta tela e envie.\n\n" + text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setPadding(24, 48, 24, 24);
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);
    }
}
