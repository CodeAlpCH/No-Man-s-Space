#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec2 v_uv;
varying vec3 v_worldPos;

uniform vec3  u_camPos;
uniform float u_time;
uniform float u_nitro;

float dh(vec2 p) {
    p = fract(p * vec2(0.1031, 0.1030));
    p += dot(p, p.yx + 19.19);
    return fract(p.x * p.y);
}
float diskNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(dh(i), dh(i + vec2(1.0, 0.0)), u.x),
        mix(dh(i + vec2(0.0, 1.0)), dh(i + vec2(1.0, 1.0)), u.x), u.y);
}
float diskFbm(vec2 p) {
    return diskNoise(p) * 0.62 + diskNoise(p * 2.07 + vec2(1.7, 2.3)) * 0.38;
}

// Warm plasma only — NO white (prevents white-hole look)
vec3 diskPlasmaRgb(float phi, float radial, float time, float nitro, float beam) {
    radial = clamp(radial, 0.0, 1.0);
    float kepler = 6.0 / (radial * 0.65 + 0.12);
    float spin   = phi + time * kepler;
    float warpR  = radial + 0.06 * sin(phi * 5.7 + time * 2.8);
    float warpP  = spin + 0.10 * diskFbm(vec2(phi * 2.3, radial * 5.0) + time * 0.4);
    float storm  = diskFbm(vec2(warpP * 2.37, warpR * 7.3 - time * 1.6));
    float fil    = pow(diskFbm(vec2(warpP * 5.71, warpR * 13.5 + time * 0.9)), 1.5);
    float pat    = storm * 0.6 + fil * 0.4;

    float heat  = 1.0 - radial;
    float heat2 = heat * heat;

    vec3 innerCol = vec3(1.00, 0.72, 0.28);
    vec3 midCol   = vec3(0.95, 0.42, 0.06);
    vec3 outerCol = vec3(0.35, 0.10, 0.02);
    vec3 color    = mix(outerCol, mix(midCol, innerCol, heat2), heat);

    color *= (0.6 + heat2 * 2.8 + pat * 0.5 + nitro * 1.2) * beam;
    return min(color, vec3(1.8, 1.0, 0.45));
}

vec3 lensArc(vec3 n, float phi, float arcY, float width, float time, float nitro, float rim) {
    float band = exp(-pow((n.y - arcY) / width, 2.0)) * pow(rim, 2.5);
    if (band < 0.02) return vec3(0.0);
    float lensPhi = phi + 3.14159;
    return diskPlasmaRgb(lensPhi, 0.08 + band * 0.10, time, nitro, 0.75) * band * (2.5 + u_nitro * 1.5);
}

void main() {
    vec3  n       = normalize(v_normal);
    vec3  viewDir = normalize(u_camPos - v_worldPos);
    float facing  = max(0.0, dot(n, viewDir));
    float rim     = 1.0 - facing;
    float phi     = atan(n.z, n.x);

    // ── ABSOLUTE VOID: centre of shadow must stay pure black ──────────────────
    if (facing > 0.55) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 color = vec3(0.0);

    // Lensed arcs (orange only, on silhouette)
    color += lensArc(n, phi,  0.52, 0.13, u_time, u_nitro, rim);
    color += lensArc(n, phi, -0.52, 0.12, u_time, u_nitro, rim);

    // Thin amber photon ring at silhouette edge only
    float eqMask  = pow(1.0 - abs(n.y), 6.0);
    float photon  = pow(rim, 14.0) * eqMask * (2.0 + u_nitro * 1.5);
    color += vec3(1.00, 0.62, 0.18) * photon;

    if (u_nitro > 0.06) color *= 1.0 + u_nitro * 0.15;

    gl_FragColor = vec4(min(color, vec3(2.5, 1.2, 0.5)), 1.0);
}
