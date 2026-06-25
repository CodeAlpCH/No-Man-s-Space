#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform float u_time;
uniform float u_nitro;
uniform float u_opacity;

void main() {
    float angle  = v_uv.x * 6.28318;
    float radial = v_uv.y;   // 0 = inner edge, 1 = outer edge

    // ── Keplerian spin: inner much faster ─────────────────────────────────────
    float spin = angle + u_time * (4.5 - radial * 3.2);

    // ── Turbulence bands (layered for complexity) ──────────────────────────────
    float t1  = sin(spin * 3.0  + radial * 9.0) * 0.5 + 0.5;
    float t2  = sin(spin * 7.0  - radial * 5.5 - u_time * 0.7) * 0.5 + 0.5;
    float t3  = sin(spin * 12.0 + radial * 4.0 + u_time * 1.2) * 0.5 + 0.5;
    float pat = mix(mix(t1, t2, 0.45), t3, 0.28);

    // ── Temperature gradient: blazing white-hot → intense orange → dark ember ──
    float heat  = 1.0 - radial;            // 1 at inner edge, 0 at outer
    float heat2 = heat * heat;
    float heat3 = heat2 * heat;

    // Inner ring: white-hot plasma (like a solar flare)
    vec3 innerCol = mix(vec3(1.00, 0.96, 0.85), vec3(1.00, 1.00, 1.00), pat * heat2);
    // Mid ring: pure orange-red
    vec3 midCol   = mix(vec3(1.00, 0.48, 0.05), vec3(1.00, 0.65, 0.18), pat);
    // Outer ring: dark smouldering embers
    vec3 outerCol = mix(vec3(0.35, 0.08, 0.01), vec3(0.58, 0.18, 0.03), t1 * 0.7 + 0.3);

    vec3 color = mix(outerCol, mix(midCol, innerCol, heat2), heat);

    // Brightness: inner edge blazes brightest — very high contrast like Interstellar
    float brightness = 0.8 + heat3 * 4.5 + pat * heat * 0.6 + u_nitro * 1.8;
    color *= brightness;

    // ── Relativistic beaming: near side is dramatically (4x) brighter ─────────
    // In Interstellar the approaching side of the disk looks almost white.
    float beamAngle = sin(angle - u_time * 0.28);
    // remap 0..1 so dim side = 0.28, bright side = 1.55
    float beam = 0.28 + pow(max(0.0, beamAngle), 2.0) * 1.27;
    color *= beam;

    // Extra: apply beaming also to the inner glow for dramatic effect
    float innerGlow = heat3 * (2.5 + u_nitro * 2.5) * (0.6 + max(0.0, beamAngle) * 0.8);
    color += vec3(1.00, 0.80, 0.35) * innerGlow;

    // ── Edge transparency ─────────────────────────────────────────────────────
    float alpha = heat * (0.65 + pat * 0.35) * u_opacity;
    alpha *= smoothstep(0.0, 0.06, radial);          // fade at inner gap
    alpha *= 1.0 - smoothstep(0.82, 1.00, radial);  // fade at outer edge
    // Beamed side is also more opaque
    alpha *= (0.7 + max(0.0, beamAngle) * 0.5);

    if (alpha < 0.006) discard;
    gl_FragColor = vec4(color, alpha);
}
