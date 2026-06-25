#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec2 v_uv;
varying vec3 v_worldPos;

uniform vec3  u_camPos;
uniform float u_time;
uniform float u_nitro;

void main() {
    vec3  n       = normalize(v_normal);
    vec3  viewDir = normalize(u_camPos - v_worldPos);
    float facing  = max(0.0, dot(n, viewDir));   // 1 = toward cam, 0 = edge
    float rim     = 1.0 - facing;                // 0 = center, 1 = silhouette
    float phi     = atan(n.z, n.x);

    // ── 1. EVENT HORIZON — absolute void ──────────────────────────────────────
    // The center MUST stay black. All effects are multiplied by rim powers
    // to guarantee zero brightness at facing=1 (dead center of sphere).
    vec3 color = vec3(0.0);

    // ── 2. PHOTON RING — ultra-thin blazing rim at silhouette ─────────────────
    // pow(rim,9) = near 0 everywhere except the last ~10% of the rim.
    float photonRing = pow(rim, 9.0) * (8.0 + u_nitro * 8.0);
    vec3  photonCol  = mix(vec3(1.00, 0.82, 0.45), vec3(1.00, 0.55, 0.10), u_nitro * 0.4);
    color += photonCol * photonRing;

    // Soft warm halo just outside the photon ring (still rim-concentrated)
    float halo = pow(rim, 4.5) * 0.60;
    color += vec3(0.65, 0.25, 0.04) * halo;

    // ── 3. EQUATORIAL DISK GLOW on sphere ─────────────────────────────────────
    // "equator" selects the belt around n.y≈0.
    // "pow(rim,5.0)" means the glow is ONLY at the silhouette edge,
    //   keeping the centre of the sphere completely black.
    float equator  = pow(1.0 - abs(n.y), 9.0);
    float diskGlow = equator * pow(rim, 5.0) * (5.0 + u_nitro * 2.5);

    float a1  = sin(phi * 5.0 + u_time * 2.2) * 0.5 + 0.5;
    float a2  = sin(phi * 9.0 - u_time * 0.9) * 0.5 + 0.5;
    float pat = mix(a1, a2, 0.45);

    // Near-side of the equatorial belt: slightly brighter (relativistic beaming)
    float nearSide = pow(facing, 0.5);
    vec3 diskHot = mix(vec3(0.90, 0.55, 0.12), vec3(1.00, 0.90, 0.65), pat * nearSide);
    diskHot *= (1.5 + pat * 0.5 + u_nitro * 1.5) * (0.5 + nearSide * 0.6);
    color += diskHot * diskGlow;

    // ── 4. GRAVITATIONAL LENSING ARCS ─────────────────────────────────────────
    // Light from the far side of the disk bends around the BH and appears
    // as bright arcs near the TOP and BOTTOM of the silhouette.
    //
    // Critical: ALL lensing effects are multiplied by pow(rim, 1.5) to ensure
    // they NEVER fill the center of the sphere with colour.

    float rimFade = pow(rim, 1.3);   // fades toward center, full at silhouette

    // Primary top arc
    float lensTop    = exp(-pow((n.y - 0.70) * 6.5, 2.0));
    float lensTopPat = sin(phi * 0.8 + u_time * 0.22) * 0.5 + 0.5;
    float lensTopArc = lensTop * rimFade * (5.0 + lensTopPat * 1.5 + u_nitro * 3.0);
    vec3  lensTopCol = mix(vec3(0.88, 0.50, 0.10), vec3(1.00, 0.78, 0.38), lensTopPat);
    color += lensTopCol * lensTopArc;

    // Secondary bottom arc (mirror image)
    float lensBot    = exp(-pow((n.y + 0.70) * 8.0, 2.0));
    float lensBotArc = lensBot * rimFade * (3.0 + u_nitro * 2.0);
    color += vec3(0.85, 0.48, 0.10) * lensBotArc;

    // ── 5. PROXIMITY EFFECT ───────────────────────────────────────────────────
    if (u_nitro > 0.06) {
        color *= 1.0 + u_nitro * 0.3;
        color = mix(color, color * vec3(1.4, 0.80, 0.28), u_nitro * 0.30);
    }

    gl_FragColor = vec4(clamp(color, 0.0, 6.0), 1.0);
}
