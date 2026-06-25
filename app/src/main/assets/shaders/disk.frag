#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
varying vec3 v_worldPos;
varying float v_lift;

uniform vec3  u_camPos;
uniform vec3  u_bhPos;
uniform float u_time;
uniform float u_nitro;
uniform float u_opacity;

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

vec4 diskPlasma(float phi, float radial, float time, float nitro, float beam) {
    radial = clamp(radial, 0.0, 1.0);
    float kepler = 6.0 / (radial * 0.65 + 0.12);
    float spin   = phi + time * kepler;
    float warpP  = spin + 0.10 * diskFbm(vec2(phi * 2.3, radial * 5.0) + time * 0.4);
    float storm  = diskFbm(vec2(warpP * 2.37, radial * 7.3 - time * 1.6));
    float pat    = storm;

    float heat  = 1.0 - radial;
    float heat2 = heat * heat;

    vec3 innerCol = vec3(1.00, 0.70, 0.25);
    vec3 midCol   = vec3(0.92, 0.40, 0.05);
    vec3 outerCol = vec3(0.32, 0.09, 0.02);
    vec3 color    = mix(outerCol, mix(midCol, innerCol, heat2), heat);

    color *= (0.55 + heat2 * 2.5 + pat * 0.4 + nitro * 1.0) * beam;
    color = min(color, vec3(1.6, 0.85, 0.35));

    float alpha = heat * (0.5 + pat * 0.35);
    alpha *= smoothstep(0.0, 0.06, radial);
    alpha *= 1.0 - smoothstep(0.78, 1.00, radial);

    return vec4(color, alpha);
}

void main() {
    float angle  = v_uv.x * 6.28318;
    float radial = v_uv.y;

    vec3 relBh   = v_worldPos - u_bhPos;
    vec3 toCam   = normalize(u_camPos - v_worldPos);
    vec3 tangent = normalize(vec3(-sin(angle), 0.0, cos(angle)));
    float vDot   = dot(tangent, toCam);
    float beam   = 0.30 + pow(max(0.0, vDot), 1.8) * 1.2;

    // Hide disk through the shadow (no white bleed in centre)
    float rXZ   = length(vec2(relBh.x, relBh.z));
    float occ   = smoothstep(0.7, 1.35, rXZ);
    if (occ < 0.02) discard;

    float hotBand = smoothstep(0.48, 0.12, radial) * smoothstep(0.04, 0.10, radial);
    float recede = smoothstep(-0.1, 0.3, vDot);

    vec4 d = diskPlasma(angle, radial, u_time, u_nitro, beam);
    d.rgb *= 1.0 + v_lift * 0.8;
    d.a   *= hotBand * recede * occ * u_opacity;

    if (d.a < 0.01) discard;
    gl_FragColor = d;
}
