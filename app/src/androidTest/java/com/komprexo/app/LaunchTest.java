package com.komprexo.app;

import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class LaunchTest {
    @Test public void launchAndRecreateShowsKomprexo() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertTitle(scenario);
            scenario.recreate();
            assertTitle(scenario);
        }
    }
    private void assertTitle(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            TextView title = activity.findViewById(R.id.title);
            assertEquals("Komprexo", title.getText().toString());
            assertEquals("com.komprexo.app", activity.getPackageName());
        });
    }
}
