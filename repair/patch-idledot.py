"""Apply IdleDot compact fix (1.3.7 change: 8.0f → 5.0f).

1.3.6 already ships the compact UI spacing from 1.3.5 but still draws the idle
dot at 8.0f; 1.3.7 reduces it to 5.0f. The patch is idempotent so it can be
re-run on an already patched tree.
"""
from pathlib import Path

DOT = Path('repair/decoded/smali_classes2/com/originisle/android/ui/samples/IdleDot.smali')
BEFORE = '    const/high16 v4, 0x41000000    # 8.0f'
AFTER = '    const/high16 v4, 0x40a00000    # 5.0f'

code = DOT.read_text()
if AFTER in code:
    print('IdleDot already patched (5.0f)')
else:
    assert code.count(BEFORE) == 1, 'IdleDot greenIcon pattern not found'
    DOT.write_text(code.replace(BEFORE, AFTER))
    print('IdleDot patched: 8.0f -> 5.0f')
