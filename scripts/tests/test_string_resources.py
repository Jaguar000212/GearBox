"""Static presentation-resource guardrails; Android compilation/device tests are separate."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
NEW = ('strings_app_screens.xml', 'strings_calculators.xml', 'strings_catalog.xml',
       'strings_games_clock.xml', 'strings_special_tools.xml', 'strings_shared_components.xml',
       'strings_system_surfaces.xml', 'strings_search.xml', 'strings_operation_navigation.xml', 'strings_working.xml', 'strings_text_groups.xml', 'strings_unit_defaults.xml', 'strings_workbench_organization.xml', 'strings_incoming_share.xml', 'strings_safe_handoffs.xml', 'strings_color_inspection.xml', 'strings_image_sampler.xml', 'strings_unit_batch.xml', 'strings_text_editor.xml')

class StringResourceTest(unittest.TestCase):
    def resources(self):
        found = set()
        for path in (ROOT / 'app/src/main/res/values').glob('*.xml'):
            for element in ET.parse(path).getroot():
                if element.tag in ('string', 'plurals'):
                    key = (element.tag, element.attrib['name'])
                    self.assertNotIn(key, found, str(path))
                    found.add(key)
        return found

    def test_unique_resource_names_and_existing_references(self):
        resources = self.resources()
        for path in (ROOT / 'app/src').rglob('*.kt'):
            for kind, name in re.findall(r'(?<!android\.)\bR\.(string|plurals)\.(\w+)', path.read_text()):
                self.assertIn((kind, name), resources, f'{path}: {name}')

    def test_new_plural_and_format_contracts(self):
        for filename in NEW:
            for element in ET.parse(ROOT / 'app/src/main/res/values' / filename).getroot():
                items = list(element) if element.tag == 'plurals' else [element]
                if element.tag == 'plurals':
                    self.assertIn('other', [item.get('quantity') for item in items], element.get('name'))
                for item in items:
                    text = ''.join(item.itertext())
                    if '%' not in text or element.get('formatted') == 'false':
                        continue
                    tokens = re.findall(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]', text)
                    placeholders = [t for t in tokens if t != '%%']
                    if len(placeholders) > 1:
                        self.assertTrue(all('$' in t for t in placeholders), (filename, element.get('name'), tokens))

    def test_direct_compose_labels_are_resource_backed(self):
        for path in (ROOT / 'app/src/main/java/com/jaguar/gearbox/ui').rglob('*.kt'):
            source = '\n'.join(line for line in path.read_text().splitlines()
                               if not line.lstrip().startswith(('//', '*')))
            # Symbols, technical encodings, user values and templates are not static prose.
            self.assertIsNone(re.search(r'\bText\(\s*"[A-Za-z][^"$]*"', source), str(path))
            self.assertIsNone(re.search(r'contentDescription\s*=\s*"[A-Za-z][^"$]*"', source), str(path))

if __name__ == '__main__':
    unittest.main()
