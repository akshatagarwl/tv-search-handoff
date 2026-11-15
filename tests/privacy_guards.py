"""Static permission/capability regression guards. Not Android runtime tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
a = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'AndroidManifest.xml').getroot()
assert not manifest.findall('uses-permission'), 'No application permissions should be requested'
service = manifest.find('application/service')
assert service.get(a + 'permission') == 'android.permission.BIND_ACCESSIBILITY_SERVICE'
config = ET.parse(root / 'res/xml/search_service.xml').getroot()
assert config.get(a + 'canRequestFilterKeyEvents') == 'false'
assert 'flagRequestFilterKeyEvents' not in config.get(a + 'accessibilityFlags', '')
assert set(config.get(a + 'packageNames').split(',')) == {
    'com.google.android.katniss', 'io.gh.reisxd.tizentube.cobalt'}
source = (root / 'src/io/akshat/tvsearch/SearchHandoffService.java').read_text()
assert 'onKeyEvent' not in source
assert '.setPackage(TARGET)' in source
assert 'appendQueryParameter("search_query", query)' in source
assert 'pollsRemaining = 40' in source
assert 'query=" +' not in source
print('Privacy/capability guards passed')
