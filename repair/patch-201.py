from pathlib import Path

root = Path('repair/decoded/smali_classes2/com/originisle/android')
name = 'HomeScreenKt$HomeScreen$4$1$5$1$1'
path = root / 'ui' / (name + '.smali')
code = path.read_text()
start = code.index('    const/4 v1, 0x2\n\n    .line 251')
end = code.index('    const v6, 0x4640d6eb', start)
code = code[:start] + '''    const/4 v4, 0x2

    const/4 v5, 0x1

    invoke-static {v12}, Lcom/originisle/android/ui/HomeScreenKt;->access$HomeScreen$lambda$7(Landroidx/compose/runtime/MutableState;)Z

    move-result v2

    invoke-static {v2}, Lcom/originisle/android/ui/LtmNotificationMode;->labels(Z)Ljava/util/List;

    move-result-object v1

''' + code[end:]
old = '''    check-cast v3, Lkotlin/jvm/functions/Function1;

    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    move v6, v9'''
new = f'''    check-cast v3, Lkotlin/jvm/functions/Function1;

    move-object/from16 v7, p0

    iget-object v7, v7, Lcom/originisle/android/ui/{name};->$context:Landroid/content/Context;

    invoke-static {{v7, v3}}, Lcom/originisle/android/ui/LtmNotificationMode;->confirm(Landroid/content/Context;Lkotlin/jvm/functions/Function1;)Lkotlin/jvm/functions/Function1;

    move-result-object v3

    invoke-interface {{v8}}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    move v6, v9'''
assert code.count(old) == 1
code = code.replace(old, new)
code = code.replace('const-string v1, "Every notification, plain messages included."', 'const-string v1, "Bao gồm tin nhắn và thông báo thường."')
start = code.index('const-string v1, "Only live cards')
end = code.index('"', code.index('"', start) + 1)
code = code[:start] + 'const-string v1, "Chỉ hiện hoạt động như cuộc gọi, điều hướng và tải xuống.' + code[end:]
path.write_text(code)

path = root / 'MainActivity.smali'
code = path.read_text()
key = '''    if-eqz v0, :cond_0

    .line 57'''
assert code.count(key) == 1
path.write_text(code.replace(key, '''    if-eqz v0, :cond_0

    invoke-static {p0}, Lcom/originisle/android/ui/LtmUpdateJob;->onForeground(Landroid/app/Activity;)V

    .line 57'''))
