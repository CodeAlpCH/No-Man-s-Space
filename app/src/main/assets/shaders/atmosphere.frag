#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec3 v_worldPos;

uniform vec3 u_camPos;
uniform vec3 u_atmColor;
uniform float u_density;

void main() {
    vec3 n    = normalize(v_normal);
    vec3 vdir = normalize(u_camPos - v_worldPos);
    float mu  = max(0.0, dot(n, vdir));

    // Limb-only shell — transparent facing camera, bright blue halo at edge
    float rim = pow(1.0 - mu, 3.2);
    float alpha = rim * u_density;
    if (alpha < 0.012) discard;

    vec3 col = u_atmColor * (0.55 + rim * 0.95);
    gl_FragColor = vec4(col, alpha * 0.72);
}
