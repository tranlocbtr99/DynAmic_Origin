from pathlib import Path
import re

root = Path('repair/decoded/smali_classes2/com/originisle/android/ui')
def replace_method(code, method, body):
    result, count = re.subn(r'^\.method ' + re.escape(method) + r'\n.*?^\.end method',
                            '.method ' + method + '\n' + body.strip('\n') + '\n.end method',
                            code, flags=re.M | re.S)
    assert count == 1, method
    return result

setup = root / 'LtmSetup.smali'
code = setup.read_text()
code = replace_method(code, 'public static showWelcome(Landroid/app/Activity;)V', '''
    .locals 1
    new-instance v0, Lcom/originisle/android/ui/LtmSetup;
    invoke-direct {v0, p0}, Lcom/originisle/android/ui/LtmSetup;-><init>(Landroid/content/Context;)V
    invoke-static {p0, v0}, Lcom/originisle/android/ui/LtmPanels;->welcome(Landroid/app/Activity;Landroid/content/DialogInterface$OnClickListener;)V
    return-void
''')
code = replace_method(code, 'public invoke()Ljava/lang/Object;', '''
    .locals 1
    iget-object v0, p0, Lcom/originisle/android/ui/LtmSetup;->context:Landroid/content/Context;
    invoke-static {v0, p0}, Lcom/originisle/android/ui/LtmPanels;->about(Landroid/content/Context;Landroid/content/DialogInterface$OnClickListener;)V
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object v0
''')
setup.write_text(code)
updates = root / 'LtmUpdates.smali'
updates.write_text(replace_method(updates.read_text(), 'public invoke()Ljava/lang/Object;', '''
    .locals 1
    iget-object v0, p0, Lcom/originisle/android/ui/LtmUpdates;->context:Landroid/content/Context;
    invoke-static {v0}, Lcom/originisle/android/ui/LtmUpgrade;->open(Landroid/content/Context;)V
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object v0
'''))
