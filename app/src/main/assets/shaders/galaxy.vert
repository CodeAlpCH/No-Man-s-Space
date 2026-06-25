attribute vec4 a_position;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec3 v_dir;

void main() {
    // Direction = normalized local position on unit sphere
    v_dir = normalize(a_position.xyz);
    gl_Position = u_projViewTrans * u_worldTrans * a_position;
}
