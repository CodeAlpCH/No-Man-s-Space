attribute vec4 a_position;
attribute vec3 a_normal;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec3 v_normal;
varying vec3 v_worldPos;

void main() {
    vec4 worldPos = u_worldTrans * a_position;
    v_worldPos    = worldPos.xyz;
    v_normal      = normalize(mat3(u_worldTrans) * a_normal);
    gl_Position   = u_projViewTrans * worldPos;
}
