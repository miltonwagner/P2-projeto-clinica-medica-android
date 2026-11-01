package com.example.clinicavidaplus;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Aplica ajustes comuns a todas as telas (bordas do sistema),
 * sem precisar repetir código em cada Activity.
 */
public class AppVidaPlus extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                Tela.ajustarBordas(activity);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle estado) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle estado) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
