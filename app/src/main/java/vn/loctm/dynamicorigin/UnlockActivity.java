package vn.loctm.dynamicorigin;

import android.app.*;
import android.os.Bundle;
import android.widget.TextView;

/** Only opened by an explicit island tap. Secure keyguard remains authoritative. */
public class UnlockActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setShowWhenLocked(true);
    TextView prompt = IOSStyle.text(this, "Mở khóa để xem thông báo", 17, true, IOSStyle.ink(this));
    int p = IOSStyle.dp(this, 24);
    prompt.setPadding(p, p, p, p);
    setContentView(prompt);
    IOSStyle.dialog(this);
    KeyguardManager manager = getSystemService(KeyguardManager.class);
    if (!manager.isKeyguardLocked()) {
      done();
      return;
    }
    manager.requestDismissKeyguard(
        this,
        new KeyguardManager.KeyguardDismissCallback() {
          @Override
          public void onDismissSucceeded() {
            done();
          }

          @Override
          public void onDismissCancelled() {
            finish();
          }

          @Override
          public void onDismissError() {
            finish();
          }
        });
  }

  private void done() {
    IslandService service = IslandService.getInstance();
    if (service != null) {
      service.replyVisibility(false);
      service.showInbox();
    }
    finish();
  }
}
