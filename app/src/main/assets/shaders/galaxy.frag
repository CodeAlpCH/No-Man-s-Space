#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_dir;

uniform vec3  u_camPos;
uniform vec3  u_bhPos;
uniform float u_bhRadius;

// ── Mobile-safe hash ──────────────────────────────────────────────────────────
float h1(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p = fract(p * 13.5 + p.yzx * 7.2 + p.zxy * 5.8);
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
float fbm(vec3 p) {
    return noise(p) * 0.667 + noise(p * 2.1) * 0.333;
}

float pointStar(vec3 d, float scale, float density, float sharp, float bright) {
    vec3 s    = (d + 1.0) * scale;
    vec3 cell = floor(s);
    vec3 f    = fract(s);
    if (h1(cell) > density) return 0.0;
    vec3 ctr = vec3(0.25) + vec3(h2(cell), h3(cell), h4(cell)) * 0.50;
    float d2 = dot(f - ctr, f - ctr);
    return exp(-d2 * sharp) * bright;
}

vec3 starColor(vec3 cell) {
    float t = h4(cell);
    if      (t > 0.99) return vec3(0.62, 0.74, 1.00);
    else if (t > 0.96) return vec3(0.82, 0.90, 1.00);
    else if (t > 0.88) return vec3(1.00, 0.97, 0.88);
    else if (t > 0.66) return vec3(1.00, 0.82, 0.52);
    else               return vec3(1.00, 0.62, 0.32);
}

// ── Light sky deflection — single sample, mobile-safe (no acos) ───────────────
vec3 gravLens(vec3 rayDir, vec3 bhDir, float angSize) {
    float cosA = dot(rayDir, bhDir);
    float angSq = max(0.0, 2.0 * (1.0 - cosA));
    float b = sqrt(angSq + 0.0008);
    float deflect = angSize * angSize * 0.35 / (b + 0.06);
    deflect = min(deflect, 0.15);

    vec3 perp = rayDir - bhDir * cosA;
    float pLen = length(perp);
    if (pLen < 0.0001) return rayDir;
    return normalize(rayDir + perp / pLen * deflect);
}

// Sample the galaxy at direction d
vec3 galaxyAt(vec3 d) {
    d = normalize(d);

    vec3 col = vec3(0.006, 0.008, 0.026);

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

    col += vec3(0.65, 0.70, 0.80) * mwBand * mwFBM * 0.14;

    float s1 = pointStar(d, 32.0, 0.015, 90.0, 22.0);
    vec3 c1  = floor((d + 1.0) * 32.0);
    vec3 col1 = starColor(c1) * s1;

    float s2 = pointStar(d, 64.0, 0.012, 110.0, 9.0);
    vec3 c2  = floor((d + 1.0) * 64.0);
    float hc2 = h3(c2);
    vec3 col2 = (hc2 > 0.60 ? vec3(0.80, 0.92, 1.00) :
                 hc2 > 0.30 ? vec3(1.00, 0.96, 0.84) :
                               vec3(1.00, 0.76, 0.50)) * s2;

    float s3  = pointStar(d, 96.0, 0.014, 140.0, 4.0);
    float mwS = pointStar(d, 64.0, 0.018, 120.0, 2.5) * mwBand;

    float n1 = max(0.0, fbm(d * 1.6 + vec3(3.1, 1.2, 0.7)) - 0.46);
    col += vec3(0.04, 0.10, 0.30) * n1 * 1.2;

    float n2 = max(0.0, fbm(d * 1.4 + vec3(-2.5, 0.8, 1.9)) - 0.48);
    col += vec3(0.22, 0.06, 0.02) * n2 * 1.0;

    col += col1;
    col += col2 * 0.85;
    col += vec3(0.88, 0.93, 1.00) * s3 * 0.55;
    col += vec3(0.84, 0.88, 0.95) * mwS * 0.65;

    return col;
}

void main() {
    vec3 d = normalize(v_dir);

    vec3 toBh  = u_bhPos - u_camPos;
    float dist = length(toBh);
    float angSize = u_bhRadius / max(dist, 120.0);

    // Far from BH: one cheap sample, no lensing math
    if (angSize < 0.003) {
        gl_FragColor = vec4(galaxyAt(d), 1.0);
        return;
    }

    vec3 bhDir = toBh / dist;
    float cosA = dot(d, bhDir);
    float angSq = max(0.0, 2.0 * (1.0 - cosA));

    // Subtle lens zone
    float lensZone = smoothstep(angSize * angSize * 40.0, angSize * angSize * 1.5, angSq);
    lensZone *= 0.55;

    vec3 sampleDir = mix(d, gravLens(d, bhDir, angSize), lensZone);
    vec3 col = galaxyAt(sampleDir);

    // Shadow falloff near BH
    float shadow = smoothstep(angSize * angSize * 0.8, angSize * angSize * 0.15, angSq);
    col *= 1.0 - shadow * 0.80;

    gl_FragColor = vec4(col, 1.0);
}
