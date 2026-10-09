"""Set real AVD geometry before boot, avoiding legacy wm resize letterboxing."""
from pathlib import Path

config = Path.home() / '.android/avd/test.avd/config.ini'
assert config.exists(), f'AVD config missing: {config}'
changes = {
    'hw.lcd.width': '720',
    'hw.lcd.height': '1280',
    'hw.lcd.density': '360',
    'skin.name': '720x1280',
    'skin.path': '_no_skin',
    'skin.dynamic': 'yes',
}
lines = []
for line in config.read_text().splitlines():
    key = line.partition('=')[0].strip()
    if key not in changes:
        lines.append(line)
lines.extend(f'{key}={value}' for key, value in changes.items())
config.write_text('\n'.join(lines) + '\n')
print('AVD physical geometry configured: 720x1280 at 360 dpi (320x569 dp)')
