# Shaders

The mod draws its effects with vanilla render types (`RenderType.lightning()`: vertex coloured,
unlit, additive quads), which means **no custom core shader is required** and the effects keep working
with shader packs that only understand vanilla types.

Drop optional post-processing or effect shaders here if a pack wants them:

* `aura.fsh`/`aura.vsh` - custom aura shading
* `beam.fsh`/`beam.vsh` - animated beam distortion
* `impact.fsh`/`impact.vsh` - shockwave refraction

Register them from the client with NeoForge's `RegisterShadersEvent` and reference them from your own
render type; the built in renderer stays on the vanilla path so it can never break a world.
