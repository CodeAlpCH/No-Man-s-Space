attribute vec4 a_position;
attribute vec3 a_normal;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform vec3 u_camPos;

varying vec3 v_normal;
varying vec3 v_worldPos;
varying vec3 v_localPos;   // position on unit sphere surface = seed for noise

void main() {
    vec4 worldPos = u_worldTrans * a_position;
    v_worldPos    = worldPos.xyz;
    v_normal      = normalize(mat3(u_worldTrans) * a_normal);
    v_localPos    = a_position.xyz;              // range ≈ -1..1 on unit sphere
    gl_Position   = u_projViewTrans * worldPos;
}
