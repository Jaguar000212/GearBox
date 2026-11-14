# Home discovery refresh

This is an independent Home change; it does not depend on the Unit Converter redesign.

- Search and category filters lead the screen; recent tools remain accessible beneath them.
- Active search or category selection hides unrelated recents.
- Home opts into compact, flat tool cards with readable names and always-visible descriptions.
- Favourite targets remain 48 dp with named accessibility actions; card heights still grow with text.
- Category counts reflect filtered results. An empty result offers a single action to clear both filters.
- The existing adaptive column policy and Material color scheme remain in use. Favorites retains its existing card style.

## Review on a device or emulator

1. In light and dark themes, search by name and keyword, clear the query, combine it with a category, and use Clear filters after a no-result search.
2. Open a tool and go back: its recent chip should appear with no filters active. Open it from that chip.
3. Toggle a star without entering the tool, then confirm Favorites reflects it. Open the card separately.
4. At 200% font scaling and a narrow window, confirm descriptions wrap and no controls overlap. Check landscape and tablet widths.
5. With TalkBack, verify category headings, tool-opening actions and the separate named favorite action.
6. Verify search with the keyboard showing and after rotation; query/category state should survive.

Compose regression tests cover empty-search recovery/tool opening and both standard/compact cards at large text. The CI workflow compiles instrumentation tests but does not execute them. Actual rendering, TalkBack and interaction checks remain required before design sign-off.
