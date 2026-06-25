attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform float u_time;

varying vec2 v_uv;
varying vec3 v_worldPos;
varying float v_lift;

void main() {
    vec3 pos = a_position.xyz;
    float r  = length(pos.xz);

    // Gravitational bowl: disk curves upward near the inner edge (light paths bend)
    float bowl = smoothstep(1.05, 1.55, r) * smoothstep(3.8, 2.2, r);
    float wave = 0.06 * sin(atan(pos.z, pos.x) * 3.0 + u_time * 1.8);
    pos.y += bowl * (0.22 + wave);

    v_lift = bowl;

    vec4 worldPos = u_worldTrans * vec4(pos, 1.0);
    v_worldPos    = worldPos.xyz;
    v_uv          = a_texCoord0;
    gl_Position   = u_projViewTrans * worldPos;
}
