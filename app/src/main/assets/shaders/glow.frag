#ifdef GL_ES
precision mediump float;
#endif

varying float v_rim;

uniform vec4  u_color;
uniform float u_rimPow;
uniform float u_coreFill; // 1.0 = solid core (matter), 0.0 = hollow (halo)
uniform float u_pulse;    // 0..1 extra brightness multiplier

void main() {
    float rimGlow  = pow(v_rim, u_rimPow);
    float core     = (1.0 - v_rim) * (1.0 - v_rim) * u_coreFill;
    float combined = rimGlow + core;

    vec3  color = u_color.rgb * (1.0 + u_pulse * 0.6);
    float alpha = combined * u_color.a;

    if (alpha < 0.008) discard;
    gl_FragColor = vec4(color * combined, alpha);
}
