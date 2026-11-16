# Default unit-system preference

Settings → Default unit system offers Metric (the existing default) and Imperial.
It is a starting preference, not a conversion command or a migration of stored data.

## Intended scope

- Fresh BMI: metric centimeters/kilograms or imperial feet/inches/pounds.
- Fresh Unit Converter: existing millimeters → centimeters or inches → feet.
- Fresh Screen Ruler: centimeters or inches. Device-density/calibration warnings remain.
- Existing saved modes, remembered results, imported snapshots, and restored navigation/task drafts take priority.
- Changing the setting does not change an open form, its inputs, or its selected units. Converter category changes keep their existing behavior.
- Fuel Cost remains unit-neutral; this preference does not add new unit conversions.
- Stored in the existing app-private `noBackupFilesDir` UI-preference file, independently of tool history. No existing data is deleted.

## Device checks

1. Fresh install: confirm Metric and unchanged initial forms.
2. Select Imperial, newly open each eligible tool, and verify the choices above.
3. Change preference with a populated form retained in navigation; return and rotate. Inputs and units must be unchanged.
4. Enable remembered modes/results, save a selection contrary to the global default, then freshly reopen. The saved selection must win.
5. Import a History/Workbench snapshot with contrary units. Its validated units and inputs must win.
6. In Unit Converter, clear and swap units; neither operation should reapply the default. Confirm category switches still behave as before.
7. Verify ruler estimate/calibration warnings and readings on physical hardware.

Automated coverage was added for parsing/defaults, persistence, selector saving, all three fresh-form defaults, remembered mode/result precedence, and active/restored draft protection. JVM/instrumentation execution must be reported separately; instrumentation compilation is not a device-test pass.
