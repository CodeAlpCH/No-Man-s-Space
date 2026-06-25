#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform float u_time;
uniform float u_thrust;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    float v = v_uv.y;
    float u = abs(v_uv.x - 0.5) * 2.0;

    float taper = 1.0 - smoothstep(0.0, 0.92, v);
    float edge  = 1.0 - smoothstep(0.15, 1.0, u);
    float shape = taper * edge;
    if (shape < 0.04) discard;

    vec2 nCoord = vec2(v_uv.x * 5.0 - u_time * 3.5, v * 9.0 + u_time * 7.0);
    float turb = noise(nCoord) * 0.55 + noise(nCoord * 2.3 + 1.7) * 0.45;

    vec3 core  = vec3(1.0, 0.97, 0.88);
    vec3 plasma = vec3(0.28, 0.68, 1.0);
    vec3 trail = vec3(1.0, 0.42, 0.08);

    vec3 col = mix(core, plasma, smoothstep(0.05, 0.42, v));
    col = mix(col, trail, smoothstep(0.38, 0.95, v));
    col *= 0.82 + turb * 0.38;

    float alpha = shape * (0.55 + u_thrust * 0.45) * (1.0 - v * 0.25);
    gl_FragColor = vec4(col, alpha);
}
