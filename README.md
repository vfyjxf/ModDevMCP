# ModDevMCP

MCP for development of mods for NeoForge.

Current project layout:

- `Server`: MCP server bootstrap and extensible tool registration APIs
- `Mod`: Minecraft-facing APIs, runtime drivers, built-in tool providers, and NeoForge entrypoints

Current implemented vertical slice:

- server-side MCP tool registration via `McpToolRegistry`
- mod-side runtime registries and registration API
- built-in UI, input, inventory, and event tool providers
- first structured UI path for `moddev.ui_snapshot` and `moddev.ui_get_interaction_state`
