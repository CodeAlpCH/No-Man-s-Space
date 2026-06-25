#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform float u_surfaceDist;
uniform float u_fogStart;

void main() {
    if (u_surfaceDist > u_fogStart) discard;

    float enter = 1.0 - clamp(u_surfaceDist / u_fogStart, 0.0, 1.0);
    enter = enter * enter;
    float deep  = 1.0 - clamp(u_surfaceDist / 600.0, 0.0, 1.0);
    float baseA = enter * 0.22 + deep * 0.16;

    float skyT = v_uv.y;
    float a = baseA * (1.0 - skyT * 0.48);
    if (a < 0.003) discard;

    vec3 col = mix(vec3(0.18, 0.46, 0.80), vec3(0.10, 0.28, 0.58), skyT * 0.35);

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    col += (dither - 0.5) / 255.0;

    gl_FragColor = vec4(col, a);
}
