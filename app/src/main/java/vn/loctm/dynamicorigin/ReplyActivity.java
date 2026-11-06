package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;

/** Collects a user-authored reply; never sends automatically or persists drafts. */
public class ReplyActivity extends Activity {
  private String key;
  private int index;
  private Notification.Action original;
  private RemoteInput input;
  private EditText editor;
  private boolean sending;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    key = getIntent().getStringExtra("key");
    index = getIntent().getIntExtra("index", -1);
    Entry e = Inbox.find(this, key);
    if (locked()
        || e == null
        || !Prefs.enabled(this)
        || !Prefs.allowed(this, e.pkg)
        || e.actions == null
        || index < 0
        || index >= e.actions.length) {
      finish();
      return;
    }
    original = e.actions[index];
    if (original.actionIntent == null
        || original.actionIntent.isImmutable()
        || original.getRemoteInputs() == null) {
      finish();
      return;
    }
    for (RemoteInput candidate : original.getRemoteInputs())
      if (candidate.getAllowFreeFormInput()) {
        input = candidate;
        break;
      }
    if (input == null) {
      Toast.makeText(this, "Ứng dụng không hỗ trợ trả lời văn bản", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    setTitle("Trả lời · " + e.app);
    IOSStyle.dialog(this);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);
    TextView context = new TextView(this);
    context.setText(e.title);
    context.setMaxLines(2);
    IOSStyle.text(context, 18, true, IOSStyle.ink(this));
    layout.addView(context);
    editor = new EditText(this);
    editor.setHint(input.getLabel());
    editor.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    editor.setMaxLines(5);
    editor.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2000)});
    IOSStyle.text(editor, 17, false, IOSStyle.ink(this));
    layout.addView(editor);
    LinearLayout buttons = new LinearLayout(this);
    Button cancel = IOSStyle.button(this, "Hủy", false, false);
    cancel.setOnClickListener(v -> finish());
    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, IOSStyle.dp(this, 46), 1);
    cp.setMarginEnd(IOSStyle.dp(this, 10));
    buttons.addView(cancel, cp);
    Button send = IOSStyle.button(this, "Gửi", true, false);
    send.setOnClickListener(v -> sendReply());
    buttons.addView(send, new LinearLayout.LayoutParams(0, IOSStyle.dp(this, 46), 1));
    layout.addView(buttons);
    setContentView(layout);
    editor.requestFocus();
    getWindow()
        .setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  }

  private boolean locked() {
    return getSystemService(KeyguardManager.class).isKeyguardLocked();
  }

  private void sendReply() {
    if (sending || editor.getText().toString().isBlank()) return;
    Entry current = Inbox.find(this, key);
    if (locked()
        || !Prefs.enabled(this)
        || current == null
        || !Prefs.allowed(this, current.pkg)
        || current.actions == null
        || index >= current.actions.length
        || !original.actionIntent.equals(current.actions[index].actionIntent)) {
      Toast.makeText(this, "Thông báo đã thay đổi. Mở lại để trả lời.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    boolean inputStillValid = false;
    RemoteInput[] inputs = current.actions[index].getRemoteInputs();
    if (inputs != null)
      for (RemoteInput candidate : inputs)
        if (candidate.getAllowFreeFormInput()
            && candidate.getResultKey().equals(input.getResultKey())) inputStillValid = true;
    if (!inputStillValid) {
      Toast.makeText(this, "Nút trả lời đã thay đổi. Mở lại thông báo.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    sending = true;
    Intent result = new Intent();
    Bundle values = new Bundle();
    values.putCharSequence(input.getResultKey(), editor.getText().toString());
    RemoteInput.addResultsToIntent(new RemoteInput[] {input}, result, values);
    RemoteInput.setResultsSource(result, RemoteInput.SOURCE_FREE_FORM_INPUT);
    try {
      original.actionIntent.send(this, 0, result);
      Toast.makeText(this, "Đã chuyển trả lời tới ứng dụng", Toast.LENGTH_SHORT).show();
      finish();
    } catch (PendingIntent.CanceledException | RuntimeException ex) {
      sending = false;
      Toast.makeText(this, "Không gửi được. Hãy mở ứng dụng để trả lời.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (locked()) {
      finish();
      return;
    }
    IslandService service = IslandService.getInstance();
    if (service != null) service.replyVisibility(true);
  }

  @Override
  public void onStop() {
    super.onStop();
    if (!isChangingConfigurations()) finish();
  }

  @Override
  public void onDestroy() {
    IslandService service = IslandService.getInstance();
    if (service != null) service.replyVisibility(false);
    super.onDestroy();
  }
}
