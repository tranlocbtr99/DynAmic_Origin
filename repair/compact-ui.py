"""Apply compact spacing to the 1.3.2 Compose UI before rebuilding."""
from pathlib import Path
import re

UI = Path('repair/decoded/smali_classes2/com/originisle/android/ui')

def change(filename, method, replacements):
    path = UI / filename
    code = path.read_text()
    match, = re.finditer(r'^\.method [^\n]*' + re.escape(method) + r'[^\n]*\n.*?^\.end method', code, re.M | re.S)
    body = match[0]
    for before, after in replacements:
        assert body.count(before) == 1, (filename, method, before)
        body = body.replace(before, after)
    path.write_text(code[:match.start()] + body + code[match.end():])

change('ComponentsKt.smali', ' <clinit>()V', [
    ('const/16 v0, 0x10\n', 'const/16 v0, 0xe\n'),
    ('const/16 v2, 0x14\n', 'const/16 v2, 0xe\n'),
    ('const/16 v4, 0x18\n', 'const/16 v4, 0x14\n'),
    ('.line 697\n    invoke-static {v2}', '.line 697\n    const/16 v2, 0x14\n    int-to-float v2, v2\n    invoke-static {v2}'),
])
change('ComponentsKt.smali', ' ScreenHeader(', [
    ('const/16 v2, 0x1c\n\n    .line 62', 'const/16 v2, 0x18\n\n    .line 62'),
])
change('ComponentsKt.smali', ' SettingRow-oC9nPe0(', [
    ('const/16 v12, 0x3c\n\n    int-to-float v12, v12\n\n    .line 482', 'const/16 v12, 0x34\n\n    int-to-float v12, v12\n\n    .line 482'),
    ('const/16 v12, 0xa\n\n    int-to-float v12, v12\n\n    invoke-static {v12}, Landroidx/compose/ui/unit/Dp;', 'const/16 v12, 0x8\n\n    int-to-float v12, v12\n\n    invoke-static {v12}, Landroidx/compose/ui/unit/Dp;'),
])
change('HomeScreenKt.smali', ' HomeScreen(', [
    ('const/16 v4, 0x18\n\n    int-to-float v4, v4\n\n    .line 767', 'const/16 v4, 0xe\n\n    int-to-float v4, v4\n\n    .line 767'),
])
change('HomeScreenKt$HomeScreen$4$1$1.smali', ' invoke(Landroidx/compose/foundation/lazy/LazyItemScope;', [
    ('const/16 v1, 0x1c\n\n    .line 149', 'const/16 v1, 0x18\n\n    .line 149'),
    ('const/16 v11, 0x20\n\n    .line 152', 'const/16 v11, 0x1c\n\n    .line 152'),
])

change('samples/IdleDot.smali', ' greenIcon()', [
    ('const/high16 v4, 0x41000000    # 8.0f', 'const/high16 v4, 0x40a00000    # 5.0f'),
])
