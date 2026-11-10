from pathlib import Path
import struct
import zipfile

BASE = Path('LTMori-2.0.3-Origin-signed.apk')
OUTPUT = Path('repair/ltmori-unsigned.apk')

def read32(data, offset):
    return struct.unpack_from('<I', data, offset)[0]

def manifest_patch(data):
    p = 8
    kind, header, size, count, styles, flags, start, style_start = struct.unpack_from('<HHIIIIII', data, p)
    assert kind == 1 and not (styles or flags or style_start)
    names = []
    for index in range(count):
        offset = p + start + read32(data, p + header + 4 * index)
        length = struct.unpack_from('<H', data, offset)[0]
        names.append(data[offset + 2:offset + 2 + 2 * length].decode('utf-16le'))
    names[names.index('2.0.3')] = '3.0.0'
    permission = 'android.permission.REQUEST_INSTALL_PACKAGES'
    provider = 'com.originisle.android.ui.LtmApkProvider'
    authority = 'com.autonavi.minimap.ltmori.updates'
    boot = 'android.permission.RECEIVE_BOOT_COMPLETED'
    job = 'com.originisle.android.ui.LtmUpdateJob'
    bind = 'android.permission.BIND_JOB_SERVICE'
    network = 'android.permission.ACCESS_NETWORK_STATE'
    add_network = network not in names
    add_boot = boot not in names
    add_job = job not in names
    add_bind = bind not in names
    names_to_add = [n for n in [boot, job, bind] if n not in names]
    names += names_to_add
    if add_network:
        names.append(network)
    idx = names.index
    no_index = 0xffffffff
    namespace = idx('http://schemas.android.com/apk/res/android')
    pool = bytearray()
    offsets = bytearray()
    for name in names:
        encoded = name.encode('utf-16le')
        offsets += struct.pack('<I', len(pool))
        pool += struct.pack('<H', len(encoded) // 2) + encoded + b'\0\0'
    pool += bytes(-len(pool) % 4)
    result = bytearray(data[:8])
    result += struct.pack('<HHIIIIII', 1, 28, 28 + len(offsets) + len(pool), len(names), 0, 0, 28 + len(offsets), 0)
    result += offsets + pool

    def element(tag, attrs):
        tag_id = idx(tag)
        block = bytearray(struct.pack('<HHIIIIIHHHHHH', 258, 16, 36 + 20 * len(attrs),
                                      1, no_index, no_index, tag_id, 20, 20, len(attrs), 0, 0, 0))
        for key, value in attrs:
            raw, kind, datum = (idx(value), 3, idx(value)) if isinstance(value, str) else (no_index, 18, value)
            block += struct.pack('<IIIHBBI', namespace, idx(key), raw, 8, 0, kind, datum)
        block += struct.pack('<HHIIIII', 259, 16, 24, 1, no_index, no_index, tag_id)
        return block

    p += size
    while p < len(data):
        kind, head, size = struct.unpack_from('<HHI', data, p)
        chunk = bytearray(data[p:p + size])
        p += size
        if kind == 384:
            ids = chunk[8:]
            ids += bytes(4 * (idx('grantUriPermissions') + 1) - len(ids))
            struct.pack_into('<I', ids, 4 * idx('grantUriPermissions'), 0x0101001b)
            chunk = struct.pack('<HHI', 384, 8, 8 + len(ids)) + ids
        if kind == 258:
            tag = names[read32(chunk, 20)]
            if tag == 'manifest':
                first, stride, length = struct.unpack_from('<HHH', chunk, 24)
                positions = [16 + first + i * stride for i in range(length)]
                version, = [pos for pos in positions if names[read32(chunk, pos + 4)] == 'versionCode']
                assert read32(chunk, version + 16) == 53
                struct.pack_into('<I', chunk, version + 16, 60)
            if tag == 'application':
                if permission not in names:
                    result += element('uses-permission', [('name', permission)])
                if add_boot:
                    result += element('uses-permission', [('name', boot)])
                if add_bind:
                    result += element('uses-permission', [('name', bind)])
                if add_network:
                    result += element('uses-permission', [('name', network)])
        if kind == 259 and names[read32(chunk, 20)] == 'application':
            if provider not in names:
                result += element('provider', [('name', provider), ('exported', 0), ('authorities', authority),
                                               ('grantUriPermissions', 0xffffffff)])
            if add_job:
                result += element('service', [('name', job), ('permission', bind), ('exported', 0)])
        result += chunk
    struct.pack_into('<I', result, 4, len(result))
    return result

with zipfile.ZipFile(BASE) as source, zipfile.ZipFile(OUTPUT, 'w') as output:
    manifest = manifest_patch(source.read('AndroidManifest.xml'))
    replacements = {'AndroidManifest.xml': manifest,
                    'classes2.dex': Path('repair/decoded/build/apk/classes2.dex').read_bytes(),
                    'classes3.dex': Path('repair/dex/classes.dex').read_bytes()}
    entries = [(item, replacements[item.filename] if item.filename in replacements else source.read(item))
               for item in source.infolist()]
    for item, content in entries:
        if item.filename.startswith('META-INF/') and item.filename.endswith(('.RSA', '.SF', '.DSA', '.EC')):
            continue
        info = zipfile.ZipInfo(item.filename, item.date_time)
        info.compress_type = item.compress_type
        info.external_attr = item.external_attr
        if info.compress_type == zipfile.ZIP_STORED:
            alignment = 16384 if info.filename.endswith('.so') else 4
            padding = -(output.fp.tell() + 30 + len(info.filename.encode())) % alignment
            while 0 < padding < 6:
                padding += alignment
            if padding:
                info.extra = struct.pack('<HHH', 0xd935, padding - 4, alignment) + bytes(padding - 6)
        output.writestr(info, content)
