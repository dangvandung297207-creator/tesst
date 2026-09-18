# Animation data

This folder is where **Blender exported animation files** live. Nothing at runtime requires them: the
mod ships procedural animation (procedural auras, scripted sequences, vanilla humanoid animation for
the boss) and only reads a file here when a definition points at one.

## Pipeline

1. Author the mesh and clip in Blender (the Blender MCP bridge drives the export).
2. Export the mesh to `assets/animeki/models/` (glTF/`.json` mesh part list) and the clip here as
   `<id>.animation.json`.
3. Reference the id from a data definition:
   * transformation aura: `AuraProfile.builder().mesh("<mesh id>", "<animation id>")`
   * impact effect: `"meshes": [{"mesh": "...", "animation": "..."}]` in `assets/animeki/vfx/*.json`
   * attach point: `"bone": "right_arm"` (skeleton part the effect is parented to)

## Suggested clip schema

```json
{
  "id": "animeki:aura_idle",
  "length": 1.0,
  "loop": true,
  "bones": {
    "root": { "rotation": [[0.0, 0.0, 0.0], [0.0, 6.0, 0.0]], "position": [[0,0,0],[0,0.05,0]] }
  }
}
```

`tools/generate_textures.py` and the renderer never need Blender installed - the export is offline
only, which keeps the mod dependency free.
