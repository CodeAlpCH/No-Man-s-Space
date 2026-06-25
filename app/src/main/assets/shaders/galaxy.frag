#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_dir;

// ── Mobile-safe hash: all intermediates stay below 30 (mediump-safe) ──────────
// Quilez-style but final product replaced with bounded mixing.
// Verified: works correctly for cell indices 0..192 on mediump float.
float h1(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));           // → [0,1]
    p = fract(p * 13.5 + p.yzx * 7.2 + p.zxy * 5.8);      // max ≈ 26.5, fract → [0,1]
    return fract(p.x + p.y + p.z);
}
float h2(vec3 p) {
    p = fract(p * vec3(0.1013, 0.1097, 0.1031));
    p = fract(p * 11.3 + p.yzx * 9.1 + p.zxy * 7.6);
    return fract(p.x + p.y + p.z);
}
float h3(vec3 p) {
    p = fract(p * vec3(0.1097, 0.1013, 0.1033));
    p = fract(p * 15.2 + p.yzx * 6.7 + p.zxy * 8.9);
    return fract(p.x + p.y + p.z);
}
float h4(vec3 p) {
    p = fract(p * vec3(0.1033, 0.1097, 0.1013));
    p = fract(p * 9.8 + p.yzx * 13.1 + p.zxy * 6.3);
    return fract(p.x + p.y + p.z);
}

// ── Smooth noise (trilinear, for FBM) ─────────────────────────────────────────
float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(h1(i),            h1(i+vec3(1,0,0)), u.x),
            mix(h1(i+vec3(0,1,0)),h1(i+vec3(1,1,0)), u.x), u.y),
        mix(mix(h1(i+vec3(0,0,1)),h1(i+vec3(1,0,1)), u.x),
            mix(h1(i+vec3(0,1,1)),h1(i+vec3(1,1,1)), u.x), u.y), u.z);
}

// ── FBM — 3 octaves (mobile-performance safe) ─────────────────────────────────
float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p *= 2.1;
        a *= 0.5;
    }
    return v;
}

// ── Point star (Voronoi cell) — scales ≤ 96 for mediump safety ────────────────
float pointStar(vec3 d, float scale, float density, float sharp, float bright) {
    vec3 s    = (d + 1.0) * scale;     // max cell index = 2*scale ≤ 192 ✓
    vec3 cell = floor(s);
    vec3 f    = fract(s);
    if (h1(cell) > density) return 0.0;
    vec3 ctr = vec3(0.2) + vec3(h2(cell), h3(cell), h4(cell)) * 0.6;
    float d2 = dot(f - ctr, f - ctr);
    return exp(-d2 * sharp) * bright;
}

// ── Star spectral colour (blackbody distribution) ─────────────────────────────
vec3 starColor(vec3 cell) {
    float t = h4(cell);
    if      (t > 0.99) return vec3(0.62, 0.74, 1.00); // O/B blue-white  (1%)
    else if (t > 0.96) return vec3(0.82, 0.90, 1.00); // A  white-blue   (3%)
    else if (t > 0.88) return vec3(1.00, 0.97, 0.88); // F/G yellow-white (8%)
    else if (t > 0.66) return vec3(1.00, 0.82, 0.52); // K  orange       (22%)
    else               return vec3(1.00, 0.62, 0.32); // M  orange-red   (66%)
}

void main() {
    vec3 d = normalize(v_dir);

    // ── DEEP SPACE BACKGROUND ─────────────────────────────────────────────────
    vec3 col = vec3(0.006, 0.008, 0.026);

    // ── MILKY WAY BAND ────────────────────────────────────────────────────────
    float mwBand   = exp(-d.y * d.y / 0.014);
    float phi      = atan(d.z, d.x);
    float armSwirl = sin(phi * 2.0 + 0.8) * 0.5 + 0.5;

    vec3  mwCoord  = d * 3.5;
    float mwFBM    = fbm(mwCoord + vec3(1.7, 2.3, 0.5));
    float mwDens   = mwBand * mwFBM * (0.85 + armSwirl * 0.35);

    float core = exp(-d.y*d.y/0.006) * exp(-d.z*d.z/0.06) * 0.45;
    mwDens += core * mwFBM;

    vec3 mwWarm  = vec3(0.52, 0.40, 0.22);
    vec3 mwCool  = vec3(0.20, 0.28, 0.48);
    vec3 mwColor = mix(mwCool, mwWarm, core / (core + 0.25));
    col += mwColor * mwDens * 0.36;

    float mwHaze = mwBand * mwFBM * 0.14;
    col += vec3(0.65, 0.70, 0.80) * mwHaze;

    // ── STARS — scales ≤ 96 (cell indices ≤ 192, mediump safe) ───────────────
    // Bright & rare: scale 32
    float s1  = pointStar(d, 32.0, 0.015, 12.0, 22.0);
    vec3  c1  = floor((d + 1.0) * 32.0);
    vec3  col1 = starColor(c1) * s1;

    // Medium: scale 64
    float s2  = pointStar(d, 64.0, 0.012, 22.0, 9.0);
    vec3  c2  = floor((d + 1.0) * 64.0);
    float hc2 = h3(c2);
    vec3  col2 = (hc2 > 0.60 ? vec3(0.80, 0.92, 1.00) :
                  hc2 > 0.30 ? vec3(1.00, 0.96, 0.84) :
                                vec3(1.00, 0.76, 0.50)) * s2;

    // Faint/distant: scale 96
    float s3  = pointStar(d, 96.0, 0.014, 38.0, 4.0);

    // Dense stars in MW band
    float mwS = pointStar(d, 64.0, 0.018, 30.0, 2.5) * mwBand;

    // ── NEBULAE — broad smooth gas clouds (NMS style) ─────────────────────────
    // pow(dot, ~2) = 60-90° wide, no hard edges, FBM for internal structure.

    vec3  n1dir = normalize(vec3(0.55,  0.40, -0.55));
    float n1    = pow(max(0.0, dot(d, n1dir)), 1.8) * fbm(d * 2.8 + vec3(3.1, 1.2, 0.7));
    col += vec3(0.04, 0.10, 0.28) * n1 * 0.50;

    vec3  n2dir = normalize(vec3(-0.65, 0.05, 0.40));
    float n2    = pow(max(0.0, dot(d, n2dir)), 2.0) * fbm(d * 2.2 + vec3(0.5, 2.8, 1.4));
    col += vec3(0.18, 0.05, 0.02) * n2 * 0.45;

    // ── COMPOSE ───────────────────────────────────────────────────────────────
    col += col1;
    col += col2 * 0.85;
    col += vec3(0.88, 0.93, 1.00) * s3 * 0.55;
    col += vec3(0.84, 0.88, 0.95) * mwS * 0.65;

    gl_FragColor = vec4(col, 1.0);
}
