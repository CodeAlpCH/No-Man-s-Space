#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform vec3 u_oceanDeep;
uniform vec3 u_oceanShallow;
uniform vec3 u_atmColor;

void main() {
    vec2 p = v_uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.0) discard;

    float rim = smoothstep(0.70, 0.98, r);
    vec3 col = mix(u_oceanDeep, u_oceanShallow, 1.0 - r * 0.80);
    col = mix(col, u_atmColor, rim * 0.50);

    float alpha = smoothstep(1.0, 0.90, r);
    gl_FragColor = vec4(col, alpha);
}
